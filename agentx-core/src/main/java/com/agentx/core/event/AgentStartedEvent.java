package com.agentx.core.event;

import com.agentx.api.event.AgentEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when an Agent begins execution.
 */
public record AgentStartedEvent(String id, String executionId, Instant timestamp) implements AgentEvent {

    public AgentStartedEvent(String executionId, Instant timestamp) {
        this(UUID.randomUUID().toString(), executionId, timestamp);
    }

    @Override
    public String type() {
        return "AgentStarted";
    }
}
