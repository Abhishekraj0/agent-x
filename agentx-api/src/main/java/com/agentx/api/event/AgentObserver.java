package com.agentx.api.event;

/**
 * Interface representing a component that listens and reacts to AgentEvents.
 */
public interface AgentObserver {

    /**
     * Callback executed when an AgentEvent occurs.
     *
     * @param event the agent event
     */
    void onEvent(AgentEvent event);
}
