package com.ghost.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimpleRateLimiterTest {

    @Test
    void tryAcquire_ShouldAllowInitialRequests() {
        // 10 permits per second
        var limiter = GhostModServer.SimpleRateLimiter.create(10.0);

        // Initially full, should return true immediately
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void tryAcquire_ShouldLimitExceededRequests() {
        // 1 permit per second for easy testing
        var limiter = GhostModServer.SimpleRateLimiter.create(1.0);

        // 1st request: Success (consumes 1.0 token)
        assertTrue(limiter.tryAcquire(), "First request should pass");

        // 2nd request: Fail (0.0 tokens left)
        assertFalse(limiter.tryAcquire(), "Second request should fail immediately");
    }

    @Test
    void tryAcquire_ShouldRefillOverTime() throws InterruptedException {
        // 5 permits per second -> refill 5 tokens every second
        var limiter = GhostModServer.SimpleRateLimiter.create(5.0);

        // Consume all 5 initial tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(), "Request " + (i + 1) + " should pass");
        }

        // Next request should fail (empty bucket)
        assertFalse(limiter.tryAcquire(), "Request should fail when bucket is empty");

        // Wait for refill (300ms should give 0.3 * 5 = 1.5 tokens)
        Thread.sleep(300);

        // Should be able to acquire again
        assertTrue(limiter.tryAcquire(), "Should accept request after refill");
    }
}
