package com.ghost.net;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.proto.AuthChallenge;
import com.ghost.api.proto.GhostPacketProto;
import com.ghost.api.proto.ProtoConverter;
import com.ghost.api.registry.IGhostRegistry;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.CryptoUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ghost サーバーと暗号化バイナリ Protobuf で通信する WebSocket クライアント（1 接続 1 インスタンス）。
 */
public class GhostWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostWebSocketClient.class);

    private final IGhostRegistry ghostRegistry;
    private final String password;
    private final SecretKey secretKey;
    private final CompletableFuture<Boolean> authFuture = new CompletableFuture<>();
    @Nullable
    private final Runnable onDisconnectCallback;
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);

    public GhostWebSocketClient(URI serverURI,
                                IGhostRegistry registry,
                                String password,
                                @Nullable Runnable onDisconnectCallback) {
        super(serverURI);
        this.ghostRegistry = registry;
        this.password = password;
        this.secretKey = CryptoUtil.deriveKey(password);
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public CompletableFuture<Boolean> getAuthFuture() {
        return authFuture;
    }

    @Override
    public void send(byte[] bytes) {
        try {
            super.send(CryptoUtil.encrypt(bytes, secretKey));
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt binary message before sending", e);
            close();
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public void onOpen(ServerHandshake data) {
        LOGGER.info("Successfully connected to the server (status: {})", data.getHttpStatus());
    }

    @Override
    public void onMessage(ByteBuffer blob) {
        try {
            // スライスや DirectBuffer でも安全に読み出せるよう remaining() 分のみ抽出する
            byte[] rawBytes = new byte[blob.remaining()];
            blob.get(rawBytes);

            byte[] decryptedBytes = CryptoUtil.decrypt(rawBytes, secretKey);
            GhostPacketProto packet = ProtoConverter.decode(decryptedBytes);
            LOGGER.debug("Received binary packet payload: {}", packet.getPayloadCase());

            switch (packet.getPayloadCase()) {
                case UPDATE -> {
                    PlayerData playerData = ProtoConverter.fromProto(packet.getUpdate());
                    ghostRegistry.updateGhost(playerData);
                }
                case LEAVE_UUID -> ghostRegistry.removeGhost(packet.getLeaveUuid());
                case DESPAWN_UUID -> ghostRegistry.removeGhost(packet.getDespawnUuid());
                case AUTH_CHALLENGE -> handleAuthChallenge(packet.getAuthChallenge());
                case AUTH_SUCCESS -> {
                    LOGGER.debug("Received AUTH_SUCCESS. Authentication verified.");
                    authFuture.complete(true);
                }
                default -> LOGGER.warn("Received unexpected binary packet payload: {}", packet.getPayloadCase());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to process WebSocket binary message", ex);
            if (!authFuture.isDone()) {
                authFuture.completeExceptionally(
                        new SecurityException("Decryption or processing failed. Invalid password or protocol?", ex));
            }
            close();
        }
    }

    private void handleAuthChallenge(AuthChallenge challenge) throws Exception {
        int serverVersion = challenge.getProtocolVersion();
        if (serverVersion != ProtoConverter.PROTOCOL_VERSION) {
            String msg = String.format(
                    "Protocol version mismatch: server=%d, client=%d",
                    serverVersion, ProtoConverter.PROTOCOL_VERSION);
            LOGGER.error(msg);
            authFuture.completeExceptionally(new IllegalStateException(msg));
            close();
            return;
        }

        String hash = ChapAuthenticator.calculateHash(password, challenge.getNonce());
        send(ProtoConverter.encodeAuthResponse(hash));
        LOGGER.debug("Sent AUTH_RESPONSE (protocol_version={}).", ProtoConverter.PROTOCOL_VERSION);
    }

    @Override
    public void onMessage(String message) {
        LOGGER.warn("Received unsupported text WebSocket frame (binary protocol required). Closing connection.");
        if (!authFuture.isDone()) {
            authFuture.completeExceptionally(new IllegalStateException("Unsupported text frame from server"));
        }
        close();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("Disconnected from server. code: {}, Reason: {}, Remote: {}", code, reason, remote);
        ghostRegistry.clear();
        if (!authFuture.isDone()) {
            authFuture.complete(false);
        }
        notifyDisconnectOnce();
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("An error occurred in WebSocket client: ", ex);
        ghostRegistry.clear();
        if (!authFuture.isDone()) {
            authFuture.completeExceptionally(ex);
        }
        notifyDisconnectOnce();
    }

    private void notifyDisconnectOnce() {
        if (onDisconnectCallback != null && disconnectNotified.compareAndSet(false, true)) {
            try {
                onDisconnectCallback.run();
            } catch (Exception e) {
                LOGGER.warn("Disconnect callback threw an exception", e);
            }
        }
    }
}
