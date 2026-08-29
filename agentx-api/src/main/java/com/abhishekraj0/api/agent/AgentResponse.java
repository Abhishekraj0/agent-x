package com.abhishekraj0.api.agent;

import com.abhishekraj0.api.event.AgentEvent;
import java.util.List;

/**
 * Structured response returned by the agent.
 */
public record AgentResponse(
        String output,
        AgentState state,
        List<AgentEvent> events
) {}
