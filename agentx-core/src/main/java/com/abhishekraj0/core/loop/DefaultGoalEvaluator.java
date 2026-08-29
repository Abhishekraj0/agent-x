package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.loop.GoalEvaluator;
import com.abhishekraj0.api.loop.GoalStatus;
import java.util.function.Predicate;

/**
 * Default implementation of GoalEvaluator mapping state status to GoalStatus,
 * with optional support for semantic state validation.
 */
public class DefaultGoalEvaluator implements GoalEvaluator {

    private final Predicate<AgentState> validator;

    public DefaultGoalEvaluator() {
        this(state -> true);
    }

    public DefaultGoalEvaluator(Predicate<AgentState> validator) {
        this.validator = validator != null ? validator : (state -> true);
    }

    @Override
    public GoalStatus evaluate(AgentState state) {
        if ("COMPLETED".equals(state.status())) {
            if (validator.test(state)) {
                return GoalStatus.COMPLETE;
            } else {
                return GoalStatus.IN_PROGRESS;
            }
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
