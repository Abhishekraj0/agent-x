package com.abhishekraj0.core.event;

/**
 * Event published when the agent loop transitions from one state to another.
 */
public class StateTransitionEvent extends BaseAgentEvent {
    
    private final String fromState;
    private final String toState;

    public StateTransitionEvent(String executionId, String fromState, String toState) {
        super(executionId);
        this.fromState = fromState;
        this.toState = toState;
    }

    @Override
    public String type() {
        return "STATE_TRANSITION";
    }

    public String fromState() {
        return fromState;
    }

    public String toState() {
        return toState;
    }
}
