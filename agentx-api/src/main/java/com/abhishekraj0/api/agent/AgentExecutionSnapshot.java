package com.abhishekraj0.api.agent;

import com.abhishekraj0.api.planner.Plan;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Stable domain snapshot representing a paused/suspended agent execution.
 */
public record AgentExecutionSnapshot(
        String executionId,
        String agentId,
        String goal,
        AgentState state,
        String loopState,
        Plan plan,
        int iteration,
        int toolCallCount,
        List<String> observations,
        List<String> memoryReferences,
        AgentDecision pendingDecision,
        String approvalState,
        Map<String, Object> budgets,
        Instant timestamp,
        Map<String, Object> metadata
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
