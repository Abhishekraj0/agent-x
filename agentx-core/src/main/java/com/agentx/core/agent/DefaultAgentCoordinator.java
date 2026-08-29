package com.agentx.core.agent;

import com.agentx.api.agent.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Coordinates and delegates tasks to target agents inside a registry.
 */
public class DefaultAgentCoordinator implements AgentCoordinator {

    private final AgentRegistry agentRegistry;

    public DefaultAgentCoordinator(AgentRegistry agentRegistry) {
        if (agentRegistry == null) {
            throw new IllegalArgumentException("AgentRegistry must not be null");
        }
        this.agentRegistry = agentRegistry;
    }

    @Override
    public AgentResponse delegate(AgentTask task) {
        Optional<Agent> agentOpt = agentRegistry.get(task.assigneeAgentName());
        if (agentOpt.isEmpty()) {
            AgentState failedState = new AgentState(
                    task.taskId(),
                    List.of(),
                    null,
                    Map.of(),
                    0,
                    0,
                    "FAILED"
            );
            return new AgentResponse(
                    "Error: Assignee agent '" + task.assigneeAgentName() + "' not found.",
                    failedState,
                    List.of()
            );
        }
        Agent agent = agentOpt.get();
        return agent.run(task.description());
    }
}
