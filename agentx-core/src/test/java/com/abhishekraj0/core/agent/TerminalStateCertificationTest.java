package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.ApprovalResult;
import com.abhishekraj0.api.security.PermissionDecision;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.security.PermissionStatus;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.event.AgentCancelledEvent;
import com.abhishekraj0.core.event.AgentCompletedEvent;
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

public class TerminalStateCertificationTest {

    @Test
    public void testNormalCompletion() {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> completedEvents = new ArrayList<>();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentCompletedEvent.class, completedEvents::add);
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant("Goal completed successfully."),
                new TokenUsage(10, 5, 15),
                "STOP"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .eventBus(eventBus)
                .goalEvaluator(s -> GoalStatus.COMPLETE)
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Complete task", execId, AgentOptions.defaultOptions()));

        assertEquals("COMPLETED", response.state().status());
        assertFalse(completedEvents.isEmpty(), "Should emit AgentCompletedEvent");
        assertTrue(failedEvents.isEmpty(), "Should not emit AgentFailedEvent");
    }

    @Test
    public void testToolFailure() {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("failing-tool"),
                "failing tool",
                ToolSchema.empty(),
                context -> {
                    throw new RuntimeException("Unrecoverable tool failure");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-1", "failing-tool", "{}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .goalEvaluator(s -> {
                    boolean errorObserved = s.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("Unrecoverable tool failure"));
                    return errorObserved ? GoalStatus.FAILED : GoalStatus.IN_PROGRESS;
                })
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Run failing tool", execId, AgentOptions.defaultOptions()));

        assertEquals("FAILED", response.state().status());
        assertFalse(failedEvents.isEmpty(), "Should emit AgentFailedEvent for tool failure");
    }

    @Test
    public void testModelFailure() {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            throw new RuntimeException("Model provider unreachable");
        });

        Agent agent = AgentX.builder()
                .model(model)
                .eventBus(eventBus)
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Failing model", execId, AgentOptions.defaultOptions()));

        assertEquals("FAILED", response.state().status());
        assertFalse(failedEvents.isEmpty(), "Should emit AgentFailedEvent for model failure");
    }

    @Test
    public void testUserCancellation() throws Exception {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> cancelledEvents = new ArrayList<>();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentCancelledEvent.class, cancelledEvents::add);
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        CountDownLatch toolStartedLatch = new CountDownLatch(1);
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("slow-tool"),
                "slow tool",
                ToolSchema.empty(),
                context -> {
                    toolStartedLatch.countDown();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ToolResult.success("slow result");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-1", "slow-tool", "{}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<AgentResponse> future = executor.submit(() -> ((AgentRuntime) agent).execute(new AgentRequest("Slow task", execId, AgentOptions.defaultOptions())));

        assertTrue(toolStartedLatch.await(2, TimeUnit.SECONDS));
        ((AgentRuntime) agent).cancel(execId);

        AgentResponse response = future.get(2, TimeUnit.SECONDS);

        assertEquals("CANCELLED", response.state().status());
        assertFalse(cancelledEvents.isEmpty(), "Must emit AgentCancelledEvent");
        assertTrue(failedEvents.isEmpty(), "Must NOT emit AgentFailedEvent for pure cancellation");

        executor.shutdownNow();
    }

    @Test
    public void testTimeout() {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> cancelledEvents = new ArrayList<>();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentCancelledEvent.class, cancelledEvents::add);
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("sleep-tool"),
                "sleep tool",
                ToolSchema.empty(),
                context -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ToolResult.success("slept");
                }
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-1", "sleep-tool", "{}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .build();

        AgentOptions options = new AgentOptions(5, 5, Duration.ofMillis(50), 0.7, Map.of());
        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Timeout task", execId, options));

        assertEquals("TIMEOUT", response.state().status());
        assertTrue(cancelledEvents.isEmpty(), "Should not emit AgentCancelledEvent for timeout");
        assertFalse(failedEvents.isEmpty(), "Should emit AgentFailedEvent for timeout");
    }

    @Test
    public void testBudgetExceeded() {
        String execId = UUID.randomUUID().toString();
        SimpleEventBus eventBus = new SimpleEventBus();
        List<AgentEvent> failedEvents = new ArrayList<>();
        eventBus.subscribe(AgentFailedEvent.class, failedEvents::add);

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-1", "noop", "{}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("noop"),
                "noop",
                ToolSchema.empty(),
                c -> ToolResult.success("ok")
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .eventBus(eventBus)
                .build();

        // Limit to max 2 iterations
        AgentOptions options = new AgentOptions(2, 10, Duration.ofSeconds(10), 0.7, Map.of());
        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Loop task", execId, options));

        assertTrue("FAILED".equals(response.state().status()) || "TIMEOUT".equals(response.state().status()));
        assertFalse(failedEvents.isEmpty(), "Should emit AgentFailedEvent when budget/iteration limit exceeded");
    }

    @Test
    public void testApprovalWait() {
        String execId = UUID.randomUUID().toString();

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("sensitive-action"),
                "sensitive action requiring approval",
                ToolSchema.empty(),
                c -> ToolResult.success("done"),
                new ToolMetadata(RiskLevel.HIGH, true, false, false, Duration.ofSeconds(5))
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-1", "sensitive-action", "{}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();

        PermissionManager permissionManager = (action, context) ->
                new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, "Requires human approval");

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Perform sensitive action", execId, AgentOptions.defaultOptions()));

        assertEquals("WAITING_APPROVAL", response.state().status());
        assertTrue(store.find(execId).isPresent());
        assertEquals("WAITING_FOR_APPROVAL", store.find(execId).get().loopState());
    }

    @Test
    public void testApprovalRejection() {
        String execId = UUID.randomUUID().toString();

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("sensitive-action"),
                "sensitive action",
                ToolSchema.empty(),
                c -> ToolResult.success("done"),
                new ToolMetadata(RiskLevel.HIGH, true, false, false, Duration.ofSeconds(5))
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasApprovalRejection = req.messages().stream()
                    .anyMatch(m -> m.content() != null && m.content().contains("Approval Rejected"));
            if (hasApprovalRejection) {
                return new ChatResponse(ChatMessage.assistant("Action was rejected by user. Stopping."), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(ChatMessage.assistant(null, List.of(new ToolCall("call-1", "sensitive-action", "{}"))), new TokenUsage(5, 5, 10), "TOOL_USE");
        });

        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();

        PermissionManager permissionManager = (action, context) ->
                new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, "Requires human approval");

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(m -> m.content() != null && m.content().contains("rejected"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        AgentResponse response1 = ((AgentRuntime) agent).execute(new AgentRequest("Sensitive task", execId, AgentOptions.defaultOptions()));
        assertEquals("WAITING_APPROVAL", response1.state().status());

        // Resume with rejected approval
        ResumeInput rejectedInput = ResumeInput.ofApproval(new ApprovalResult(false, "Admin", "User denied approval"));
        AgentResponse response2 = ((ResumableAgentRuntime) agent).resume(execId, rejectedInput);

        assertEquals("COMPLETED", response2.state().status());
        assertTrue(response2.output().contains("rejected"));
    }
}
