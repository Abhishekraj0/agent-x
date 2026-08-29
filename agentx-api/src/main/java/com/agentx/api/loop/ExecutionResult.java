package com.agentx.api.loop;

import com.agentx.api.agent.AgentState;

/**
 * Result returned after executing a processing engine step.
 */
public record ExecutionResult(
        AgentState state,
        String output,
        boolean success,
        Throwable error
) {}
