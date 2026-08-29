package com.agentx.api.tool;

/**
 * Details of a failure during tool execution.
 */
public record ToolError(
        String code,
        String message,
        Throwable cause
) {}
