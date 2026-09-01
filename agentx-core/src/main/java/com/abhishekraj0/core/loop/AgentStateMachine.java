package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.loop.LoopState;
import com.abhishekraj0.api.loop.LoopTransition;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces valid state transitions and manages the agent execution status.
 */
public class AgentStateMachine {

    private final Map<LoopState, List<LoopTransition>> transitions = new ConcurrentHashMap<>();

    public void registerTransition(LoopTransition transition) {
        transitions.computeIfAbsent(transition.from(), k -> new ArrayList<>()).add(transition);
    }

    public void registerDefaultTransitions() {
        // Register standard transitions
        for (LoopState fromState : LoopState.values()) {
            for (LoopState toState : LoopState.values()) {
                if (isValidDefault(fromState, toState)) {
                    registerTransition(new DefaultLoopTransition(fromState, toState));
                }
            }
        }
    }

    private boolean isValidDefault(LoopState from, LoopState to) {
        if (from == to) return true;
        // Universal transitions to terminal states
        if (to == LoopState.CANCELLED || to == LoopState.TIMEOUT || to == LoopState.FAILED) {
            return true;
        }
        // Finished states are terminal (unless transitioned to a terminal error/cancellation state checked above)
        if (from == LoopState.COMPLETED || from == LoopState.FAILED || from == LoopState.CANCELLED || from == LoopState.TIMEOUT) {
            return false;
        }

        switch (from) {
            case CREATED:
                return to == LoopState.INITIALIZING;
            case INITIALIZING:
                return to == LoopState.UNDERSTANDING || to == LoopState.COMPLETED;
            case UNDERSTANDING:
                return to == LoopState.BUILDING_CONTEXT || to == LoopState.COMPLETED;
            case BUILDING_CONTEXT:
                return to == LoopState.RETRIEVING_MEMORY || to == LoopState.COMPLETED;
            case RETRIEVING_MEMORY:
                return to == LoopState.RESOLVING_TOOLS || to == LoopState.COMPLETED;
            case RESOLVING_TOOLS:
                return to == LoopState.PLANNING || to == LoopState.COMPLETED;
            case PLANNING:
                return to == LoopState.DECIDING || to == LoopState.COMPLETED;
            case DECIDING:
                return to == LoopState.VALIDATING || to == LoopState.REPLANNING || to == LoopState.WAITING_FOR_USER || to == LoopState.WAITING_FOR_APPROVAL || to == LoopState.COMPLETED;
            case VALIDATING:
                return to == LoopState.AUTHORIZING;
            case AUTHORIZING:
                return to == LoopState.EXECUTING || to == LoopState.WAITING_FOR_APPROVAL;
            case WAITING_FOR_APPROVAL:
                return to == LoopState.EXECUTING || to == LoopState.FAILED || to == LoopState.CANCELLED;
            case EXECUTING:
                return to == LoopState.OBSERVING;
            case OBSERVING:
                return to == LoopState.UPDATING_STATE;
            case UPDATING_STATE:
                return to == LoopState.EVALUATING_GOAL;
            case EVALUATING_GOAL:
                return to == LoopState.REPLANNING || to == LoopState.COMPLETED || to == LoopState.FAILED || to == LoopState.WAITING_FOR_USER || to == LoopState.BUILDING_CONTEXT;
            case REPLANNING:
                return to == LoopState.PLANNING || to == LoopState.DECIDING || to == LoopState.EVALUATING_GOAL;
            case WAITING_FOR_USER:
                return to == LoopState.UNDERSTANDING || to == LoopState.COMPLETED || to == LoopState.FAILED;
            default:
                return false;
        }
    }

    public AgentState transition(AgentState state, LoopState targetState) {
        LoopState current;
        try {
            current = LoopState.valueOf(state.status());
        } catch (IllegalArgumentException e) {
            current = LoopState.CREATED;
        }

        if (current == targetState) {
            return state;
        }

        List<LoopTransition> allowed = transitions.get(current);
        if (allowed != null) {
            for (LoopTransition transition : allowed) {
                if (transition.to() == targetState && transition.canTransition(state)) {
                    return transition.apply(state);
                }
            }
        }

        throw new IllegalStateException("Invalid state transition from " + current + " to " + targetState);
    }

    private static class DefaultLoopTransition implements LoopTransition {
        private final LoopState from;
        private final LoopState to;

        public DefaultLoopTransition(LoopState from, LoopState to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public LoopState from() {
            return from;
        }

        @Override
        public LoopState to() {
            return to;
        }

        @Override
        public boolean canTransition(AgentState state) {
            return true;
        }

        @Override
        public AgentState apply(AgentState state) {
            return new AgentState(
                    state.executionId(),
                    state.history(),
                    state.plan(),
                    state.variables(),
                    state.iterations(),
                    state.toolCalls(),
                    to.name()
            );
        }
    }
}
