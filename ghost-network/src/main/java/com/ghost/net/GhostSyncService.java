package com.ghost.net;

import com.ghost.api.registry.IGhostRegistry;
import com.ghost.api.packet.GhostPacket;
import com.ghost.util.SerializationUtil;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class GhostSyncService {
    @Nullable
    private GhostWebSocketClient session = null;
    private final IGhostRegistry ghostRegistry;
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostSyncService.class);

    public GhostSyncService(IGhostRegistry ghostRegistry) {
        this.ghostRegistry = ghostRegistry;
    }

    public void connect(URI serverURI) {
        if (this.isConnected()) {
            return;
        }
        session = new GhostWebSocketClient(serverURI, ghostRegistry);
        session.connect();
    }

    public boolean connectBlocking(URI servverURI, long timeout, TimeUnit unit) {
        if (this.isConnected()) {
            LOGGER.info("Already connected");
            return true;
        }
        session = new GhostWebSocketClient(servverURI, ghostRegistry);
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

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    public <T> void sendPacket(GhostPacket<T> packet) {
        if (session != null && session.isOpen()) {
            final String msg = SerializationUtil.serializePacket(packet);
            session.send(msg);
        }
    }
}
