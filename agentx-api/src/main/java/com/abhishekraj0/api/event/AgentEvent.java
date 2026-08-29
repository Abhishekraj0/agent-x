package com.abhishekraj0.api.event;

import java.time.Instant;

/**
 * Base interface for all events published during the execution of an agent.
 */
public interface AgentEvent {

    /**
     * Unique identifier for the event.
     *
     * @return the event ID
     */
    String id();

    /**
     * The ID of the agent execution context.
     *
     * @return the execution ID
     */
    String executionId();

    /**
     * The timestamp when the event occurred.
     *
     * @return the timestamp
     */
    Instant timestamp();

    /**
     * A human-readable event type or classification.
     *
     * @return the event type
     */
    String type();
}
