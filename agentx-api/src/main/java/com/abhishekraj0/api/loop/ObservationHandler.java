package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;

/**
 * Interface to update the agent state with an observation.
 */
public interface ObservationHandler {
    AgentState handle(AgentObservation observation, AgentState state);
}
