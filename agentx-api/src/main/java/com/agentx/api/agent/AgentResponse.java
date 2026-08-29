package com.agentx.api.agent;

import com.agentx.api.event.AgentEvent;
import java.util.List;

/**
 * Structured response returned by the agent.
 */
public record AgentResponse(
        String output,
        AgentState state,
        List<AgentEvent> events
) {}
