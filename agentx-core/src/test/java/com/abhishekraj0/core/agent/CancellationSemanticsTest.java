package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.event.AgentCancelledEvent;
import com.abhishekraj0.core.event.AgentFailedEvent;
import com.abhishekraj0.core.event.SimpleEventBus;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class CancellationSemanticsTest {

    @Test
    public void testUserCancellationSemantics() throws Exception {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> cancelledEvents = new ArrayList<>();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentCancelledEvent.class, cancelledEvents::add);
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        CountDownLatch toolStartedLatch = new CountDownLatch(1);
        AtomicBoolean toolCancelled = new AtomicBoolean(false);

        toolRegistry.register(new FunctionTool(
                new ToolId("long-running"),
                "long running tool",
                ToolSchema.empty(),
                context -> {
                    toolStartedLatch.countDown();
                    CancellationToken token = context.cancellationToken();
                    for (int i = 0; i < 100; i++) {
                        if (token != null && token.isCancelled()) {
                            toolCancelled.set(true);
                            token.throwIfCancelled();
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            toolCancelled.set(true);
                            throw new RuntimeException(e);
                        }
                    }
                    return ToolResult.success("completed");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            ToolCall call = new ToolCall("call-1", "long-running", "{}");
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(call)),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .build();

        assertTrue(agent instanceof AgentRuntime);
        AgentRuntime runtime = (AgentRuntime) agent;

        AgentOptions options = new AgentOptions(5, 5, Duration.ofSeconds(10), 0.7, Map.of());
        AgentRequest request = new AgentRequest("Run long running task", execId, options);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<AgentResponse> future = executor.submit(() -> runtime.execute(request));

        assertTrue(toolStartedLatch.await(5, TimeUnit.SECONDS));
        runtime.cancel(execId);

        AgentResponse response = future.get(5, TimeUnit.SECONDS);

        // Assert 1: user cancellation -> CANCELLED
        assertEquals("CANCELLED", response.state().status());

        // Assert 4: cancellation event emitted
        assertFalse(cancelledEvents.isEmpty(), "Should emit AgentCancelledEvent");

        // Assert 5: failure event not emitted for pure cancellation
        assertTrue(failedEvents.isEmpty(), "Should not emit AgentFailedEvent for pure cancellation");

        // Assert 6: resources released (cancellation token deregistered)
        assertNull(DefaultCancellationToken.get(execId), "Cancellation token should be deregistered");

        executor.shutdownNow();
    }

    @Test
    public void testTimeoutSemantics() throws Exception {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> cancelledEvents = new ArrayList<>();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentCancelledEvent.class, cancelledEvents::add);
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("sleepy-tool"),
                "sleepy tool",
                ToolSchema.empty(),
                context -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ToolResult.success("slept");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            ToolCall call = new ToolCall("call-1", "sleepy-tool", "{}");
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(call)),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .build();

        AgentOptions options = new AgentOptions(5, 5, Duration.ofMillis(100), 0.7, Map.of());
        AgentRequest request = new AgentRequest("Run quick timeout task", execId, options);

        AgentResponse response = ((AgentRuntime) agent).execute(request);

        // Assert 2: timeout -> TIMEOUT
        assertEquals("TIMEOUT", response.state().status());

        // Assert 4: cancellation event not emitted for timeout
        assertTrue(cancelledEvents.isEmpty(), "Should not emit AgentCancelledEvent for timeout");

        // Assert 5: failure event emitted for timeout
        assertFalse(failedEvents.isEmpty(), "Should emit AgentFailedEvent for timeout");

        // Assert 6: resources released
        assertNull(DefaultCancellationToken.get(execId), "Cancellation token should be deregistered");
    }

    @Test
    public void testToolFailureSemantics() throws Exception {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> cancelledEvents = new ArrayList<>();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentCancelledEvent.class, cancelledEvents::add);
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("failing-tool"),
                "failing tool",
                ToolSchema.empty(),
                context -> {
                    throw new RuntimeException("Tool error occurred");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            ToolCall call = new ToolCall("call-1", "failing-tool", "{}");
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(call)),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        // Use custom goal evaluator that fails if any observation has failure
        GoalEvaluator goalEvaluator = state -> {
            boolean hasError = state.history().stream()
                    .anyMatch(msg -> msg.content() != null && (msg.content().contains("Tool error occurred") || msg.content().contains("error")));
            if (hasError) {
                return GoalStatus.FAILED;
            }
            return GoalStatus.IN_PROGRESS;
        };

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .goalEvaluator(goalEvaluator)
                .build();

        AgentOptions options = new AgentOptions(5, 5, Duration.ofSeconds(10), 0.7, Map.of());
        AgentRequest request = new AgentRequest("Run task with failing tool", execId, options);

        AgentResponse response = ((AgentRuntime) agent).execute(request);

        // Assert 3: tool failure -> FAILED
        assertEquals("FAILED", response.state().status());

        // Assert 4: failure event emitted
        assertFalse(failedEvents.isEmpty(), "Should emit AgentFailedEvent");

        // Assert 5: cancellation event not emitted for failure
        assertTrue(cancelledEvents.isEmpty(), "Should not emit AgentCancelledEvent");

        // Assert 6: resources released
        assertNull(DefaultCancellationToken.get(execId), "Cancellation token should be deregistered");
    }
}
