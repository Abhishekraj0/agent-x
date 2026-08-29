package com.abhishekraj0.api.loop;

import java.time.Duration;

/**
 * Result representing whether to retry or fail the current step.
 */
public record RetryDecision(
        boolean shouldRetry,
        Duration delay,
        String reason
) {
    public static RetryDecision stop(String reason) {
        return new RetryDecision(false, Duration.ZERO, reason);
    }

    public static RetryDecision retry(Duration delay, String reason) {
        return new RetryDecision(true, delay, reason);
    }
}
