package com.abhishekraj0.api.model;

import com.abhishekraj0.api.loop.RetryDecision;

/**
 * Interface to determine retrying model invocations.
 */
public interface ModelRetryStrategy {

    RetryDecision shouldRetry(Throwable error, int attempt);
}
