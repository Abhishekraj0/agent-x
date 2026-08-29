package com.agentx.core.loop;

import com.agentx.api.loop.FailureContext;
import com.agentx.api.loop.RetryDecision;
import com.agentx.api.loop.RetryStrategy;
import java.time.Duration;

/**
 * Simple implementation of RetryStrategy with maximum attempts and delay.
 */
public class SimpleRetryStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final Duration delay;

    public SimpleRetryStrategy(int maxAttempts, Duration delay) {
        this.maxAttempts = maxAttempts;
        this.delay = delay != null ? delay : Duration.ZERO;
    }

    @Override
    public RetryDecision onFailure(FailureContext context) {
        if (context == null || context.attempt() >= maxAttempts) {
            return RetryDecision.stop("Max attempts exceeded (" + maxAttempts + ")");
        }
        return RetryDecision.retry(delay, "Retry attempt " + (context.attempt() + 1));
    }
}
