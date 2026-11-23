package com.ghost.server;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RateLimiter} および {@link RateLimiter.PerIpManager} の決定論的テスト。
 * 仮想時計を注入することで、Thread.sleep に依存せず高速かつ確実に動作を検証します。
 */
class RateLimiterTest {

    @Test
    void tryAcquire_AllowsInitialBurstAndRejectsWhenExhausted() {
        AtomicLong clock = new AtomicLong(0L);
        RateLimiter limiter = new RateLimiter(5.0, clock::get);

        // 初期バースト 5 回は許可されること
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(), (i + 1) + " 回目は許可されるべき");
        }
        // 6 回目は拒否されること
        assertFalse(limiter.tryAcquire(), "トークン枯渇後は拒否されるべき");
    }

    @Test
    void tryAcquire_RefillsTokensOverTimeUpToCap() {
        AtomicLong clock = new AtomicLong(0L);
        RateLimiter limiter = new RateLimiter(4.0, clock::get);

        // 4 回すべて消費
        for (int i = 0; i < 4; i++) {
            assertTrue(limiter.tryAcquire());
        }
        assertFalse(limiter.tryAcquire());

        // 250ms 経過で 1 トークン補充（4 permits/sec）
        clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(250));
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        // 十分時間が経過しても上限（4 トークン）を超えないこと
        clock.addAndGet(TimeUnit.SECONDS.toNanos(10));
        for (int i = 0; i < 4; i++) {
            assertTrue(limiter.tryAcquire());
        }
        assertFalse(limiter.tryAcquire());
    }

    @Test
    void constructor_RejectsNonPositiveRate() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0.0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(-5.0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(Double.NaN));
    }

    @Test
    void perIpManager_IsolatesIpsAndEvictsIdleEntries() throws Exception {
        AtomicLong clock = new AtomicLong(0L);
        long idleExpire = TimeUnit.SECONDS.toNanos(60);
        long cleanupInterval = TimeUnit.SECONDS.toNanos(30);
        RateLimiter.PerIpManager manager = new RateLimiter.PerIpManager(
                2.0, idleExpire, cleanupInterval, clock::get);

        InetAddress ip1 = InetAddress.getByName("127.0.0.1");
        InetAddress ip2 = InetAddress.getByName("127.0.0.2");

        // IP ごとに独立してバケットが管理されること
        assertTrue(manager.tryAcquire(ip1));
        assertTrue(manager.tryAcquire(ip1));
        assertFalse(manager.tryAcquire(ip1));

        assertTrue(manager.tryAcquire(ip2));
        assertEquals(2, manager.size());

        // 40 秒経過後に ip2 だけアクセス → まだ ip1 は期限切れ前（40s < 60s）
        clock.addAndGet(TimeUnit.SECONDS.toNanos(40));
        assertTrue(manager.tryAcquire(ip2));
        assertEquals(2, manager.size());

        // さらに 30 秒経過（合計 70 秒）→ ip1 は 70 秒アイドルで掃除対象、ip2 は 30 秒なので残存
        clock.addAndGet(TimeUnit.SECONDS.toNanos(30));
        int evicted = manager.evictIdleEntries();
        assertEquals(1, evicted);
        assertEquals(1, manager.size());
    }
}
