package com.ghost.server;

import com.ghost.api.dto.AuthData;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import com.ghost.api.proto.ProtoConverter;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.CryptoUtil;
import com.ghost.util.SerializationUtil;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class GhostModServer extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModServer.class);

    private static final int CLOSE_AUTH_FAILED = 4001;
    private static final int CLOSE_HANDSHAKE_ERROR = 4002;
    private static final int CLOSE_DECRYPTION_FAILED = 4003;

    private final ServerConfig config;
    private final String serverPassword;
    private final VisibilityTracker<WebSocket> visibilityTracker;
    private final RateLimiter.PerIpManager rateLimiterManager;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public GhostModServer(ServerConfig config) {
        super(new InetSocketAddress(config.port()));
        this.config = config;
        this.serverPassword = config.password();
        this.visibilityTracker = new VisibilityTracker<>(config.viewDistance());
        this.rateLimiterManager = new RateLimiter.PerIpManager(config.rateLimit());
    }

    @Override
    public void onStart() {
        LOGGER.info("Server started on port: {}", getPort());
        LOGGER.info("Authentication enabled using CHAP.");
        LOGGER.info("Visibility Range: {} blocks", config.viewDistance());
        LOGGER.info("Rate Limit: {} packets/sec", config.rateLimit());
        LOGGER.debug("\n=== DEBUG MODE ===\n");
        setConnectionLostTimeout(100);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("New connection from: {}", conn.getRemoteSocketAddress());

        String nonce = ChapAuthenticator.generateNonce();
        visibilityTracker.registerConnection(conn, nonce);

        AuthData authData = new AuthData(nonce, null);
        GhostPacket<AuthData> challengePacket = new GhostPacket<>(MessageType.AUTH_CHALLENGE, authData);
        try {
            conn.send(CryptoUtil.encrypt(SerializationUtil.serializePacket(challengePacket), serverPassword));
            LOGGER.info("Sent AUTH_CHALLENGE to {}", conn.getRemoteSocketAddress());
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt challenge for {}", conn.getRemoteSocketAddress(), e);
            conn.close(CLOSE_HANDSHAKE_ERROR, "Handshake Error");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (conn == null) {
            return;
        }
        var result = visibilityTracker.removeConnection(conn);
        PlayerData disconnectedPlayer = result.disconnectedPlayer();
        if (disconnectedPlayer != null) {
            LOGGER.info("Player {} disconnected: {} (Code: {}, Reason: {}, Remote: {})",
                    disconnectedPlayer.name(), conn.getRemoteSocketAddress(), code, reason, remote);
            sendLeavePacket(disconnectedPlayer, result.authenticatedNotifyTargets());
        } else {
            LOGGER.debug("Disconnected client without active player state: {}", conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (isRateLimited(conn)) {
            return;
        }

        // スライスや DirectBuffer でも安全に読み出せるよう remaining() 分のみ抽出する
        byte[] rawBytes = new byte[message.remaining()];
        message.get(rawBytes);

        byte[] decryptedBytes;
        try {
            decryptedBytes = CryptoUtil.decrypt(rawBytes, serverPassword);
        } catch (Exception e) {
            LOGGER.warn("Failed to decrypt binary message from {}, closing connection.", conn.getRemoteSocketAddress());
            conn.close(CLOSE_DECRYPTION_FAILED, "Decryption Failed");
            return;
        }

        var sessionOpt = visibilityTracker.getSession(conn);
        if (sessionOpt.isEmpty() || !sessionOpt.get().isAuthenticated()) {
            LOGGER.warn("Received binary message from unauthenticated client: {}", conn.getRemoteSocketAddress());
            return;
        }
        GhostClientData session = sessionOpt.get();

        try {
            MessageType type = ProtoConverter.deserializePacketType(decryptedBytes);
            if (type == MessageType.UPDATE) {
                PlayerData playerData = ProtoConverter.deserializeUpdatePacket(decryptedBytes);
                if (session.playerData() == null) {
                    LOGGER.info("Player initialized: {} ({})", playerData.name(), conn.getRemoteSocketAddress());
                }
                dispatchPlayerUpdate(conn, playerData);
            }
        } catch (Exception ex) {
            LOGGER.error("Error processing binary message from {}", conn.getRemoteSocketAddress(), ex);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (isRateLimited(conn)) {
            return;
        }

        String decryptedMessage;
        try {
            decryptedMessage = CryptoUtil.decrypt(message, serverPassword);
        } catch (Exception e) {
            LOGGER.warn("Failed to decrypt message from {}, closing connection.", conn.getRemoteSocketAddress());
            conn.close(CLOSE_DECRYPTION_FAILED, "Decryption Failed");
            return;
        }

        var sessionOpt = visibilityTracker.getSession(conn);
        if (sessionOpt.isEmpty()) {
            return;
        }
        GhostClientData session = sessionOpt.get();
        if (!session.isAuthenticated()) {
            handleAuthHandshake(conn, session, decryptedMessage);
            return;
        }

        LOGGER.debug("Message from {}: {}", conn.getRemoteSocketAddress(), decryptedMessage);
    }

    private boolean isRateLimited(WebSocket conn) {
        InetSocketAddress remoteAddr = conn.getRemoteSocketAddress();
        if (remoteAddr == null || remoteAddr.getAddress() == null) {
            return true;
        }
        InetAddress address = remoteAddr.getAddress();
        if (!rateLimiterManager.tryAcquire(address)) {
            LOGGER.warn("Rate limit exceeded for {}", address);
            return true;
        }
        return false;
    }

    private void handleAuthHandshake(WebSocket conn, GhostClientData session, String message) {
        try {
            var packet = SerializationUtil.deserializePacket(message);

            if (packet != null && packet.getType() == MessageType.AUTH_RESPONSE) {
                AuthData authData = SerializationUtil.parseAuthData(packet.getData());
                String clientHash = authData != null ? authData.hash() : null;
                String nonce = session.nonce();

                if (ChapAuthenticator.verify(serverPassword, nonce, clientHash)) {
                    visibilityTracker.markAuthenticated(conn);
                    LOGGER.info("Authentication SUCCESS for {}", conn.getRemoteSocketAddress());

                    GhostPacket<Void> successPacket = new GhostPacket<>(MessageType.AUTH_SUCCESS, null);
                    try {
                        conn.send(CryptoUtil.encrypt(SerializationUtil.serializePacket(successPacket), serverPassword));
                    } catch (Exception e) {
                        LOGGER.error("Failed to encrypt auth success", e);
                    }
                } else {
                    LOGGER.warn("Authentication FAILED for {}. Closing connection.", conn.getRemoteSocketAddress());
                    conn.close(CLOSE_AUTH_FAILED, "Authentication Failed");
                }
            } else {
                LOGGER.warn(
                        "Expected AUTH_RESPONSE but received different packet/invalid data from unauthenticated client: {}",
                        conn.getRemoteSocketAddress());
            }
        } catch (Exception e) {
            LOGGER.error("Auth handshake error for {}", conn.getRemoteSocketAddress(), e);
            conn.close(CLOSE_HANDSHAKE_ERROR, "Handshake Error");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            LOGGER.error("An error occurred on connection {}:", conn.getRemoteSocketAddress(), ex);
            var result = visibilityTracker.removeConnection(conn);
            if (result.disconnectedPlayer() != null) {
                sendLeavePacket(result.disconnectedPlayer(), result.authenticatedNotifyTargets());
            }
        } else {
            LOGGER.error("An error occurred:", ex);
        }
    }

    private void sendLeavePacket(PlayerData playerData, List<WebSocket> targets) {
        if (targets.isEmpty()) {
            return;
        }
        byte[] leaveBytes = ProtoConverter.serializeUuidPacket(MessageType.LEAVE, playerData.uuid());
        byte[] encryptedLeave;
        try {
            encryptedLeave = CryptoUtil.encrypt(leaveBytes, serverPassword);
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt leave packet", e);
            return;
        }
        for (WebSocket target : targets) {
            if (target.isOpen()) {
                target.send(encryptedLeave);
            }
        }
    }

    private void dispatchPlayerUpdate(WebSocket sender, PlayerData sendData) {
        var result = visibilityTracker.updatePlayer(sender, sendData);

        // 1. 視認範囲内のピアへ送信者の UPDATE を配信
        if (!result.notifyUpdateTargets().isEmpty()) {
            byte[] encryptedUpdate = encryptUpdate(sendData);
            if (encryptedUpdate != null) {
                for (WebSocket peer : result.notifyUpdateTargets()) {
                    if (peer.isOpen()) {
                        peer.send(encryptedUpdate);
                    }
                }
            }
        }

        // 2. 新規に視認範囲へ入ったピアの最新状態を送信者へバックフィル
        for (PlayerData peerData : result.backfillToSender()) {
            if (!sender.isOpen()) {
                break;
            }
            byte[] encryptedPeerUpdate = encryptUpdate(peerData);
            if (encryptedPeerUpdate != null) {
                sender.send(encryptedPeerUpdate);
            }
        }

        // 3. 範囲外へ出たピアへ送信者の DESPAWN を配信（必要なときだけ遅延暗号化）
        if (!result.notifyDespawnTargets().isEmpty()) {
            byte[] encryptedDespawn = encryptDespawn(sendData.uuid());
            if (encryptedDespawn != null) {
                for (WebSocket peer : result.notifyDespawnTargets()) {
                    if (peer.isOpen()) {
                        peer.send(encryptedDespawn);
                    }
                }
            }
        }

        // 4. 範囲外になったピアの DESPAWN を送信者へ通知
        for (String despawnPeerUuid : result.despawnToSender()) {
            if (!sender.isOpen()) {
                break;
            }
            byte[] encryptedPeerDespawn = encryptDespawn(despawnPeerUuid);
            if (encryptedPeerDespawn != null) {
                sender.send(encryptedPeerDespawn);
            }
        }
    }

    private byte[] encryptUpdate(PlayerData data) {
        try {
            return CryptoUtil.encrypt(ProtoConverter.serializeUpdatePacket(data), serverPassword);
        } catch (Exception e) {
            LOGGER.error("Encryption failed for update packet", e);
            return null;
        }
    }

    private byte[] encryptDespawn(String uuid) {
        try {
            return CryptoUtil.encrypt(ProtoConverter.serializeUuidPacket(MessageType.DESPAWN, uuid), serverPassword);
        } catch (Exception e) {
            LOGGER.error("Encryption failed for despawn packet", e);
            return null;
        }
    }

    /**
     * 全クライアントへ切断理由 (1001: Going Away) を通知し、サーバーを正常停止します。
     * ServerConsole や ShutdownHook から呼び出されます。
     */
    public void shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) {
            return;
        }
        LOGGER.info("Shutting down the server...");
        for (WebSocket conn : getConnections()) {
            conn.close(1001, "Server is shutting down");
        }
        try {
            stop(1000);
            LOGGER.info("Server stopped successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Server shutdown was interrupted", e);
        }
    }

    // =========================================================================
    // エントリポイント
    // =========================================================================

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
            Properties props = ServerConfig.loadProperties(Path.of(configFile));
            ServerConfig serverConfig;
            try {
                serverConfig = ServerConfig.resolve(port, password, viewDistance, rateLimit, props);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid server configuration: {}", e.getMessage());
                return;
            }

            if (serverConfig.passwordAutoGenerated()) {
                LOGGER.warn("No password provided. Using generated password: '{}'", serverConfig.password());
            }

            try {
                GhostModServer server = new GhostModServer(serverConfig);

                Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Server-Shutdown-Hook"));

                server.start();
                new ServerConsole(server).start();
            } catch (Exception ex) {
                LOGGER.error("Failed to start WebSocket server", ex);
            }
        }
    }

    public static void main(String[] args) {
        new CommandLine(new ServerArgs()).execute(args);
    }
}
