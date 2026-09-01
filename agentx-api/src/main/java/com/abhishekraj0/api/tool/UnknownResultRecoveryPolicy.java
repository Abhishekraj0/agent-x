package com.abhishekraj0.api.tool;

/**
 * Strategy for handling non-idempotent tool execution when an execution outcome is unknown (e.g. process crash before persistence).
 */
public enum UnknownResultRecoveryPolicy {
    /**
     * Fail safely with an AgentFailure to prevent duplicate side effects.
     */
    FAIL_SAFE,

    /**
     * Suspend execution and enter WAITING_APPROVAL status so human verification can decide whether to resume or re-run.
     */
    REQUIRE_APPROVAL,

    /**
     * Re-execute tool assuming the previous execution did not take effect.
     */
    RETRY
}
