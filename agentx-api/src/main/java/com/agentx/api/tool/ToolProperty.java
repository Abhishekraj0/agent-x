package com.agentx.api.tool;

/**
 * Represents a property field in a ToolSchema.
 */
public record ToolProperty(
        String name,
        String type, // e.g. string, number, integer, boolean, array, object
        String description,
        boolean required
) {}
