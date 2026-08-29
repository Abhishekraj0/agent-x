package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;

/**
 * Result representing the final outcome of running the agent loop.
 */
public record LoopResult(
        AgentState finalState,
        String output,
        boolean success,
        Throwable error
) {}
