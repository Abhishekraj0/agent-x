package com.agentx.api.loop;

/**
 * Strategy interface to decide retries when executing agent actions.
 */
public interface RetryStrategy {

    /**
     * Determines whether to retry a failed operation.
     *
     * @param context the failure details
     * @return the retry decision
     */
    RetryDecision onFailure(FailureContext context);
}
