package com.ghost.net;

import com.ghost.api.dto.AuthData;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import com.ghost.api.registry.IGhostRegistry;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.SerializationUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class GhostWebSocketClient extends WebSocketClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(GhostWebSocketClient.class);
    private final IGhostRegistry GHOST_REGISTRY;
    private final String PASSWORD;

    public GhostWebSocketClient(URI serverURI, IGhostRegistry registry, String password) {
        super(serverURI);
        GHOST_REGISTRY = registry;
        PASSWORD = password;
    }

    @Override
    public void onOpen(ServerHandshake data) {
        LOGGER.info("Successfully connected to the server (status: {})", data.getHttpStatus());
    }

    @Override
    public void onMessage(String message) {
        try {
            var packet = SerializationUtil.deserializePacket(message);
            var type = packet.getType();
            var data = packet.getData();
            LOGGER.debug("type: {},data: {}", type, data);
            switch (type) {
                case AUTH_CHALLENGE:
                    if (data != null) {
                        try {
                            AuthData challenge = SerializationUtil.getGson().fromJson(data, AuthData.class);
                            String nonce = challenge.nonce();
                            String hash = ChapAuthenticator.calculateHash(PASSWORD, nonce);
                            AuthData response = new AuthData(null, hash);
                            GhostPacket<AuthData> responsePacket = new GhostPacket<>(MessageType.AUTH_RESPONSE,
                                    response);
                            send(SerializationUtil.serializePacket(responsePacket));
                            LOGGER.info("Sent AUTH_RESPONSE.");
                        } catch (Exception e) {
                            LOGGER.error("Failed to generate auth response", e);
                            close();
                        }
                    }
                    break;
                case UPDATE:
                    if (data != null) {
                        // dataElementを直接 PlayerData にデシリアライズ
                        PlayerData playerData = SerializationUtil.parsePlayerData(data);
                        GHOST_REGISTRY.updateGhost(playerData);
                    }
                    break;
                case LEAVE:
                    if (data != null) {
                        String uuid = SerializationUtil.parseUUID(data);
                        GHOST_REGISTRY.removeGhost(uuid);
                    }
                    break;
                case INITIAL_SYNC:
                    if (data != null) {
                        var list = SerializationUtil.parsePlayerDataList(data);
                        for (PlayerData playerData : list) {
                            GHOST_REGISTRY.updateGhost(playerData);
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to process WebSocket message: {}", message, ex);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("Disconnected from server. code: {}, Reason: {}, Remote: {}", code, reason, remote);
        GHOST_REGISTRY.clear();
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("An error occurred in Websocket client: ", ex);
        GHOST_REGISTRY.clear();
    }
}
