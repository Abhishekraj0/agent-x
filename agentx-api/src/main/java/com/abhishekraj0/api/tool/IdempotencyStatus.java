package com.abhishekraj0.api.tool;

/**
 * Status returned when evaluating idempotency for a tool execution request.
 */
public enum IdempotencyStatus {
    EXECUTE_NEW,
    CACHED_RESULT,
    UNKNOWN_RESULT
}
