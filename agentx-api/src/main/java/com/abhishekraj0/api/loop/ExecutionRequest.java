package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;

/**
 * Encapsulates the input request and current state to trigger an execution step.
 */
public record ExecutionRequest(
        AgentRequest request,
        AgentState state
) {}
