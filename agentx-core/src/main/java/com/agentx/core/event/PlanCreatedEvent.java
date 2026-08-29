package com.agentx.core.event;

import com.agentx.api.event.AgentEvent;
import com.agentx.api.planner.Plan;
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
