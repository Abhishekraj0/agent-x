package com.agentx.core.event;

/**
 * Event triggered when an agent execution starts.
 */
public class ExecutionStartedEvent extends BaseAgentEvent {
    
    private final String input;

    public ExecutionStartedEvent(String executionId, String input) {
        super(executionId);
        this.input = input;
    }

    public String input() {
        return input;
    }

    @Override
    public String type() {
        return "EXECUTION_STARTED";
    }
}
