package com.ghost.server;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.proto.AuthResponse;
import com.ghost.api.proto.GhostPacketProto;
import com.ghost.api.proto.ProtoConverter;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.CryptoUtil;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ghost Mod 用の WebSocket 同期サーバー。
 * 設定解決は {@link ServerConfig}、レート制限は {@link RateLimiter.PerIpManager}、
 * セッション＆視認範囲追跡は {@link VisibilityTracker} へ責務を委譲し、
 * 本クラスは WebSocket 通信制御・暗号化・CHAP 認証に専念します。
 */
public class GhostModServer extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModServer.class);

    private static final int CLOSE_AUTH_FAILED = 4001;
    private static final int CLOSE_HANDSHAKE_ERROR = 4002;
    private static final int CLOSE_DECRYPTION_FAILED = 4003;
    private static final int CLOSE_PROTOCOL_MISMATCH = 4004;
    private static final int CLOSE_UNSUPPORTED_FRAME = 4005;

    private final String serverPassword;
    private final SecretKey secretKey;
    private final double visibleRange;
    private final double packetsPerSecond;
    private final RateLimiter.PerIpManager rateLimiterManager;
    private final VisibilityTracker<WebSocket> visibilityTracker;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public GhostModServer(ServerConfig config) {
        this(config.port(), config.password(), config.viewDistance(), config.rateLimit());
    }

    public GhostModServer(int port, String password, double visibleRange, double packetsPerSecond) {
        super(new InetSocketAddress(port));
        this.serverPassword = password;
        this.secretKey = CryptoUtil.deriveKey(password);
        this.visibleRange = visibleRange;
        this.packetsPerSecond = packetsPerSecond;
        this.rateLimiterManager = new RateLimiter.PerIpManager(packetsPerSecond);
        this.visibilityTracker = new VisibilityTracker<>(visibleRange);
    }

    @Override
    public void onStart() {
        LOGGER.info("Server started on port: {}", getPort());
        LOGGER.info("Authentication enabled using HMAC-SHA256 CHAP (protocol v{}).", ProtoConverter.PROTOCOL_VERSION);
        LOGGER.info("Visibility Range: {} blocks", visibleRange);
        LOGGER.info("Rate Limit: {} packets/sec", packetsPerSecond);
        LOGGER.debug("=== DEBUG MODE ===");
        setConnectionLostTimeout(100);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("New connection from: {}", conn.getRemoteSocketAddress());

        String nonce = ChapAuthenticator.generateNonce();
        visibilityTracker.registerConnection(conn, nonce);

        sendEncrypted(conn, ProtoConverter.encodeAuthChallenge(nonce), "AUTH_CHALLENGE");
        LOGGER.info("Sent AUTH_CHALLENGE to {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
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
            decryptedBytes = CryptoUtil.decrypt(rawBytes, secretKey);
        } catch (Exception e) {
            LOGGER.warn("Failed to decrypt binary message from {}, closing connection.", conn.getRemoteSocketAddress());
            conn.close(CLOSE_DECRYPTION_FAILED, "Decryption Failed");
            return;
        }

        var sessionOpt = visibilityTracker.getSession(conn);
        if (sessionOpt.isEmpty()) {
            return;
        }
        GhostClientData session = sessionOpt.get();

        GhostPacketProto packet;
        try {
            packet = ProtoConverter.decode(decryptedBytes);
        } catch (Exception ex) {
            LOGGER.warn("Failed to decode Protobuf packet from {}", conn.getRemoteSocketAddress(), ex);
            if (!session.isAuthenticated()) {
                conn.close(CLOSE_HANDSHAKE_ERROR, "Handshake Error");
            }
            return;
        }

        if (!session.isAuthenticated()) {
            handleAuthHandshake(conn, session, packet);
            return;
        }

        if (packet.getPayloadCase() == GhostPacketProto.PayloadCase.UPDATE) {
            PlayerData playerData = ProtoConverter.fromProto(packet.getUpdate());
            if (session.playerData() == null) {
                LOGGER.info("Player initialized: {} ({})", playerData.name(), conn.getRemoteSocketAddress());
            }
            dispatchPlayerUpdate(conn, playerData);
        } else {
            LOGGER.debug("Ignoring unsupported client packet payload: {}", packet.getPayloadCase());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (isRateLimited(conn)) {
            return;
        }
        LOGGER.warn("Received unsupported text frame from {}. Binary protocol is required.",
                conn.getRemoteSocketAddress());
        conn.close(CLOSE_UNSUPPORTED_FRAME, "Binary Protocol Required");
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

    private void handleAuthHandshake(WebSocket conn, GhostClientData session, GhostPacketProto packet) {
        if (packet.getPayloadCase() != GhostPacketProto.PayloadCase.AUTH_RESPONSE) {
            LOGGER.warn("Expected AUTH_RESPONSE but received {} from {}",
                    packet.getPayloadCase(), conn.getRemoteSocketAddress());
            conn.close(CLOSE_HANDSHAKE_ERROR, "Unexpected Packet During Auth");
            return;
        }

        AuthResponse response = packet.getAuthResponse();
        if (response.getProtocolVersion() != ProtoConverter.PROTOCOL_VERSION) {
            LOGGER.warn("Protocol version mismatch from {}: client={}, server={}",
                    conn.getRemoteSocketAddress(), response.getProtocolVersion(), ProtoConverter.PROTOCOL_VERSION);
            conn.close(CLOSE_PROTOCOL_MISMATCH, "Protocol Version Mismatch");
            return;
        }

        if (ChapAuthenticator.verify(serverPassword, session.nonce(), response.getHash())) {
            visibilityTracker.markAuthenticated(conn);
            LOGGER.info("Authentication SUCCESS for {}", conn.getRemoteSocketAddress());
            sendEncrypted(conn, ProtoConverter.encodeAuthSuccess(), "AUTH_SUCCESS");
        } else {
            LOGGER.warn("Authentication FAILED for {}. Closing connection.", conn.getRemoteSocketAddress());
            conn.close(CLOSE_AUTH_FAILED, "Authentication Failed");
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

    private void sendLeavePacket(PlayerData playerData, java.util.List<WebSocket> authenticatedTargets) {
        if (authenticatedTargets.isEmpty()) {
            return;
        }
        try {
            byte[] encryptedLeave = CryptoUtil.encrypt(ProtoConverter.encodeLeave(playerData.uuid()), secretKey);
            for (WebSocket target : authenticatedTargets) {
                if (target.isOpen()) {
                    target.send(encryptedLeave);
                }
            }
        } catch (GeneralSecurityException e) {
            LOGGER.error("Failed to encrypt leave packet", e);
        }
    }

    private void dispatchPlayerUpdate(WebSocket sender, PlayerData sendData) {
        var result = visibilityTracker.updatePlayer(sender, sendData);

        // 1. 視認範囲内のピアへ送信者の UPDATE を一括配信（暗号化は 1 回だけ実行）
        if (!result.notifyUpdateTargets().isEmpty()) {
            try {
                byte[] encryptedUpdate = CryptoUtil.encrypt(ProtoConverter.encodeUpdate(sendData), secretKey);
                for (WebSocket peer : result.notifyUpdateTargets()) {
                    if (peer.isOpen()) {
                        peer.send(encryptedUpdate);
                    }
                }
            } catch (GeneralSecurityException e) {
                LOGGER.error("Encryption failed for update packet", e);
            }
        }

        // 2. 新たに視認範囲内へ入ったピアの最新状態を送信者へ返送
        for (PlayerData peerData : result.backfillToSender()) {
            sendEncrypted(sender, ProtoConverter.encodeUpdate(peerData), "BACKFILL_UPDATE");
        }

        // 3. 範囲外に出たピアがいる場合のみ DESPAWN を遅延生成・暗号化して通知
        if (!result.notifyDespawnTargets().isEmpty()) {
            try {
                byte[] encryptedDespawn = CryptoUtil.encrypt(
                        ProtoConverter.encodeDespawn(sendData.uuid()), secretKey);
                for (WebSocket peer : result.notifyDespawnTargets()) {
                    if (peer.isOpen()) {
                        peer.send(encryptedDespawn);
                    }
                }
            } catch (GeneralSecurityException e) {
                LOGGER.error("Encryption failed for despawn packet", e);
            }
        }

        // 4. 範囲外になったピアの DESPAWN を送信者へ通知
        for (String peerUuid : result.despawnToSender()) {
            sendEncrypted(sender, ProtoConverter.encodeDespawn(peerUuid), "PEER_DESPAWN");
        }
    }

    private void sendEncrypted(WebSocket target, byte[] plainPayload, String label) {
        if (!target.isOpen()) {
            return;
        }
        try {
            target.send(CryptoUtil.encrypt(plainPayload, secretKey));
        } catch (GeneralSecurityException e) {
            LOGGER.error("Failed to encrypt {} packet", label, e);
        }
    }

    /**
     * 全クライアントへ切断理由 (1001: Going Away) を通知し、サーバーを正常停止します。
     * {@link ServerConsole} や ShutdownHook から呼び出されます。
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

    /**
     * 起動引数の定義（Picocli）。
     * 優先度: コマンドライン引数 &gt; 設定ファイル &gt; デフォルト値
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
            try {
                Properties props = ServerConfig.loadProperties(Path.of(configFile));
                ServerConfig config = ServerConfig.resolve(port, password, viewDistance, rateLimit, props);

                if (config.passwordAutoGenerated()) {
                    LOGGER.warn("No password provided. Using generated password: '{}'", config.password());
                }

                GhostModServer server = new GhostModServer(config);
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
