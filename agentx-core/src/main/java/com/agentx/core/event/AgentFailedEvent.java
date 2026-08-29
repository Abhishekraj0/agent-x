package com.agentx.core.event;

import com.agentx.api.event.AgentEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when an Agent execution fails.
 */
public record AgentFailedEvent(String id, String executionId, Throwable error, Instant timestamp) implements AgentEvent {

    public AgentFailedEvent(String executionId, Throwable error, Instant timestamp) {
        this(UUID.randomUUID().toString(), executionId, error, timestamp);
    }

    @Override
    public String type() {
        return "AgentFailed";
    }
}
