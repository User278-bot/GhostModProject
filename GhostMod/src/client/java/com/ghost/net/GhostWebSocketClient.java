package com.ghost.net;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.registry.IGhostRegistry;
import com.ghost.common.util.SerializationUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class GhostWebSocketClient extends WebSocketClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(GhostWebSocketClient.class);
    private final IGhostRegistry GHOST_REGISTRY;

    public GhostWebSocketClient(URI serverURI, IGhostRegistry registry) {
        super(serverURI);
        GHOST_REGISTRY = registry;
    }

    @Override
    public void onOpen(ServerHandshake data) {
        LOGGER.info("Successfully connected to the server (status: {})", data.getHttpStatus());
    }

    @Override
    public void onMessage(String message) {
        LOGGER.info("Received message: {}", message);
        final PlayerData data = SerializationUtil.deserialize(message);
        GHOST_REGISTRY.updateGhost(data);
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
