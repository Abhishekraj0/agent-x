package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.event.AgentEvent;

/**
 * Interface to update the agent's state based on an event.
 */
public interface StateUpdater {
    AgentState update(AgentState state, AgentEvent event);
}
