package com.ghost.server;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * トークンバケット方式のレートリミッター。
 * 単一クライアント向けの {@link RateLimiter} 本体と、IP アドレス単位で管理・期限切れ掃除を行う
 * {@link PerIpManager} を提供します。
 */
public class RateLimiter {

    private final double permitsPerSecond;
    private final LongSupplier nanoTimeSource;
    private double tokens;
    private long lastRefillTimestamp;
    private volatile long lastAccessTimestamp;

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
        long now = nanoTimeSource.getAsLong();
        this.lastRefillTimestamp = now;
        this.lastAccessTimestamp = now;
    }

    /**
     * 1 トークン消費を試みます。許可された場合は true、レート制限超過時は false を返します。
     */
    public synchronized boolean tryAcquire() {
        long now = nanoTimeSource.getAsLong();
        lastAccessTimestamp = now;
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

    long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    /**
     * IP アドレスごとの {@link RateLimiter} を管理し、一定時間アクセスのないエントリを掃除して
     * 長期稼働時のメモリリークを防ぎます。
     */
    public static class PerIpManager {
        private static final long DEFAULT_IDLE_EXPIRE_NANOS = TimeUnit.MINUTES.toNanos(10);
        private static final long DEFAULT_CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

        private final Map<InetAddress, RateLimiter> limiters = new ConcurrentHashMap<>();
        private final double permitsPerSecond;
        private final long idleExpireNanos;
        private final long cleanupIntervalNanos;
        private final LongSupplier nanoTimeSource;
        private final AtomicLong lastCleanupTimestamp;

        public PerIpManager(double permitsPerSecond) {
            this(permitsPerSecond, DEFAULT_IDLE_EXPIRE_NANOS, DEFAULT_CLEANUP_INTERVAL_NANOS, System::nanoTime);
        }

        PerIpManager(double permitsPerSecond,
                     long idleExpireNanos,
                     long cleanupIntervalNanos,
                     LongSupplier nanoTimeSource) {
            if (permitsPerSecond <= 0.0) {
                throw new IllegalArgumentException("permitsPerSecond must be positive: " + permitsPerSecond);
            }
            this.permitsPerSecond = permitsPerSecond;
            this.idleExpireNanos = idleExpireNanos;
            this.cleanupIntervalNanos = cleanupIntervalNanos;
            this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource must not be null");
            this.lastCleanupTimestamp = new AtomicLong(nanoTimeSource.getAsLong());
        }

        /**
         * 指定 IP アドレスからのリクエスト許可を判定します。
         */
        public boolean tryAcquire(InetAddress address) {
            Objects.requireNonNull(address, "address must not be null");
            maybeCleanup();
            RateLimiter limiter = limiters.computeIfAbsent(
                    address, k -> new RateLimiter(permitsPerSecond, nanoTimeSource));
            return limiter.tryAcquire();
        }

        /**
         * アイドル期限を超過した IP エントリを削除します。
         */
        public int evictIdleEntries() {
            long now = nanoTimeSource.getAsLong();
            int removed = 0;
            var iterator = limiters.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (now - entry.getValue().getLastAccessTimestamp() >= idleExpireNanos) {
                    iterator.remove();
                    removed++;
                }
            }
            return removed;
        }

        int size() {
            return limiters.size();
        }

        private void maybeCleanup() {
            long now = nanoTimeSource.getAsLong();
            long last = lastCleanupTimestamp.get();
            if (now - last >= cleanupIntervalNanos && lastCleanupTimestamp.compareAndSet(last, now)) {
                evictIdleEntries();
            }
        }
    }
}
