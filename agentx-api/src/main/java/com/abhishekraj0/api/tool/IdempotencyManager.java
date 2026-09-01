package com.abhishekraj0.api.tool;

/**
 * Checks and records tool executions to prevent duplicate actions and handle crash recovery.
 */
public interface IdempotencyManager {

    IdempotencyDecision check(ToolExecutionRequest request);

    default void recordPending(ToolExecutionRequest request) {}

    void record(ToolExecutionResult result);
}
