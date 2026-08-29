package com.abhishekraj0.api.agent;

/**
 * Interface representing an Agent that can process requests synchronously.
 */
public interface Agent {

    /**
     * Executes the agent with the specified structured request.
     *
     * @param request the agent request
     * @return the agent response
     */
    AgentResponse run(AgentRequest request);

    /**
     * Executes the agent with a simple text input using default options.
     *
     * @param input the text input
     * @return the agent response
     */
    AgentResponse run(String input);

    /**
     * Resets the agent's internal state.
     */
    void reset();

    /**
     * Returns the current state of the agent.
     *
     * @return the agent state
     */
    AgentState state();
}
