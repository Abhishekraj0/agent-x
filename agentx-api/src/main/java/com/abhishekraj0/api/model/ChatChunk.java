package com.abhishekraj0.api.model;

/**
 * A streamed chunk of a chat model response.
 */
public record ChatChunk(
        String content,
        ToolCall toolCall,
        boolean isLast
) {}
