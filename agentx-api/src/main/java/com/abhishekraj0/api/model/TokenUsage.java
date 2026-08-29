package com.abhishekraj0.api.model;

/**
 * Represents the token usage stats for a model call.
 */
public record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {
    public static TokenUsage zero() {
        return new TokenUsage(0, 0, 0);
    }
}
