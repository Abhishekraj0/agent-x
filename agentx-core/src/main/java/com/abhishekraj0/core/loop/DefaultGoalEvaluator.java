package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.loop.GoalEvaluator;
import com.abhishekraj0.api.loop.GoalStatus;

/**
 * Default implementation of GoalEvaluator mapping state status to GoalStatus.
 */
public class DefaultGoalEvaluator implements GoalEvaluator {

    @Override
    public GoalStatus evaluate(AgentState state) {
        if ("COMPLETED".equals(state.status())) {
            return GoalStatus.COMPLETE;
        }
        if ("FAILED".equals(state.status())) {
            return GoalStatus.FAILED;
        }
        if ("TIMEOUT".equals(state.status())) {
            return GoalStatus.TIMEOUT;
        }
        if ("CANCELLED".equals(state.status())) {
            return GoalStatus.FAILED; // Cancelled is mapped to failure of achieving goal
        }
        if ("WAITING_FOR_USER".equals(state.status())) {
            return GoalStatus.NEEDS_USER;
        }
        return GoalStatus.IN_PROGRESS;
    }
}
