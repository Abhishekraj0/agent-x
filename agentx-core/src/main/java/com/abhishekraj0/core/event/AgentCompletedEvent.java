package com.abhishekraj0.core.event;

import com.abhishekraj0.api.event.AgentEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when an Agent completes execution successfully.
 */
public record AgentCompletedEvent(String id, String executionId, String output, Instant timestamp) implements AgentEvent {

    public AgentCompletedEvent(String executionId, String output, Instant timestamp) {
        this(UUID.randomUUID().toString(), executionId, output, timestamp);
    }

    @Override
    public String type() {
        return "AgentCompleted";
    }
}
