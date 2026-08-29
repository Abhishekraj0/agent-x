package com.abhishekraj0.api.tool;

import java.io.Serializable;
import java.time.Instant;

/**
 * Captures the result, metadata, and status of a tool call attempt.
 */
public record ToolExecutionResult(
        String executionId,
        String toolCallId,
        String toolId,
        String idempotencyKey,
        boolean success,
        String output,
        String errorMessage,
        Instant completedAt,
        String status
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
