package com.ghost.net;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.proto.ProtoConverter;
import com.ghost.api.registry.IGhostRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Ghost サーバーとの接続ライフサイクルおよびパケット送信をスレッド安全に管理するサービス。
 */
public class GhostSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostSyncService.class);

    private final Object connectionLock = new Object();
    @Nullable
    private volatile GhostWebSocketClient session = null;
    private final IGhostRegistry ghostRegistry;
    private final List<Runnable> disconnectListeners = new CopyOnWriteArrayList<>();

    public GhostSyncService(IGhostRegistry ghostRegistry) {
        this.ghostRegistry = Objects.requireNonNull(ghostRegistry, "ghostRegistry must not be null");
    }

    /**
     * サーバー切断時に呼び出されるリスナーを登録します。
     */
    public void addDisconnectListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        disconnectListeners.add(listener);
    }

    /**
     * 非同期でサーバーへ接続・認証を行います。
     */
    public CompletableFuture<Boolean> connectAsync(URI serverURI, String password, long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> this.connectBlocking(serverURI, password, timeout, unit));
    }

    /**
     * サーバーへ接続し、CHAP 認証が完了するまで待機します。
     *
     * @return 接続および認証が成功した場合は true、失敗・タイムアウト時は false
     */
    public boolean connectBlocking(URI serverURI, String password, long timeout, TimeUnit unit) {
        synchronized (connectionLock) {
            closeCurrentSessionLocked();

            GhostWebSocketClient newSession = new GhostWebSocketClient(
                    serverURI, ghostRegistry, password, this::notifyDisconnectListeners);
            this.session = newSession;

            boolean authenticated = false;
            try {
                boolean connected = newSession.connectBlocking(timeout, unit);
                if (!connected) {
                    LOGGER.warn("Timed out or failed to establish WebSocket connection to {}", serverURI);
                    return false;
                }
                authenticated = Boolean.TRUE.equals(newSession.getAuthFuture().get(timeout, unit));
                if (!authenticated) {
                    LOGGER.warn("Authentication rejected by server: {}", serverURI);
                }
                return authenticated;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Connection attempt interrupted: {}", serverURI, ex);
                return false;
            } catch (Exception ex) {
                LOGGER.error("Failed to connect or authenticate to {}", serverURI, ex);
                return false;
            } finally {
                if (!authenticated) {
                    newSession.close();
                    if (this.session == newSession) {
                        this.session = null;
                    }
                    ghostRegistry.clear();
                }
            }
        }
    }

    /**
     * 現在の接続（接続試行中を含む）を切断し、ゴーストレジストリをクリアします。
     */
    public void disconnect() {
        synchronized (connectionLock) {
            closeCurrentSessionLocked();
        }
    }

    private void closeCurrentSessionLocked() {
        GhostWebSocketClient current = this.session;
        this.session = null;
        if (current != null) {
            try {
                if (current.isOpen()) {
                    current.closeBlocking();
                } else {
                    current.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                current.close();
            } catch (Exception e) {
                LOGGER.debug("Error while closing previous session", e);
            }
        }
        ghostRegistry.clear();
    }

    public boolean isConnected() {
        GhostWebSocketClient current = this.session;
        return current != null && current.isOpen();
    }

    /**
     * 現在接続中のサーバー URI を取得します。未接続の場合は null を返します。
     */
    @Nullable
    public URI getConnectedUri() {
        GhostWebSocketClient current = this.session;
        return current != null && current.isOpen() ? current.getURI() : null;
    }

    /**
     * 自プレイヤーの状態（UPDATE パケット）をサーバーへ送信します。
     */
    public void sendPlayerUpdate(PlayerData playerData) {
        GhostWebSocketClient current = this.session;
        if (current != null && current.isOpen()) {
            current.send(ProtoConverter.encodeUpdate(playerData));
        }
    }

    private void notifyDisconnectListeners() {
        for (Runnable listener : disconnectListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                LOGGER.warn("Disconnect listener threw an exception", e);
            }
        }
    }
}
