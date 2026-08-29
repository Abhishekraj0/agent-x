package com.agentx.api.agent;

/**
 * Interface for coordinating and delegating tasks to agents.
 */
public interface AgentCoordinator {

    /**
     * Delegates a task to an agent and waits for completion.
     *
     * @param task the agent task
     * @return the agent response
     */
    AgentResponse delegate(AgentTask task);
}
