package com.agentx.core.event;

/**
 * Event triggered when an agent execution completes.
 */
public class ExecutionCompletedEvent extends BaseAgentEvent {
    
    private final String output;

    public ExecutionCompletedEvent(String executionId, String output) {
        super(executionId);
        this.output = output;
    }

    public String output() {
        return output;
    }

    @Override
    public String type() {
        return "EXECUTION_COMPLETED";
    }
}
