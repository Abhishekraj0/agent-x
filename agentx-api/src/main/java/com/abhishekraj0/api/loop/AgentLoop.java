package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentResponse;
import com.abhishekraj0.api.agent.AgentState;

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
