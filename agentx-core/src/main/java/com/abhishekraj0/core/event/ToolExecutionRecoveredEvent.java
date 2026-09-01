package com.abhishekraj0.core.event;

/**
 * Event published when an unknown tool execution outcome is successfully resolved or recovered.
 */
public class ToolExecutionRecoveredEvent extends BaseAgentEvent {

    private final String toolName;
    private final String toolCallId;
    private final String resolutionType;

    public ToolExecutionRecoveredEvent(String executionId, String toolName, String toolCallId, String resolutionType) {
        super(executionId);
        this.toolName = toolName;
        this.toolCallId = toolCallId;
        this.resolutionType = resolutionType;
    }

    public String toolName() {
        return toolName;
    }

    public String toolCallId() {
        return toolCallId;
    }

    public String resolutionType() {
        return resolutionType;
    }

    @Override
    public String type() {
        return "ToolExecutionRecovered";
    }
}
