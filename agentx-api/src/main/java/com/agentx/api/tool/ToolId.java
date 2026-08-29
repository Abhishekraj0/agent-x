package com.agentx.api.tool;

/**
 * Unique identifier for an AgentTool, supporting optional namespacing.
 */
public record ToolId(String namespace, String name) {

    public ToolId(String name) {
        this(null, name);
    }

    /**
     * Returns the fully qualified name of the tool (namespace:name or just name).
     *
     * @return the full name
     */
    public String getFullName() {
        return (namespace == null || namespace.isBlank()) ? name : namespace + ":" + name;
    }
}
