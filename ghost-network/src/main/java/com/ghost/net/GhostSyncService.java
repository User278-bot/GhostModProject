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

    public void connect(URI serverURI, String password) {
        if (this.isConnected()) {
            return;
        }
        session = new GhostWebSocketClient(serverURI, ghostRegistry, password);
        session.connect();
    }

    public boolean connectBlocking(URI servverURI, String password, long timeout, TimeUnit unit) {
        if (this.isConnected()) {
            LOGGER.info("Already connected");
            return true;
        }
        session = new GhostWebSocketClient(servverURI, ghostRegistry, password);
        try {
            boolean socketConnected = session.connectBlocking(timeout, unit);
            if (!socketConnected) {
                return false;
            }
            // 認証完了を待つ
            // connectBlockingで消費した時間は考慮していないが、簡易実装として別途timeout待つ
            return session.getAuthFuture().get(timeout, unit);
        } catch (Exception ex) {
            LOGGER.error("Failed to connect or authenticate:", ex);
            session.close();
            session = null;
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

    /**
     * 現在接続中のサーバーURIを取得します。
     * 接続していない場合はnullを返します。
     */
    @Nullable
    public URI getConnectedUri() {
        return session != null && session.isOpen() ? session.getURI() : null;
    }

    public <T> void sendPacket(GhostPacket<T> packet) {
        if (session != null && session.isOpen()) {
            final String msg = SerializationUtil.serializePacket(packet);
            session.send(msg);
        }
    }
}
