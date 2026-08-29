package com.abhishekraj0.api.model;

/**
 * The response returned by a ChatModel invocation.
 */
public record ChatResponse(
        ChatMessage message,
        TokenUsage usage,
        String finishReason // e.g. STOP, TOOL_CALLS, LENGTH, CONTENT_FILTER
) {}
