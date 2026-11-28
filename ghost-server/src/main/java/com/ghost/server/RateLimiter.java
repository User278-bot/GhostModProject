package com.ghost.server;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * トークンバケット方式のレートリミッター。
 * WebSocket 1 接続（セッション）単位でパケット送信レートを制限します。
 */
public class RateLimiter {

    private final double permitsPerSecond;
    private final LongSupplier nanoTimeSource;
    private double tokens;
    private long lastRefillTimestamp;

    public RateLimiter(double permitsPerSecond) {
        this(permitsPerSecond, System::nanoTime);
    }

    RateLimiter(double permitsPerSecond, LongSupplier nanoTimeSource) {
        if (permitsPerSecond <= 0.0 || Double.isNaN(permitsPerSecond) || Double.isInfinite(permitsPerSecond)) {
            throw new IllegalArgumentException("permitsPerSecond must be positive: " + permitsPerSecond);
        }
        this.permitsPerSecond = permitsPerSecond;
        this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource must not be null");
        this.tokens = permitsPerSecond;
        this.lastRefillTimestamp = nanoTimeSource.getAsLong();
    }

    /**
     * 1 トークン消費を試みます。許可された場合は true、レート制限超過時は false を返します。
     */
    public synchronized boolean tryAcquire() {
        long now = nanoTimeSource.getAsLong();
        refill(now);
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill(long now) {
        long elapsedNanos = now - lastRefillTimestamp;
        if (elapsedNanos <= 0) {
            return;
        }
        double secondsPassed = elapsedNanos / 1_000_000_000.0;
        double newTokens = secondsPassed * permitsPerSecond;
        if (newTokens > 0) {
            tokens = Math.min(permitsPerSecond, tokens + newTokens);
            lastRefillTimestamp = now;
        }
    }
}
