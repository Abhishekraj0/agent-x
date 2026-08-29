package com.abhishekraj0.api.tool;

import java.io.Serializable;

/**
 * Result of checking whether a tool execution request can be resolved from cache.
 */
public record IdempotencyDecision(
        boolean isDuplicate,
        String cachedOutput,
        boolean success,
        String errorMessage
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static IdempotencyDecision executeNew() {
        return new IdempotencyDecision(false, null, false, null);
    }

    public static IdempotencyDecision useCached(String output, boolean success, String errorMessage) {
        return new IdempotencyDecision(true, output, success, errorMessage);
    }
}
