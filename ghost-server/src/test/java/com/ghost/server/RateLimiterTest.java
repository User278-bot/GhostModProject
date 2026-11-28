package com.ghost.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RateLimiter} の決定論的テスト。
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
}
