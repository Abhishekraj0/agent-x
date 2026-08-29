package com.abhishekraj0.api.tool;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the detailed execution context of a tool call.
 */
public record ToolExecutionRequest(
        String executionId,
        String toolCallId,
        String toolId,
        int attempt,
        String idempotencyKey,
        Instant startedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
