package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.AgentExecutionSnapshot;
import com.abhishekraj0.api.agent.AgentExecutionStore;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of AgentExecutionStore supporting optimistic locking.
 */
public class InMemoryAgentExecutionStore implements AgentExecutionStore {

    private final Map<String, AgentExecutionSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public synchronized void save(AgentExecutionSnapshot snapshot) {
        if (snapshot == null || snapshot.executionId() == null) {
            return;
        }

        AgentExecutionSnapshot existing = store.get(snapshot.executionId());
        if (existing != null) {
            if (snapshot.version() > 0 && snapshot.version() != existing.version()) {
                throw new ConcurrentModificationException("Optimistic locking failure for execution " + snapshot.executionId() +
                        ": expected version " + snapshot.version() + " but store has " + existing.version());
            }
            int newVersion = Math.max(existing.version() + 1, snapshot.version() + 1);
            AgentExecutionSnapshot newSnapshot = new AgentExecutionSnapshot(
                    snapshot.executionId(),
                    snapshot.agentId(),
                    snapshot.goal(),
                    snapshot.state(),
                    snapshot.loopState(),
                    snapshot.plan(),
                    snapshot.iteration(),
                    snapshot.toolCallCount(),
                    snapshot.observations(),
                    snapshot.memoryReferences(),
                    snapshot.pendingDecision(),
                    snapshot.approvalState(),
                    snapshot.budgets(),
                    snapshot.timestamp(),
                    snapshot.metadata(),
                    newVersion
            );
            store.put(snapshot.executionId(), newSnapshot);
        } else {
            int initVersion = snapshot.version() > 0 ? snapshot.version() : 1;
            AgentExecutionSnapshot newSnapshot = new AgentExecutionSnapshot(
                    snapshot.executionId(),
                    snapshot.agentId(),
                    snapshot.goal(),
                    snapshot.state(),
                    snapshot.loopState(),
                    snapshot.plan(),
                    snapshot.iteration(),
                    snapshot.toolCallCount(),
                    snapshot.observations(),
                    snapshot.memoryReferences(),
                    snapshot.pendingDecision(),
                    snapshot.approvalState(),
                    snapshot.budgets(),
                    snapshot.timestamp(),
                    snapshot.metadata(),
                    initVersion
            );
            store.put(snapshot.executionId(), newSnapshot);
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
    public synchronized void delete(String executionId) {
        if (executionId != null) {
            store.remove(executionId);
        }
    }
}
