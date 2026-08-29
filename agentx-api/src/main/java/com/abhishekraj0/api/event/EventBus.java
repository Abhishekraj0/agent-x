package com.abhishekraj0.api.event;

import java.util.function.Consumer;

/**
 * Event bus to publish and subscribe to agent execution events.
 */
public interface EventBus {

    /**
     * Publishes an event to all subscribers.
     *
     * @param event the agent event
     */
    void publish(AgentEvent event);

    /**
     * Subscribes a consumer to events of a specific type.
     *
     * @param type     the event type class
     * @param consumer the consumer callback
     * @param <T>      the type of event
     */
    <T extends AgentEvent> void subscribe(Class<T> type, Consumer<T> consumer);
}
