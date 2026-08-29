package com.abhishekraj0.api.agent;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry to keep track of active and available agents.
 */
public interface AgentRegistry {

    /**
     * Registers an agent with a given name.
     *
     * @param name  the name of the agent
     * @param agent the agent instance
     */
    void register(String name, Agent agent);

    /**
     * Retrieves an agent by name.
     *
     * @param name the agent name
     * @return the agent if found
     */
    Optional<Agent> get(String name);

    /**
     * Returns all registered agents.
     *
     * @return the collection of registered agents
     */
    Collection<Agent> all();
}
