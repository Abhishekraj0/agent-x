package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.api.agent.AgentRegistry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of AgentRegistry.
 */
public class DefaultAgentRegistry implements AgentRegistry {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    @Override
    public void register(String name, Agent agent) {
        if (name == null || agent == null) {
            throw new IllegalArgumentException("Name and agent must not be null");
        }
        agents.put(name, agent);
    }

    @Override
    public Optional<Agent> get(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.get(name));
    }

    @Override
    public Collection<Agent> all() {
        return agents.values();
    }
}
