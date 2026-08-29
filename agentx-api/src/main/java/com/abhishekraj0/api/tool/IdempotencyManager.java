package com.abhishekraj0.api.tool;

/**
 * Checks and records tool executions to prevent duplicate actions.
 */
public interface IdempotencyManager {

    IdempotencyDecision check(ToolExecutionRequest request);

    void record(ToolExecutionResult result);
}
