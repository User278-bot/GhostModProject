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
import java.util.stream.Collectors;

public class GhostModServer extends WebSocketServer {

    // 接続中のクライアントをスレッドセーフに管理するためのセット
    private static final Map<WebSocket, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModServer.class);

    // 認証関連
    private static final Map<WebSocket, String> pendingChallenges = new ConcurrentHashMap<>(); // Connection -> Nonce
    private static final Map<WebSocket, Boolean> authenticatedSessions = new ConcurrentHashMap<>(); // Connection ->
    // IsAuthenticated
    private final String serverPassword;

    // レート制限関連
    private static final Map<InetAddress, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private static final double PACKETS_PER_SECOND = 20.0; // 1秒あたりの許容パケット数

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
        String nonce = ChapAuthenticator.generateNonce();
        pendingChallenges.put(conn, nonce);
        authenticatedSessions.put(conn, false);

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
        pendingChallenges.remove(conn);
        authenticatedSessions.remove(conn);

        var disconnectedPlayer = playerDataMap.remove(conn);
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
        if (!Boolean.TRUE.equals(authenticatedSessions.get(conn))) {
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
                var previous = playerDataMap.put(conn, playerData);
                if (previous == null) {
                    var joinedPlayer = SerializationUtil.parsePlayerData(packet.getData());
                    sendJoinPacket(conn, joinedPlayer);
                } else {
                    // ★重要：受信したメッセージを、送信元以外の全クライアントに転送（ブロードキャスト）する
                    broadcast(message, conn);
                }
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
                AuthData authData = SerializationUtil.parseAuthData(packet.getData());
                String clientHash = authData.hash();
                String nonce = pendingChallenges.get(conn);

                if (ChapAuthenticator.verify(serverPassword, nonce, clientHash)) {
                    // 認証成功
                    authenticatedSessions.put(conn, true);
                    pendingChallenges.remove(conn); // nonceはもう不要
                    LOGGER.info("Authentication SUCCESS for {}", conn.getRemoteSocketAddress());

                    // 認証成功パケットを送信
                    GhostPacket<Void> successPacket = new GhostPacket<>(MessageType.AUTH_SUCCESS, null);
                    conn.send(SerializationUtil.serializePacket(successPacket));

                    // 初期同期パケットを送るなど、通常のフローへ
                    // (現状の実装では、クライアントがUPDATEを送ってきたタイミングでJOIN扱いになるため、ここでは何もしなくてよい(大嘘))
                    sendInitialPaket(conn);
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
            playerDataMap.remove(conn);
            authenticatedSessions.remove(conn);
            pendingChallenges.remove(conn);
        } else {
            LOGGER.error("An error occurred:", ex);
        }
    }

    /**
     * 特定のクライアントを除いて、接続中の全クライアントにメッセージを送信する
     *
     * @param message 送信するメッセージ
     * @param exclude 除外するクライアント
     */
    public void broadcast(String message, WebSocket exclude) {
        // スレッドセーフなSetを安全にループするため、synchronizedブロックで囲みます
        var connections = this.getConnections();
        synchronized (connections) {
            for (WebSocket conn : connections) {
                // 認証済みのクライアントにのみブロードキャストする
                if (conn != null && conn.isOpen() && !conn.equals(exclude)
                        && Boolean.TRUE.equals(authenticatedSessions.get(conn))) {
                    conn.send(message);
                }
            }
        }
    }

    private void sendInitialPaket(WebSocket newConnection) {
        var excludePlayers = playerDataMap.entrySet().stream()
                .filter(
                        (entry) -> !entry.getKey().equals(newConnection))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        if (excludePlayers.isEmpty()) {
            return;
        }

        var packet = new GhostPacket<>(MessageType.INITIAL_SYNC, excludePlayers);
        var msg = SerializationUtil.serializePacket(packet);
        newConnection.send(msg);
    }

    private void sendLeavePacket(final PlayerData playerData) {
        var leavePacket = new GhostPacket<>(MessageType.LEAVE, playerData.uuid());
        var msg = SerializationUtil.serializePacket(leavePacket);
        broadcast(msg);
    }

    private void sendJoinPacket(WebSocket conn, PlayerData joinedPlayer) {
        var joinPacket = new GhostPacket<>(MessageType.JOIN, joinedPlayer);
        var msg = SerializationUtil.serializePacket(joinPacket);
        broadcast(msg, conn);
        LOGGER.info("Player '{}' joined.", joinedPlayer.name());
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
