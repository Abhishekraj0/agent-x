package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;

/**
 * Result returned after executing a processing engine step.
 */
public record ExecutionResult(
        AgentState state,
        String output,
        boolean success,
        Throwable error
) {}
