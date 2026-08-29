package com.agentx.core.event;

/**
 * Event triggered when an agent invokes a tool.
 */
public class ToolCalledEvent extends BaseAgentEvent {
    
    private final String toolName;
    private final String argumentsJson;
    private final String output;

    public ToolCalledEvent(String executionId, String toolName, String argumentsJson, String output) {
        super(executionId);
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
        this.output = output;
    }

    public String toolName() {
        return toolName;
    }

    public String argumentsJson() {
        return argumentsJson;
    }

    public String output() {
        return output;
    }

    @Override
    public String type() {
        return "TOOL_CALLED";
    }
}
