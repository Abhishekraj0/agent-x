package com.abhishekraj0.api.context;

/**
 * Tracks the token budget allocations and consumption.
 */
public record TokenBudget(
        int maxTokens,
        int consumedTokens,
        int remainingTokens
) {
    public boolean isExceeded() {
        return remainingTokens <= 0;
    }
}
