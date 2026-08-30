package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.event.EventBus;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
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

public class CancellationPropagationTest {

    @Test
    public void testCancellationFlow() throws Exception {
        String execId = UUID.randomUUID().toString();
        System.out.println("Starting test with execId: " + execId);
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribe(AgentFailedEvent.class, receivedEvents::add);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        AtomicBoolean toolStoppedCooperatively = new AtomicBoolean(false);
        CountDownLatch toolStartedLatch = new CountDownLatch(1);

        toolRegistry.register(new FunctionTool(
                new ToolId("long-running"),
                "long running tool",
                ToolSchema.empty(),
                context -> {
                    System.out.println("Tool started running.");
                    CancellationToken token = context.cancellationToken();
                    System.out.println("Tool context token: " + token);
                    
                    // Signal that the tool has started running
                    toolStartedLatch.countDown();

                    for (int i = 0; i < 200; i++) {
                        if (token != null && token.isCancelled()) {
                            System.out.println("Tool detected cancellation at iteration: " + i);
                            toolStoppedCooperatively.set(true);
                            token.throwIfCancelled();
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            System.out.println("Tool sleep interrupted!");
                            Thread.currentThread().interrupt();
                            toolStoppedCooperatively.set(true);
                            throw new RuntimeException(e);
                        }
                    }
                    System.out.println("Tool finished 200 iterations successfully!");
                    return ToolResult.success("completed");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            System.out.println("MockChatModel handler invoked.");
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

        assertTrue(agent instanceof AgentRuntime, "Agent should implement AgentRuntime for cancellation");
        AgentRuntime runtime = (AgentRuntime) agent;

        AgentOptions options = new AgentOptions(5, 5, Duration.ofSeconds(10), 0.7, Map.of());
        AgentRequest request = new AgentRequest("Run long running task", execId, options);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        System.out.println("Submitting execute request to runner thread...");
        Future<AgentResponse> future = executor.submit(() -> runtime.execute(request));

        // Wait for the tool to actually start running before triggering cancel
        boolean toolStarted = toolStartedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(toolStarted, "Tool should have started running within timeout");

        System.out.println("Calling runtime.cancel(" + execId + ")...");
        runtime.cancel(execId);

        AgentResponse response = future.get(5, TimeUnit.SECONDS);
        System.out.println("Response state status: " + response.state().status());
        System.out.println("toolStoppedCooperatively: " + toolStoppedCooperatively.get());

        assertTrue(toolStoppedCooperatively.get(), "Tool should have cooperative cancellation flag set");

        // The execution state should end in CANCELLED
        AgentExecution exec = runtime.getExecution(execId);
        assertNotNull(exec);
        assertEquals("CANCELLED", exec.state().status()); // AgentState status becomes CANCELLED

        // Verify that AgentFailedEvent was emitted (indicating cancellation / failure)
        assertFalse(receivedEvents.isEmpty(), "Events should have been received");
        boolean hasCancellationEvent = receivedEvents.stream().anyMatch(event -> {
            if (event instanceof AgentFailedEvent afe) {
                return afe.error() != null && afe.error().getMessage() != null && afe.error().getMessage().contains("cancelled");
            }
            return false;
        });
        assertTrue(hasCancellationEvent, "Should have received an AgentFailedEvent with cancellation details");

        executor.shutdownNow();
    }
}
