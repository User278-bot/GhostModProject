package com.ghost.server;

import com.ghost.api.dto.AuthData;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.SerializationUtil;
import com.ghost.util.CryptoUtil;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GhostModServer extends WebSocketServer {

    // 簡易的なトークンバケットアルゴリズムの実装
    static class SimpleRateLimiter {
        private final double permitsPerSecond;
        private double tokens;
        private long lastRefillTimestamp;

        public SimpleRateLimiter(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
            this.tokens = permitsPerSecond; // Start full
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double secondsPassed = (now - lastRefillTimestamp) / 1_000_000_000.0;
            double newTokens = secondsPassed * permitsPerSecond;

            if (newTokens > 0) {
                tokens = Math.min(permitsPerSecond, tokens + newTokens);
                lastRefillTimestamp = now;
            }
        }

        public static SimpleRateLimiter create(double permitsPerSecond) {
            return new SimpleRateLimiter(permitsPerSecond);
        }
    }

    private static final Map<WebSocket, GhostClientData> sessions = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModServer.class);

    private final String serverPassword;

    // レート制限関連
    private final Map<InetAddress, SimpleRateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final double packetsPerSecond;

    // 視認距離の二乗（ブロック単位）
    private final double visibleRangeSqr;

    public GhostModServer(int port, String password, double visibleRange, double packetsPerSecond) {
        super(new InetSocketAddress(port));
        this.serverPassword = password;
        this.visibleRangeSqr = visibleRange * visibleRange;
        this.packetsPerSecond = packetsPerSecond;
    }

    @Override
    public void onStart() {
        // サーバーが起動したときの処理
        LOGGER.info("Server started on port: {}", getPort());
        LOGGER.info("Authentication enabled using CHAP.");
        LOGGER.info("Visibility Range: {} blocks", Math.sqrt(visibleRangeSqr));
        LOGGER.info("Rate Limit: {} packets/sec", packetsPerSecond);
        LOGGER.debug("\n=== DEBUG MODE ===\n");
        setConnectionLostTimeout(100); // 接続タイムアウトの設定（秒）
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // 新しいクライアントが接続したときの処理
        LOGGER.info("New connection from: {}", conn.getRemoteSocketAddress());

        // 1. チャレンジ(乱数)の生成と送信
        final var nonce = ChapAuthenticator.generateNonce();
        sessions.put(conn, new GhostClientData(nonce, false, null));

        // クライアントへ送信
        AuthData authData = new AuthData(nonce, null);
        // Note: Generic type T is inferred or we can cast. Serialization utilizes
        // object structure.
        GhostPacket<AuthData> challengePacket = new GhostPacket<>(MessageType.AUTH_CHALLENGE, authData);
        try {
            conn.send(CryptoUtil.encrypt(SerializationUtil.serializePacket(challengePacket), serverPassword));
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt challenge", e);
        }

        LOGGER.info("Sent AUTH_CHALLENGE to {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // クライアントが切断したときの処理
        var removedData = sessions.remove(conn);
        var disconnectedPlayer = removedData.playerData();
        if (disconnectedPlayer != null) {
            LOGGER.info("Player {} disconnected: {} (Code: {}, Reason: {}, Remote: {})", disconnectedPlayer.name(),
                    conn.getRemoteSocketAddress(), code, reason, remote);
            sendLeavePacket(disconnectedPlayer);
        } else {
            LOGGER.warn("Disconnected a client that had not sent any data: {}", conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        // レート制限チェック
        InetAddress address = conn.getRemoteSocketAddress().getAddress();
        SimpleRateLimiter limiter = rateLimiters.computeIfAbsent(address,
                k -> SimpleRateLimiter.create(packetsPerSecond));

        if (!limiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for {}", address);
            return;
        }

        // 復号化処理
        byte[] decryptedBytes;
        try {
            decryptedBytes = CryptoUtil.decrypt(message.array(), serverPassword);
        } catch (Exception e) {
            LOGGER.warn("Failed to decrypt binary message from {}, closing connection.", conn.getRemoteSocketAddress());
            conn.close(4003, "Decryption Failed");
            return;
        }

        // 認証チェック
        var session = sessions.get(conn);
        if (session == null || !session.isAuthenticated()) {
            LOGGER.warn("Received binary message from unauthenticated client: {}", conn.getRemoteSocketAddress());
            return;
        }

        try {
            MessageType type = com.ghost.api.proto.ProtoConverter.deserializePacketType(decryptedBytes);
            if (type == MessageType.UPDATE) {
                var playerData = com.ghost.api.proto.ProtoConverter.deserializeUpdatePacket(decryptedBytes);
                if (playerData == null) {
                    LOGGER.warn("Failed to parse PlayerData from {}", conn.getRemoteSocketAddress());
                    return;
                }
                if (session.playerData() == null) {
                    LOGGER.info("Player initialized: {}", conn.getRemoteSocketAddress());
                } else {
                    sendUpdatePacket(playerData, conn);
                }

                // 追跡状態（trackedPlayers）を引き継いで更新
                sessions.put(conn, new GhostClientData(session.nonce(), true, playerData, session.trackedPlayers()));
            }
        } catch (Exception ex) {
            LOGGER.error("Error processing binary message from {}", conn.getRemoteSocketAddress(), ex);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // レート制限チェック
        InetAddress address = conn.getRemoteSocketAddress().getAddress();
        SimpleRateLimiter limiter = rateLimiters.computeIfAbsent(address,
                k -> SimpleRateLimiter.create(packetsPerSecond));

        if (!limiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for {}", address);
            return; // 制限超過時はメッセージを無視
        }

        // 復号化処理
        String decryptedMessage;
        try {
            decryptedMessage = CryptoUtil.decrypt(message, serverPassword);
        } catch (Exception e) {
            LOGGER.warn("Failed to decrypt message from {}, closing connection.", conn.getRemoteSocketAddress());
            conn.close(4003, "Decryption Failed");
            return;
        }

        // 認証チェック
        var session = sessions.get(conn);
        if (session == null) {
            return;
        }
        if (!session.isAuthenticated()) {
            handleAuthHandshake(conn, decryptedMessage);
            return;
        }

        LOGGER.debug("Message from {}: {}", conn.getRemoteSocketAddress(), decryptedMessage);
    }

    private void handleAuthHandshake(WebSocket conn, String message) {
        try {
            // パケットタイプを確認せずにデシリアライズ（型安全ではないが、JSON構造チェックで判断）
            var packet = SerializationUtil.deserializePacket(message);

            if (packet != null && packet.getType() == MessageType.AUTH_RESPONSE) {
                var session = sessions.get(conn);
                AuthData authData = SerializationUtil.parseAuthData(packet.getData());
                String clientHash = authData.hash();
                String nonce = session.nonce();

                if (ChapAuthenticator.verify(serverPassword, nonce, clientHash)) {
                    // 認証成功
                    sessions.put(conn, new GhostClientData(null, true, null));
                    LOGGER.info("Authentication SUCCESS for {}", conn.getRemoteSocketAddress());

                    // 認証成功パケットを送信
                    GhostPacket<Void> successPacket = new GhostPacket<>(MessageType.AUTH_SUCCESS, null);
                    try {
                        conn.send(CryptoUtil.encrypt(SerializationUtil.serializePacket(successPacket), serverPassword));
                    } catch (Exception e) {
                        LOGGER.error("Failed to encrypt auth success", e);
                    }

                    // 初期同期パケットを送るなど、通常のフローへ
                } else {
                    // 認証失敗
                    LOGGER.warn("Authentication FAILED for {}. Closing connection.", conn.getRemoteSocketAddress());
                    conn.close(4001, "Authentication Failed");
                }
            } else {
                LOGGER.warn(
                        "Expected AUTH_RESPONSE but received different packet/invalid data from unauthenticated client: {}",
                        conn.getRemoteSocketAddress());
                // 認証前に変なパケットを送ってきたら切断するなどの厳しい措置も検討
            }
        } catch (Exception e) {
            LOGGER.error("Auth handshake error for {}", conn.getRemoteSocketAddress(), e);
            conn.close(4002, "Handshake Error");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // エラーが発生したときの処理
        // ex.toString() の代わりにexを直接渡すことで、スタックトレースがログに出力され、デバッグが容易になります。
        if (conn != null) {
            LOGGER.error("An error occurred on connection {}:", conn.getRemoteSocketAddress(), ex);
            sessions.remove(conn);
        } else {
            LOGGER.error("An error occurred:", ex);
        }
    }

    private void sendLeavePacket(final PlayerData playerData) {
        byte[] leaveBytes = com.ghost.api.proto.ProtoConverter.serializeUuidPacket(MessageType.LEAVE, playerData.uuid());
        try {
            broadcast(CryptoUtil.encrypt(leaveBytes, serverPassword));
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt leave packet", e);
        }
    }

    private void sendUpdatePacket(final PlayerData sendData, final WebSocket exclude) {
        final byte[] updateBytes = com.ghost.api.proto.ProtoConverter.serializeUpdatePacket(sendData);
        final byte[] despawnBytes = com.ghost.api.proto.ProtoConverter.serializeUuidPacket(MessageType.DESPAWN, sendData.uuid());

        final byte[] encryptedUpdate;
        final byte[] encryptedDespawn;
        try {
            encryptedUpdate = CryptoUtil.encrypt(updateBytes, serverPassword);
            encryptedDespawn = CryptoUtil.encrypt(despawnBytes, serverPassword);
        } catch (Exception e) {
            LOGGER.error("Encryption failed for update packet", e);
            return;
        }
        final var targetUuid = sendData.uuid();

        final var excludeSession = sessions.get(exclude);

        sessions.entrySet().stream()
                .filter((entry -> !exclude.equals(entry.getKey())))
                .filter((entry) -> entry.getValue().isAuthenticated())
                .filter((entry) -> entry.getValue().playerData() != null)
                .forEach((entry) -> {
                    var sock = entry.getKey();
                    var clientData = entry.getValue();
                    var tracked = clientData.trackedPlayers();
                    var entryUuid = clientData.playerData().uuid();

                    if (within_range(sendData, clientData.playerData())) {
                        // 範囲内
                        if (tracked.add(targetUuid)) {
                            var excludePlayerData = entry.getValue().playerData();
                            var excludeUpdateBytes = com.ghost.api.proto.ProtoConverter.serializeUpdatePacket(excludePlayerData);
                            try {
                                exclude.send(CryptoUtil.encrypt(excludeUpdateBytes, serverPassword));
                            } catch (Exception e) {
                                LOGGER.error("Failed to encrypt exclude update", e);
                            }
                        }
                        sock.send(encryptedUpdate);
                    } else if (tracked.contains(targetUuid)) {
                        // 追跡中だったが範囲外に出た: DESPAWN送信してリストから削除
                        tracked.remove(targetUuid);
                        sock.send(encryptedDespawn);

                        var excludeDespawnBytes = com.ghost.api.proto.ProtoConverter.serializeUuidPacket(MessageType.DESPAWN, entryUuid);
                        excludeSession.trackedPlayers().remove(entryUuid);
                        try {
                            exclude.send(CryptoUtil.encrypt(excludeDespawnBytes, serverPassword));
                        } catch (Exception e) {
                            LOGGER.error("Failed to encrypt exclude despawn", e);
                        }
                    }
                });
    }

    private boolean within_range(PlayerData pl1, PlayerData pl2) {
        var diff_x = pl1.pos().x() - pl2.pos().x();
        var diff_z = pl1.pos().z() - pl2.pos().z();
        var horizonal_distance = diff_x * diff_x + diff_z * diff_z;
        return pl1.dimension().equals(pl2.dimension())
                && horizonal_distance < visibleRangeSqr;
    }

    private final java.util.concurrent.atomic.AtomicBoolean isShuttingDown = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 全クライアントへ切断理由 (1001: Going Away) を通知し、サーバーを正常停止します。
     * ServerConsole や ShutdownHook から呼び出されます。
     */
    public void shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) {
            return; // 既にシャットダウン処理中または完了済み
        }
        LOGGER.info("Shutting down the server...");
        for (WebSocket conn : getConnections()) {
            conn.close(1001, "Server is shutting down");
        }
        try {
            stop(1000); // 接続の終了待ち時間 (1秒)
            LOGGER.info("Server stopped successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Server shutdown was interrupted", e);
        }
    }

    // =========================================================================
    // エントリポイント
    // =========================================================================

    /**
     * 起動引数の定義（Picocli）。
     * 優先度: コマンドライン引数 > 設定ファイル > デフォルト値
     */
    @Command(
            name = "GhostModServer",
            mixinStandardHelpOptions = true,
            description = "Ghost Mod Server - WebSocket-based ghost player synchronization server"
    )
    static class ServerArgs implements Runnable {

        @Option(names = "--port", description = "Port number to listen on (Default: 8887)")
        Integer port;

        @Option(names = "--password", description = "Server password (Generated automatically if not specified)")
        String password;

        @Option(names = "--config", defaultValue = "server.properties", description = "Config file path")
        String configFile;

        @Option(names = "--view-distance", description = "View distance in blocks (Default: 80.0)")
        Double viewDistance;

        @Option(names = "--rate-limit", description = "Rate limit in packets/second (Default: 50.0)")
        Double rateLimit;

        @Override
        public void run() {
            // --- 設定ファイル読み込み ---
            java.util.Properties props = new java.util.Properties();
            java.io.File file = new java.io.File(configFile);
            if (file.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    props.load(fis);
                    LOGGER.info("Loaded configuration from {}", configFile);
                } catch (java.io.IOException e) {
                    LOGGER.warn("Failed to load config file: {}", e.getMessage());
                }
            } else {
                LOGGER.info("Config file {} not found. Using defaults.", configFile);
            }

            // --- 優先度: CLI > Properties > Default ---
            int resolvedPort = port != null ? port
                    : props.containsKey("port") ? Integer.parseInt(props.getProperty("port"))
                    : 8887;

            String resolvedPassword = password != null ? password
                    : props.getProperty("password"); // null の場合は自動生成へ

            double resolvedViewDistance = viewDistance != null ? viewDistance
                    : props.containsKey("view-distance") ? Double.parseDouble(props.getProperty("view-distance"))
                    : 5 * 16.0; // デフォルト 5 chunk

            double resolvedRateLimit = rateLimit != null ? rateLimit
                    : props.containsKey("rate-limit") ? Double.parseDouble(props.getProperty("rate-limit"))
                    : 50.0;

            // --- パスワード自動生成 ---
            if (resolvedPassword == null || resolvedPassword.isEmpty()) {
                int randomSuffix = (int) (Math.random() * 10000);
                resolvedPassword = String.format("changeme%04d", randomSuffix);
                LOGGER.warn("No password provided. Using generated password: '{}'", resolvedPassword);
            }

            // --- サーバー起動 ---
            try {
                GhostModServer server = new GhostModServer(resolvedPort, resolvedPassword, resolvedViewDistance, resolvedRateLimit);

                // JVM 終了時のシャットダウンフック（SIGINT / プロセスキル時にも安全に停止）
                Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Server-Shutdown-Hook"));

                server.start(); // WebSocket サーバーを別スレッドで起動
                new ServerConsole(server).start(); // コンソールループ（stop / Ctrl+C まで待機）
            } catch (Exception ex) {
                LOGGER.error("Failed to start WebSocket server", ex);
            }
        }
    }

    public static void main(String[] args) {
        new CommandLine(new ServerArgs()).execute(args);
    }
}
