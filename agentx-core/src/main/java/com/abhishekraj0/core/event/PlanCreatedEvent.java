package com.abhishekraj0.core.event;

import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.planner.Plan;
import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when an Agent execution plan is created.
 */
public record PlanCreatedEvent(String id, String executionId, Plan plan, Instant timestamp) implements AgentEvent {

    public PlanCreatedEvent(String executionId, Plan plan, Instant timestamp) {
        this(UUID.randomUUID().toString(), executionId, plan, timestamp);
    }

    @Override
    public String type() {
        return "PlanCreated";
    }
}
