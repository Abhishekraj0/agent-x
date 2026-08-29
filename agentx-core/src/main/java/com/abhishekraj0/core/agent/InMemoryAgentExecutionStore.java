package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.AgentExecutionSnapshot;
import com.abhishekraj0.api.agent.AgentExecutionStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of AgentExecutionStore.
 */
public class InMemoryAgentExecutionStore implements AgentExecutionStore {

    private final Map<String, AgentExecutionSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public void save(AgentExecutionSnapshot snapshot) {
        if (snapshot != null && snapshot.executionId() != null) {
            store.put(snapshot.executionId(), snapshot);
        }
    }

    @Override
    public Optional<AgentExecutionSnapshot> find(String executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(executionId));
    }

    @Override
    public void delete(String executionId) {
        if (executionId != null) {
            store.remove(executionId);
        }
    }
}
