package com.abhishekraj0.api.agent;

import java.util.Map;
import java.util.UUID;

/**
 * Request details for triggering an agent run.
 */
public record AgentRequest(
        String input,
        String executionId,
        AgentOptions options,
        Map<String, Object> variables
) {
    public AgentRequest(String input, String executionId, AgentOptions options) {
        this(input, executionId, options, Map.of());
    }

    public AgentRequest(String input) {
        this(input, UUID.randomUUID().toString(), AgentOptions.defaultOptions(), Map.of());
    }
}
