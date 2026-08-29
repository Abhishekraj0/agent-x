package com.abhishekraj0.api.model;

import com.abhishekraj0.api.tool.AgentTool;
import java.util.List;
import java.util.Map;

/**
 * Structured request for a ChatModel invocation.
 */
public record ChatRequest(
        List<ChatMessage> messages,
        List<AgentTool> tools,
        Double temperature,
        Map<String, Object> additionalParameters
) {
    public static ChatRequest of(List<ChatMessage> messages) {
        return new ChatRequest(messages, List.of(), null, Map.of());
    }
}
