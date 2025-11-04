package com.ghost.net;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.registry.IGhostRegistry;
import com.ghost.common.util.SerializationUtil;
import com.ghost.registry.InMemoryGhostRegistry;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class ConnectionManager {
    @Nullable
    private GhostWebSocketClient session = null;
    private final IGhostRegistry GHOST_REGISTRY;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    public ConnectionManager() {
        GHOST_REGISTRY = new InMemoryGhostRegistry();
    }

    public void connect(URI serverURI) {
        if (this.isOpen()) {
            return;
        }
        session = new GhostWebSocketClient(serverURI, GHOST_REGISTRY);
        session.connect();
    }

    public boolean connectBlocking(URI servverURI, long timeout, TimeUnit unit) {
        if (this.isOpen()) {
            LOGGER.info("Already connected");
            return true;
        }
        session = new GhostWebSocketClient(servverURI, GHOST_REGISTRY);
        try {
            return session.connectBlocking(timeout, unit);
        } catch (Exception ex) {
            LOGGER.error("Failed to connect:", ex);
        }
        return false;
    }

    public void disconnect() {
        if (session != null && session.isOpen()) {
            session.close();
            session = null;
        }
    }

    public IGhostRegistry getGhostRegistry() {
        return this.GHOST_REGISTRY;
    }

    public boolean isOpen() {
        if (session == null) {
            return false;
        }
        return session.isOpen();
    }

    public void sendPlayerData(PlayerData current_data) {
        if (session != null && session.isOpen()) {
            final String msg = SerializationUtil.serialize(current_data);
            session.send(msg);
        }
    }
}
