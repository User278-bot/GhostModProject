package com.ghost.net;

import com.ghost.api.dto.AuthData;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import com.ghost.api.registry.IGhostRegistry;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.SerializationUtil;
import com.ghost.util.CryptoUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class GhostWebSocketClient extends WebSocketClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(GhostWebSocketClient.class);
    private final IGhostRegistry GHOST_REGISTRY;
    private final String PASSWORD;
    private CompletableFuture<Boolean> authFuture = new CompletableFuture<>();

    public GhostWebSocketClient(URI serverURI, IGhostRegistry registry, String password) {
        super(serverURI);
        GHOST_REGISTRY = registry;
        PASSWORD = password;
    }

    public CompletableFuture<Boolean> getAuthFuture() {
        return authFuture;
    }

    @Override
    public void send(String text) {
        try {
            super.send(CryptoUtil.encrypt(text, PASSWORD));
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt message before sending", e);
            close();
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public void send(byte[] bytes) {
        try {
            super.send(CryptoUtil.encrypt(bytes, PASSWORD));
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt binary message before sending", e);
            close();
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public void onOpen(ServerHandshake data) {
        LOGGER.info("Successfully connected to the server (status: {})", data.getHttpStatus());
        if (authFuture.isDone()) {
            authFuture = new CompletableFuture<>();
        }
    }

    @Override
    public void onMessage(ByteBuffer blob) {
        try {
            byte[] rawBytes = blob.array();
            byte[] decryptedBytes = CryptoUtil.decrypt(rawBytes, PASSWORD);
            MessageType type = com.ghost.api.proto.ProtoConverter.deserializePacketType(decryptedBytes);
            LOGGER.debug("Received binary packet type: {}", type);

            switch (type) {
                case UPDATE:
                    PlayerData playerData = com.ghost.api.proto.ProtoConverter.deserializeUpdatePacket(decryptedBytes);
                    GHOST_REGISTRY.updateGhost(playerData);
                    break;
                case LEAVE:
                case DESPAWN:
                    String uuid = com.ghost.api.proto.ProtoConverter.deserializeUuid(decryptedBytes);
                    GHOST_REGISTRY.removeGhost(uuid);
                    break;
                default:
                    LOGGER.warn("Received unexpected binary packet type: {}", type);
                    break;
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to process WebSocket binary message", ex);
            close();
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            String decryptedMessage = CryptoUtil.decrypt(message, PASSWORD);
            var packet = SerializationUtil.deserializePacket(decryptedMessage);
            if (packet == null) {
                LOGGER.warn("Received invalid/malformed packet payload.");
                close();
                return;
            }
            var type = packet.getType();
            var data = packet.getData();
            LOGGER.debug("type: {},data: {}", type, data);
            switch (type) {
                case AUTH_CHALLENGE:
                    if (data != null) {
                        try {
                            AuthData challenge = SerializationUtil.parseAuthData(data);
                            String nonce = challenge.nonce();
                            String hash = ChapAuthenticator.calculateHash(PASSWORD, nonce);
                            AuthData response = new AuthData(null, hash);
                            GhostPacket<AuthData> responsePacket = new GhostPacket<>(MessageType.AUTH_RESPONSE,
                                    response);
                            send(SerializationUtil.serializePacket(responsePacket));
                            LOGGER.debug("Sent AUTH_RESPONSE.");
                        } catch (Exception e) {
                            LOGGER.error("Failed to generate auth response", e);
                            close();
                        }
                    }
                    break;
                case AUTH_SUCCESS:
                    LOGGER.debug("Received AUTH_SUCCESS. Authentication verified.");
                    authFuture.complete(true);
                    break;
                default:
                    LOGGER.warn("Received unhandled text packet type: {}", type);
                    break;
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to process WebSocket message: {}", message, ex);
            if (!authFuture.isDone()) {
                authFuture.completeExceptionally(new SecurityException("Decryption or processing failed. Invalid password?", ex));
            }
            close();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("Disconnected from server. code: {}, Reason: {}, Remote: {}", code, reason, remote);
        GHOST_REGISTRY.clear();
        if (!authFuture.isDone()) {
            authFuture.complete(false);
        }
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("An error occurred in Websocket client: ", ex);
        GHOST_REGISTRY.clear();
        if (!authFuture.isDone()) {
            authFuture.completeExceptionally(ex);
        }
    }
}
