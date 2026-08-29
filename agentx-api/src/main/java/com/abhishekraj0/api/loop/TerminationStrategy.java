package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;

/**
 * Interface to evaluate whether the agent loop should terminate.
 */
public interface TerminationStrategy {
    TerminationDecision evaluate(AgentState state);
}
