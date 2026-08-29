package com.agentx.api.loop;

import com.agentx.api.agent.AgentResponse;
import com.agentx.api.agent.AgentState;

/**
 * Interface representing the primary agent execution loop.
 */
public interface AgentLoop {

    /**
     * Executes the agent loop starting with the given state.
     *
     * @param state the current state of the agent
     * @return the final agent response
     */
    AgentResponse execute(AgentState state);
}
