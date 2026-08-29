package com.abhishekraj0.api.tool;

import java.util.List;

/**
 * Schema defining the expected inputs of a tool.
 */
public record ToolSchema(
        String type, // typically "object"
        List<ToolProperty> properties
) {
    public static ToolSchema empty() {
        return new ToolSchema("object", List.of());
    }
}
