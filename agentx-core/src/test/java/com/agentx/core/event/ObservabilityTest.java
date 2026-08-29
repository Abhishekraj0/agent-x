package com.agentx.core.event;

import static org.junit.jupiter.api.Assertions.*;

import com.agentx.api.event.AgentEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ObservabilityTest {

    @Test
    public void testEventBusSubscriptionAndPublishing() {
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> receivedEvents = new ArrayList<>();
        List<ToolCalledEvent> receivedToolEvents = new ArrayList<>();

        // Subscribe to base event
        eventBus.subscribe(AgentEvent.class, receivedEvents::add);
        // Subscribe to specialized tool event
        eventBus.subscribe(ToolCalledEvent.class, receivedToolEvents::add);

        // Publish events
        ExecutionStartedEvent start = new ExecutionStartedEvent("exec-1", "Hello");
        ToolCalledEvent tool = new ToolCalledEvent("exec-1", "calculator", "{\"val\": 1}", "2");
        ExecutionCompletedEvent end = new ExecutionCompletedEvent("exec-1", "Result: 2");

        eventBus.publish(start);
        eventBus.publish(tool);
        eventBus.publish(end);

        // Verify broad subscriber received all events
        assertEquals(3, receivedEvents.size());
        assertTrue(receivedEvents.get(0) instanceof ExecutionStartedEvent);
        assertTrue(receivedEvents.get(1) instanceof ToolCalledEvent);
        assertTrue(receivedEvents.get(2) instanceof ExecutionCompletedEvent);

        // Verify narrow subscriber only received ToolCalledEvent
        assertEquals(1, receivedToolEvents.size());
        assertEquals("calculator", receivedToolEvents.get(0).toolName());
        assertEquals("{\"val\": 1}", receivedToolEvents.get(0).argumentsJson());
        assertEquals("2", receivedToolEvents.get(0).output());
    }
}
