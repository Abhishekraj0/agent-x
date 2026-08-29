package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.loop.StateUpdater;

/**
 * Default implementation of StateUpdater.
 */
public class DefaultStateUpdater implements StateUpdater {

    @Override
    public AgentState update(AgentState state, AgentEvent event) {
        // Simple passthrough since our state is transition-driven.
        return state;
    }
}
