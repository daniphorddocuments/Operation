package com.daniphord.mahanga.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * Rate Limiter to prevent brute force attacks
 */
public class RateLimiter {
    private final ConcurrentHashMap<String, AttemptTracker> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowDurationMs;
    private final long lockoutDurationMs;

    public RateLimiter(int maxAttempts, long windowDurationSeconds, long lockoutDurationSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowDurationMs = windowDurationSeconds * 1000;
        this.lockoutDurationMs = lockoutDurationSeconds * 1000;
    }

    /**
     * Check if IP/user is rate limited
     */
    public boolean isRateLimited(String identifier) {
        AttemptTracker tracker = attempts.computeIfAbsent(identifier, k -> new AttemptTracker());

        long now = System.currentTimeMillis();

        // Check if still locked out
        if (tracker.lockedUntil != 0 && now < tracker.lockedUntil) {
            return true;
        }

        // Check if window has expired
        if (now - tracker.firstAttemptTime > windowDurationMs) {
            tracker.reset(now);
        }

        return tracker.attempts.get() >= maxAttempts;
    }

    /**
     * Record an attempt
     */
    public void recordAttempt(String identifier) {
        AttemptTracker tracker = attempts.computeIfAbsent(identifier, k -> new AttemptTracker());
        tracker.attempts.incrementAndGet();

        if (tracker.attempts.get() >= maxAttempts) {
            tracker.lockedUntil = System.currentTimeMillis() + lockoutDurationMs;
        }
    }

    /**
     * Reset attempts for identifier
     */
    public void reset(String identifier) {
        AttemptTracker tracker = attempts.get(identifier);
        if (tracker != null) {
            tracker.reset(System.currentTimeMillis());
        }
    }

    /**
     * Get remaining attempts
     */
    public int getRemainingAttempts(String identifier) {
        AttemptTracker tracker = attempts.get(identifier);
        if (tracker == null) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - tracker.attempts.get());
    }

    /**
     * Get lockout remaining time in seconds
     */
    public long getLockoutRemainingSeconds(String identifier) {
        AttemptTracker tracker = attempts.get(identifier);
        if (tracker == null || tracker.lockedUntil == 0) {
            return 0;
        }
        long remaining = tracker.lockedUntil - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    /**
     * Inner class to track attempts
     */
    private static class AttemptTracker {
        AtomicInteger attempts = new AtomicInteger(0);
        long firstAttemptTime = System.currentTimeMillis();
        long lockedUntil = 0;

        void reset(long currentTime) {
            attempts.set(0);
            firstAttemptTime = currentTime;
            lockedUntil = 0;
        }
    }
}

