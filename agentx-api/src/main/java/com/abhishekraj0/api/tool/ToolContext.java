package com.abhishekraj0.api.tool;

import java.util.Map;

/**
 * Context provided to an AgentTool during execution, containing input arguments and runtime state.
 */
public record ToolContext(
        String executionId,
        Map<String, Object> arguments,
        Map<String, Object> contextVariables
) {
    public ToolContext(String executionId, Map<String, Object> arguments) {
        this(executionId, arguments, Map.of());
    }
}
