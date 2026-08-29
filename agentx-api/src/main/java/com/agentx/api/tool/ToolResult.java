package com.agentx.api.tool;

/**
 * Result returned by an AgentTool execution.
 */
public record ToolResult(
        String output,
        ToolError error,
        boolean success
) {
    public static ToolResult success(String output) {
        return new ToolResult(output, null, true);
    }

    public static ToolResult failure(String code, String message, Throwable cause) {
        return new ToolResult(null, new ToolError(code, message, cause), false);
    }

    public static ToolResult failure(String message) {
        return failure("ERROR", message, null);
    }
}
