package com.abhishekraj0.core.event;

/**
 * Event published when a tool execution outcome is unknown due to a prior crash or unconfirmed completion.
 */
public class ToolExecutionUnknownEvent extends BaseAgentEvent {

    private final String toolName;
    private final String toolCallId;
    private final String reason;

    public ToolExecutionUnknownEvent(String executionId, String toolName, String toolCallId, String reason) {
        super(executionId);
        this.toolName = toolName;
        this.toolCallId = toolCallId;
        this.reason = reason;
    }

    public String toolName() {
        return toolName;
    }

    public String toolCallId() {
        return toolCallId;
    }

    public String reason() {
        return reason;
    }

    @Override
    public String type() {
        return "ToolExecutionUnknown";
    }
}
