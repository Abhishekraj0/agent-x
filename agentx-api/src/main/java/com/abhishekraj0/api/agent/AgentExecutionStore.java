package com.abhishekraj0.api.agent;

import java.util.Optional;

/**
 * Interface to store and retrieve AgentExecutionSnapshots for durable execution.
 */
public interface AgentExecutionStore {

    void save(AgentExecutionSnapshot snapshot);

    Optional<AgentExecutionSnapshot> find(String executionId);

    void delete(String executionId);
}
