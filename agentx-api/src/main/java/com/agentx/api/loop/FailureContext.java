package com.agentx.api.loop;

/**
 * Context payload describing a failure to determine retry outcomes.
 */
public record FailureContext(
        String executionId,
        Throwable error,
        int attempt,
        String stage // e.g. MODEL_CALL, TOOL_EXECUTION, PLANNING, RETRY
) {}
