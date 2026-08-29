package com.agentx.api.agent;

/**
 * Interface representing the Agent Runtime which manages and executes agents.
 */
public interface AgentRuntime {

    /**
     * Executes the agent request within the runtime.
     *
     * @param request the agent request
     * @return the execution response
     */
    AgentResponse execute(AgentRequest request);

    /**
     * Cancels an ongoing agent execution.
     *
     * @param executionId the ID of the execution to cancel
     */
    void cancel(String executionId);

    /**
     * Retrieves the execution details for a given execution ID.
     *
     * @param executionId the execution ID
     * @return the agent execution details
     */
    AgentExecution getExecution(String executionId);
}
