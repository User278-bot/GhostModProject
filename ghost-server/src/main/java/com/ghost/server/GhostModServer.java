package com.ghost.server;

import com.ghost.api.dto.AuthData;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.SerializationUtil;
import com.google.common.util.concurrent.RateLimiter;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GhostModServer extends WebSocketServer {

    private static final Map<WebSocket, GhostClientData> sessions = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModServer.class);

    private final String serverPassword;

    // レート制限関連
    private static final Map<InetAddress, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private static final double PACKETS_PER_SECOND = 50.0; // 1秒あたりの許容パケット数 (Tickレート20Hz + ゆとり)

    private static final int GHOST_VISIBLE_CHUNK_RANGE = 5 * 5;

    public GhostModServer(int port, String password) {
        super(new InetSocketAddress(port));
        this.serverPassword = password;
    }

    @Override
    public void onStart() {
        // サーバーが起動したときの処理
        LOGGER.info("Server started on port: {}", getPort());
        LOGGER.info("Authentication enabled using CHAP.");
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
        conn.send(SerializationUtil.serializePacket(challengePacket));

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
    public void onMessage(WebSocket conn, String message) {
        // レート制限チェック
        InetAddress address = conn.getRemoteSocketAddress().getAddress();
        RateLimiter limiter = rateLimiters.computeIfAbsent(address, k -> RateLimiter.create(PACKETS_PER_SECOND));

        if (!limiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for {}", address);
            return; // 制限超過時はメッセージを無視
        }

        // 認証チェック
        var session = sessions.get(conn);
        if (session == null) {
            return;
        }
        if (!session.isAuthenticated()) {
            handleAuthHandshake(conn, message);
            return;
        }

        LOGGER.debug("Message from {}: {}", conn.getRemoteSocketAddress(), message);

        try {
            var packet = SerializationUtil.deserializePacket(message);
            if (packet == null) {
                return;
            }

            // 認証済みセッションでの処理
            if (packet.getType() == MessageType.UPDATE) {
                var playerData = SerializationUtil.parsePlayerData(packet.getData());
                if (playerData == null) {
                    LOGGER.warn("Failed to parse PlayerData from {}", conn.getRemoteSocketAddress());
                    return;
                }
                if (session.playerData() == null) {
                    // 初回パケット受信時はJOINパケットを送らず、UPDATEのみ（クライアント側で処理）
                    // 何もしない、あるいはログ出力のみ
                    LOGGER.info("Player initialized: {}", conn.getRemoteSocketAddress());
                } else {
                    sendUpdatePacket(playerData, conn);
                }

                // 追跡状態（trackedPlayers）を引き継いで更新
                sessions.put(conn, new GhostClientData(session.nonce(), true, playerData, session.trackedPlayers()));
            }
        } catch (Exception ex) {
            LOGGER.error("Error processing message from {}", conn.getRemoteSocketAddress(), ex);
        }
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
                    conn.send(SerializationUtil.serializePacket(successPacket));

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
        var leavePacket = new GhostPacket<>(MessageType.LEAVE, playerData.uuid());
        var msg = SerializationUtil.serializePacket(leavePacket);
        broadcast(msg);
    }

    private void sendUpdatePacket(final PlayerData sendData, final WebSocket exclude) {
        final var packet = new GhostPacket<>(MessageType.UPDATE, sendData);
        final var despawnPacket = new GhostPacket<>(MessageType.DESPAWN, sendData.uuid());
        final var update = SerializationUtil.serializePacket(packet);
        final var despawn = SerializationUtil.serializePacket(despawnPacket);
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
                            var excludePacket = new GhostPacket<>(MessageType.UPDATE, excludePlayerData);
                            var excludeMsg = SerializationUtil.serializePacket(excludePacket);
                            exclude.send(excludeMsg);
                        }
                        sock.send(update);
                    } else if (tracked.contains(targetUuid)) {
                        // 追跡中だったが範囲外に出た: DESPAWN送信してリストから削除
                        tracked.remove(targetUuid);
                        sock.send(despawn);

                        var excludePacket = new GhostPacket<>(MessageType.DESPAWN, entryUuid);
                        var excludeMsg = SerializationUtil.serializePacket(excludePacket);
                        excludeSession.trackedPlayers().remove(entryUuid);
                        exclude.send(excludeMsg);
                    }
                });
    }

    private boolean within_range(PlayerData pl1, PlayerData pl2) {
        var diff_x = pl1.pos().x() - pl2.pos().x();
        var diff_z = pl1.pos().z() - pl2.pos().z();
        var horizonal_distance = diff_x * diff_x + diff_z * diff_z;
        return pl1.dimension().equals(pl2.dimension())
                && horizonal_distance < GHOST_VISIBLE_CHUNK_RANGE;
    }

    public static void main(String[] args) {
        int port = 8887; // デフォルトポート
        String password = "changeme"; // デフォルトパスワード

        // --- コマンドライン引数の定義 ---
        Options options = new Options();
        options.addOption(Option.builder().longOpt("port").hasArg().desc("Server Port").build());
        options.addOption(Option.builder().longOpt("password").hasArg().desc("Server Password").build());

        // --- 引数の解析 ---
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("port")) {
                port = Integer.parseInt(cmd.getOptionValue("port"));
            }
            if (cmd.hasOption("password")) {
                password = cmd.getOptionValue("password");
            } else {
                // デフォルトパスワードにランダムな4桁の数字を付与
                int randomSuffix = (int) (Math.random() * 10000);
                password += String.format("%04d", randomSuffix);
                LOGGER.warn("No password provided. Using generated password: '{}'", password);
                LOGGER.warn("Please use --password <your_password> to set a secure password.");
            }
        } catch (ParseException e) {
            LOGGER.error("Failed to parse command line arguments: {}", e.getMessage());
            return;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid port number format.");
            return;
        }

        try {
            GhostModServer server = new GhostModServer(port, password);
            server.start(); // サーバーを起動
        } catch (Exception ex) {
            LOGGER.error("Failed to start WebSocket server", ex);
        }
    }
}
