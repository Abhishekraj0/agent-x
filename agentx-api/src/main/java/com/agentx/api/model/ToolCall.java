package com.agentx.api.model;

/**
 * Represents a request from a chat model to execute a tool.
 */
public record ToolCall(
        String id,
        String name,
        String argumentsJson
) {}
