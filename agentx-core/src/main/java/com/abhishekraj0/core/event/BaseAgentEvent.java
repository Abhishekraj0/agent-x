package com.abhishekraj0.core.event;

import com.abhishekraj0.api.event.AgentEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class containing shared event attributes.
 */
public abstract class BaseAgentEvent implements AgentEvent {
    
    private final String id = UUID.randomUUID().toString();
    private final String executionId;
    private final Instant timestamp = Instant.now();

    protected BaseAgentEvent(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String executionId() {
        return executionId;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }
}
