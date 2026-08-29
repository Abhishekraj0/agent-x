package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;

/**
 * Interface to evaluate whether the agent's goal has been reached or if it's blocked/failed.
 */
public interface GoalEvaluator {
    GoalStatus evaluate(AgentState state);
}
