package com.agentx.api.loop;

import com.agentx.api.agent.AgentRequest;
import com.agentx.api.agent.AgentState;

/**
 * Encapsulates the input request and current state to trigger an execution step.
 */
public record ExecutionRequest(
        AgentRequest request,
        AgentState state
) {}
