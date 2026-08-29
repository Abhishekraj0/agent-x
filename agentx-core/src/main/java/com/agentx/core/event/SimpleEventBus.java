package com.agentx.core.event;

import com.agentx.api.event.AgentEvent;
import com.agentx.api.event.EventBus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe implementation of EventBus.
 */
public class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(AgentEvent event) {
        if (event == null) return;
        Class<?> clazz = event.getClass();
        
        for (Map.Entry<Class<?>, List<Consumer<?>>> entry : subscribers.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                for (Consumer<?> consumer : entry.getValue()) {
                    @SuppressWarnings("unchecked")
                    Consumer<AgentEvent> castConsumer = (Consumer<AgentEvent>) consumer;
                    try {
                        castConsumer.accept(event);
                    } catch (Exception e) {
                        // Suppress consumer exceptions so other consumers still receive the event
                    }
                }
            }
        }
    }

    @Override
    public <T extends AgentEvent> void subscribe(Class<T> type, Consumer<T> consumer) {
        if (type == null || consumer == null) return;
        subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(consumer);
    }
}
