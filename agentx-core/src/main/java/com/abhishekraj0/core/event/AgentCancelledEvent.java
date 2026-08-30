package com.abhishekraj0.core.event;

import com.abhishekraj0.api.event.AgentEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when an Agent execution is cancelled cooperatively.
 */
public record AgentCancelledEvent(String id, String executionId, String reason, Instant timestamp) implements AgentEvent {

    public AgentCancelledEvent(String executionId, String reason, Instant timestamp) {
        this(UUID.randomUUID().toString(), executionId, reason, timestamp);
    }

    @Override
    public String type() {
        return "AgentCancelled";
    }
}
