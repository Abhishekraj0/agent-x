package com.abhishekraj0.api.tool;

import java.io.Serializable;

/**
 * Result of checking whether a tool execution request can be resolved from cache or requires recovery.
 */
public record IdempotencyDecision(
        boolean isDuplicate,
        String cachedOutput,
        boolean success,
        String errorMessage,
        IdempotencyStatus status
) implements Serializable {
    private static final long serialVersionUID = 2L;

    public IdempotencyDecision(boolean isDuplicate, String cachedOutput, boolean success, String errorMessage) {
        this(isDuplicate, cachedOutput, success, errorMessage, isDuplicate ? IdempotencyStatus.CACHED_RESULT : IdempotencyStatus.EXECUTE_NEW);
    }

    public static IdempotencyDecision executeNew() {
        return new IdempotencyDecision(false, null, false, null, IdempotencyStatus.EXECUTE_NEW);
    }

    public static IdempotencyDecision useCached(String output, boolean success, String errorMessage) {
        return new IdempotencyDecision(true, output, success, errorMessage, IdempotencyStatus.CACHED_RESULT);
    }

    public static IdempotencyDecision unknownResult(String message) {
        return new IdempotencyDecision(false, null, false, message, IdempotencyStatus.UNKNOWN_RESULT);
    }

    public boolean isUnknownResult() {
        return status == IdempotencyStatus.UNKNOWN_RESULT;
    }
}
