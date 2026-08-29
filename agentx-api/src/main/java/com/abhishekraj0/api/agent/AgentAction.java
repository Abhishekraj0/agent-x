package com.abhishekraj0.api.agent;

import java.util.Map;

/**
 * Represents an action to be performed by the agent.
 */
public record AgentAction(
        String actionId,
        String type, // e.g. TOOL_CALL, MODEL_CALL, USER_INPUT, FINISH
        Map<String, Object> details
) {}
