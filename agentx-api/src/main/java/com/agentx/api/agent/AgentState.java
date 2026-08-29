package com.agentx.api.agent;

import com.agentx.api.model.ChatMessage;
import com.agentx.api.planner.Plan;
import java.util.List;
import java.util.Map;

/**
 * Immutable state of the agent execution.
 */
public record AgentState(
        String executionId,
        List<ChatMessage> history,
        Plan plan,
        Map<String, Object> variables,
        int iterations,
        int toolCalls,
        String status // e.g. INITIALIZED, RUNNING, WAITING_APPROVAL, COMPLETED, FAILED
) {
    /**
     * Creates an initial empty state.
     *
     * @param executionId the execution ID
     * @return the initial state
     */
    public static AgentState initial(String executionId) {
        return new AgentState(executionId, List.of(), null, Map.of(), 0, 0, "INITIALIZED");
    }
}
