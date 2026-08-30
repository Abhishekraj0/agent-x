package com.abhishekraj0.api.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a single message in the chat conversation history.
 */
public record ChatMessage(
        ChatMessageRole role,
        String content,
        String toolCallId,
        List<ToolCall> toolCalls,
        Map<String, Object> metadata
) {
    public ChatMessage {
        content = com.abhishekraj0.api.security.SecretRedactor.getInstance().redact(content);
    }
    public static ChatMessage system(String content) {
        return new ChatMessage(ChatMessageRole.SYSTEM, content, null, null, Map.of());
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatMessageRole.USER, content, null, null, Map.of());
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatMessageRole.ASSISTANT, content, null, null, Map.of());
    }

    public static ChatMessage assistant(String content, List<ToolCall> toolCalls) {
        return new ChatMessage(ChatMessageRole.ASSISTANT, content, null, toolCalls, Map.of());
    }

    public static ChatMessage tool(String toolCallId, String content) {
        return new ChatMessage(ChatMessageRole.TOOL, content, toolCallId, null, Map.of());
    }
}
