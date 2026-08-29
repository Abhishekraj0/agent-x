package com.abhishekraj0.api.loop;

/**
 * Interface representing the engine that runs individual execution steps of an agent loop.
 */
public interface ExecutionEngine {

    /**
     * Executes a single processing request step.
     *
     * @param request the execution request
     * @return the execution result
     */
    ExecutionResult execute(ExecutionRequest request);
}
