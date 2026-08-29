package com.abhishekraj0.api.context;

import com.abhishekraj0.api.model.ChatMessage;
import java.util.List;
import java.util.Map;

/**
 * Context payload containing messages and environment/variable state parsed for the model call.
 */
public record AgentContext(
        List<ChatMessage> messages,
        Map<String, Object> variables,
        String systemInstruction,
        Map<String, Object> metadata
) {}
