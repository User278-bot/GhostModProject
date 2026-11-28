package com.ghost.net;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.proto.AuthChallenge;
import com.ghost.api.proto.GhostPacketProto;
import com.ghost.api.proto.ProtoConverter;
import com.ghost.api.registry.IGhostRegistry;
import com.ghost.net.ConnectionResult.FailureReason;
import com.ghost.net.auth.ChapAuthenticator;
import com.ghost.util.CryptoUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ghost サーバーと暗号化バイナリ Protobuf で通信する WebSocket クライアント（1 接続 1 インスタンス）。
 */
public class GhostWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GhostWebSocketClient.class);

    public static final int CLOSE_AUTH_FAILED = 4001;
    public static final int CLOSE_DECRYPTION_FAILED = 4003;
    public static final int CLOSE_PROTOCOL_MISMATCH = 4004;

    private final IGhostRegistry ghostRegistry;
    private final String password;
    private final SecretKey secretKey;
    private final CompletableFuture<Boolean> authFuture = new CompletableFuture<>();
    @Nullable
    private final Runnable onDisconnectCallback;
    @Nullable
    private final Runnable onUnexpectedDisconnectCallback;
    private final AtomicBoolean handshakeOpened = new AtomicBoolean(false);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final AtomicBoolean localDisconnect = new AtomicBoolean(false);
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);
    private final AtomicBoolean unexpectedDisconnectNotified = new AtomicBoolean(false);
    private final AtomicReference<FailureReason> failureReason = new AtomicReference<>(FailureReason.NONE);

    public GhostWebSocketClient(URI serverURI,
                                IGhostRegistry registry,
                                String password,
                                @Nullable Runnable onDisconnectCallback,
                                @Nullable Runnable onUnexpectedDisconnectCallback) {
        super(serverURI);
        this.ghostRegistry = registry;
        this.password = password;
        this.secretKey = CryptoUtil.deriveKey(password);
        this.onDisconnectCallback = onDisconnectCallback;
        this.onUnexpectedDisconnectCallback = onUnexpectedDisconnectCallback;
    }

    public CompletableFuture<Boolean> getAuthFuture() {
        return authFuture;
    }

    /**
     * 接続・認証失敗時に記録された失敗理由を取得します。
     */
    public FailureReason getFailureReason() {
        return failureReason.get();
    }

    /**
     * クライアント側の明示的な操作として切断を開始するマークを付与します。
     * これにより「予期せぬ切断」通知が発火しないようにします。
     */
    public void markLocalDisconnect() {
        localDisconnect.set(true);
    }

    @Override
    public void send(byte[] bytes) {
        try {
            super.send(CryptoUtil.encrypt(bytes, secretKey));
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt binary message before sending", e);
            recordFailure(FailureReason.CONNECTION_ERROR);
            close();
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public void onOpen(ServerHandshake data) {
        handshakeOpened.set(true);
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
                    authenticated.set(true);
                    authFuture.complete(true);
                }
                default -> LOGGER.warn("Received unexpected binary packet payload: {}", packet.getPayloadCase());
            }
        } catch (GeneralSecurityException ex) {
            LOGGER.warn("Failed to decrypt WebSocket frame (password mismatch?)", ex);
            if (!authenticated.get()) {
                recordFailure(FailureReason.AUTH_FAILED);
            } else {
                recordFailure(FailureReason.CONNECTION_ERROR);
            }
            if (!authFuture.isDone()) {
                authFuture.completeExceptionally(
                        new SecurityException("Decryption failed due to invalid password.", ex));
            }
            close();
        } catch (Exception ex) {
            LOGGER.error("Failed to process WebSocket binary message", ex);
            recordFailure(FailureReason.CONNECTION_ERROR);
            if (!authFuture.isDone()) {
                authFuture.completeExceptionally(ex);
            }
            close();
        }
    }

    private void handleAuthChallenge(AuthChallenge challenge) {
        int serverVersion = challenge.getProtocolVersion();
        if (serverVersion != ProtoConverter.PROTOCOL_VERSION) {
            String msg = String.format(
                    "Protocol version mismatch: server=%d, client=%d",
                    serverVersion, ProtoConverter.PROTOCOL_VERSION);
            LOGGER.error(msg);
            recordFailure(FailureReason.PROTOCOL_MISMATCH);
            authFuture.completeExceptionally(new IllegalStateException(msg));
            close();
            return;
        }

        final String hash;
        try {
            hash = ChapAuthenticator.calculateHash(password, challenge.getNonce());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to calculate CHAP hash", ex);
        }
        send(ProtoConverter.encodeAuthResponse(hash));
        LOGGER.debug("Sent AUTH_RESPONSE (protocol_version={}).", ProtoConverter.PROTOCOL_VERSION);
    }

    @Override
    public void onMessage(String message) {
        LOGGER.warn("Received unsupported text WebSocket frame (binary protocol required). Closing connection.");
        recordFailure(FailureReason.CONNECTION_ERROR);
        if (!authFuture.isDone()) {
            authFuture.completeExceptionally(new IllegalStateException("Unsupported text frame from server"));
        }
        close();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("Disconnected from server. code: {}, Reason: {}, Remote: {}", code, reason, remote);
        ghostRegistry.clear();
        if (!authenticated.get()) {
            recordFailure(classifyCloseCode(code, handshakeOpened.get()));
        }
        if (!authFuture.isDone()) {
            authFuture.complete(false);
        }
        notifyDisconnectOnce();
        if (authenticated.get() && !localDisconnect.get()) {
            notifyUnexpectedDisconnectOnce();
        }
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("An error occurred in WebSocket client: ", ex);
        ghostRegistry.clear();
        recordFailure(classifyException(ex, handshakeOpened.get()));
        if (!authFuture.isDone()) {
            authFuture.completeExceptionally(ex);
        }
        notifyDisconnectOnce();
    }

    private void recordFailure(FailureReason reason) {
        if (reason != FailureReason.NONE) {
            failureReason.compareAndSet(FailureReason.NONE, reason);
        }
    }

    static FailureReason classifyCloseCode(int code, boolean handshakeWasOpened) {
        return switch (code) {
            case CLOSE_AUTH_FAILED, CLOSE_DECRYPTION_FAILED -> FailureReason.AUTH_FAILED;
            case CLOSE_PROTOCOL_MISMATCH -> FailureReason.PROTOCOL_MISMATCH;
            default -> handshakeWasOpened ? FailureReason.CONNECTION_ERROR : FailureReason.UNREACHABLE;
        };
    }

    static FailureReason classifyException(Throwable throwable, boolean handshakeWasOpened) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof SSLException) {
                return FailureReason.TLS_ERROR;
            }
            if (cursor instanceof ConnectException
                    || cursor instanceof UnknownHostException
                    || cursor instanceof NoRouteToHostException
                    || cursor instanceof SocketTimeoutException
                    || cursor instanceof UnresolvedAddressException) {
                return FailureReason.UNREACHABLE;
            }
            if (cursor instanceof GeneralSecurityException || cursor instanceof SecurityException) {
                return FailureReason.AUTH_FAILED;
            }
            cursor = cursor.getCause();
        }
        return handshakeWasOpened ? FailureReason.CONNECTION_ERROR : FailureReason.UNREACHABLE;
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

    private void notifyUnexpectedDisconnectOnce() {
        if (onUnexpectedDisconnectCallback != null && unexpectedDisconnectNotified.compareAndSet(false, true)) {
            try {
                onUnexpectedDisconnectCallback.run();
            } catch (Exception e) {
                LOGGER.warn("Unexpected disconnect callback threw an exception", e);
            }
        }
    }
}
