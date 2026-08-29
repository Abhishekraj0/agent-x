package com.agentx.api.agent;

import java.time.Instant;

/**
 * Tracks historical or running execution of an agent.
 */
public record AgentExecution(
        String executionId,
        AgentRequest request,
        AgentState state,
        Instant startTime,
        Instant endTime
) {}
