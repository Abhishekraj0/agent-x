package com.agentx.api.tool;

/**
 * Interface representing an executable tool that can be used by an AI Agent.
 */
public interface AgentTool {

    /**
     * Unique identifier for this tool.
     *
     * @return the tool ID
     */
    ToolId id();

    /**
     * A description of what the tool does, used by models to understand when to invoke it.
     *
     * @return the description
     */
    String description();

    /**
     * The input parameter schema expected by this tool.
     *
     * @return the input schema
     */
    ToolSchema inputSchema();

    /**
     * Executes the tool with the given context.
     *
     * @param context the tool execution context
     * @return the tool result
     */
    ToolResult execute(ToolContext context);

    /**
     * Configuration and execution policies of the tool.
     *
     * @return the tool metadata
     */
    default ToolMetadata metadata() {
        return ToolMetadata.defaultMetadata();
    }
}
