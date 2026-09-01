package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.ApprovalResult;
import com.abhishekraj0.api.security.PermissionDecision;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.security.PermissionStatus;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ConcurrentResumeOwnershipTest {

    @Test
    public void test2_CriticalOwnershipTest() throws Exception {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        MockChatModel modelA = new MockChatModel();
        modelA.setHandler(req -> new ChatResponse(ChatMessage.assistant("Runtime A output"), new TokenUsage(5, 5, 10), "STOP"));

        MockChatModel modelB = new MockChatModel();
        modelB.setHandler(req -> new ChatResponse(ChatMessage.assistant("Runtime B output"), new TokenUsage(5, 5, 10), "STOP"));

        Agent agentA = AgentX.builder().model(modelA).executionStore(store).goalEvaluator(s -> GoalStatus.COMPLETE).build();
        Agent agentB = AgentX.builder().model(modelB).executionStore(store).goalEvaluator(s -> GoalStatus.COMPLETE).build();

        // Setup execution in WAITING_APPROVAL
        AgentState state = new AgentState(execId, List.of(), null, Map.of("pending_tool_call_id", "call-1"), 0, 0, "WAITING_APPROVAL");
        AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                execId, "agent-1", "test goal", state, "WAITING_FOR_APPROVAL", null, 0, 0, List.of(), List.of(), null, null, null, java.time.Instant.now(), Map.of(), 1
        );
        store.save(snapshot);

        CountDownLatch barrier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "user", "ok"));

        Future<Object> fA = executor.submit(() -> {
            barrier.await();
            return ((ResumableAgentRuntime) agentA).resume(execId, input);
        });

        Future<Object> fB = executor.submit(() -> {
            barrier.await();
            return ((ResumableAgentRuntime) agentB).resume(execId, input);
        });

        barrier.countDown();

        int ownerCount = 0;
        int rejectedCount = 0;

        for (Future<Object> f : List.of(fA, fB)) {
            try {
                Object res = f.get(5, TimeUnit.SECONDS);
                if (res instanceof AgentResponse) {
                    ownerCount++;
                }
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof AgentFailure af && "EXECUTION_ALREADY_RUNNING".equals(af.getCode())) {
                    rejectedCount++;
                } else if (ee.getCause() instanceof java.util.ConcurrentModificationException) {
                    rejectedCount++;
                } else {
                    throw ee;
                }
            }
        }

        executor.shutdownNow();

        assertEquals(1, ownerCount, "Exactly ONE runtime must acquire ownership");
        assertEquals(1, rejectedCount, "Exactly ONE runtime must be rejected");
    }

    @Test
    public void test3_and_5_ToolAndModelDuplicationPrevention() throws Exception {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        AtomicInteger modelInvocations = new AtomicInteger(0);
        AtomicInteger toolInvocations = new AtomicInteger(0);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("counting-tool"),
                "counting tool",
                ToolSchema.empty(),
                c -> {
                    toolInvocations.incrementAndGet();
                    return ToolResult.success("count updated");
                },
                new ToolMetadata(RiskLevel.HIGH, true, false, false, Duration.ofSeconds(5))
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            modelInvocations.incrementAndGet();
            boolean hasToolResult = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("count updated"));
            if (hasToolResult) {
                return new ChatResponse(ChatMessage.assistant("Tool completed successfully"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(ChatMessage.assistant(null, List.of(new ToolCall("call-1", "counting-tool", "{}"))), new TokenUsage(5, 5, 10), "TOOL_USE");
        });

        PermissionManager permissionManager = (action, context) ->
                new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, "Requires approval");

        Agent agentA = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Tool completed successfully")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        Agent agentB = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Tool completed successfully")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        // 1. Execute agent until it reaches WAITING_APPROVAL
        AgentResponse initialResponse = ((AgentRuntime) agentA).execute(new AgentRequest("Run task", execId, AgentOptions.defaultOptions()));
        assertEquals("WAITING_APPROVAL", initialResponse.state().status());

        int initialModelCalls = modelInvocations.get();

        // 2. Simultaneously resume with Agent A and Agent B
        CountDownLatch barrier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "user", "ok"));

        Future<Object> fA = executor.submit(() -> {
            barrier.await();
            return ((ResumableAgentRuntime) agentA).resume(execId, input);
        });

        Future<Object> fB = executor.submit(() -> {
            barrier.await();
            return ((ResumableAgentRuntime) agentB).resume(execId, input);
        });

        barrier.countDown();

        int ownerCount = 0;
        int rejectedCount = 0;

        for (Future<Object> f : List.of(fA, fB)) {
            try {
                Object res = f.get(5, TimeUnit.SECONDS);
                if (res instanceof AgentResponse) {
                    ownerCount++;
                }
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof AgentFailure af && "EXECUTION_ALREADY_RUNNING".equals(af.getCode())) {
                    rejectedCount++;
                } else if (ee.getCause() instanceof java.util.ConcurrentModificationException) {
                    rejectedCount++;
                }
            }
        }

        executor.shutdownNow();

        assertEquals(1, ownerCount, "Exactly 1 runtime owns and completes execution");
        assertEquals(1, rejectedCount, "Exactly 1 runtime is rejected");
        assertEquals(1, toolInvocations.get(), "Tool must be invoked exactly ONCE");
        assertEquals(initialModelCalls + 2, modelInvocations.get(), "Only owning runtime performs model execution on resume");
    }

    @Test
    public void test4_BlockInsideToolConcurrencySafety() throws Exception {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        CountDownLatch toolStarted = new CountDownLatch(1);
        CountDownLatch releaseTool = new CountDownLatch(1);
        AtomicInteger toolExecutions = new AtomicInteger(0);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("blocking-tool"),
                "blocking tool",
                ToolSchema.empty(),
                c -> {
                    toolExecutions.incrementAndGet();
                    toolStarted.countDown();
                    try {
                        releaseTool.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ToolResult.success("block released");
                },
                new ToolMetadata(RiskLevel.HIGH, true, false, false, Duration.ofSeconds(5))
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean done = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("block released"));
            if (done) {
                return new ChatResponse(ChatMessage.assistant("Done"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(ChatMessage.assistant(null, List.of(new ToolCall("call-1", "blocking-tool", "{}"))), new TokenUsage(5, 5, 10), "TOOL_USE");
        });

        PermissionManager permissionManager = (action, context) ->
                new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, "Requires approval");

        Agent agentA = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Done")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        Agent agentB = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Done")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        AgentResponse initialResponse = ((AgentRuntime) agentA).execute(new AgentRequest("Blocking task", execId, AgentOptions.defaultOptions()));
        assertEquals("WAITING_APPROVAL", initialResponse.state().status());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "user", "ok"));

        // Start resume on Agent A (which will enter blocking-tool)
        Future<AgentResponse> futureA = executor.submit(() -> ((ResumableAgentRuntime) agentA).resume(execId, input));

        // Wait until tool starts executing in Agent A
        assertTrue(toolStarted.await(5, TimeUnit.SECONDS), "Tool should start running in Agent A");

        // While Agent A is blocked inside tool execution, Agent B attempts to resume
        AgentFailure failureB = assertThrows(AgentFailure.class, () -> ((ResumableAgentRuntime) agentB).resume(execId, input));
        assertEquals("EXECUTION_ALREADY_RUNNING", failureB.getCode());

        // Now release tool in Agent A and verify completion
        releaseTool.countDown();
        AgentResponse responseA = futureA.get(5, TimeUnit.SECONDS);

        assertEquals("COMPLETED", responseA.state().status());
        assertEquals(1, toolExecutions.get(), "Tool must execute exactly once");

        executor.shutdownNow();
    }

    @Test
    public void test10_ConcurrentResumeStress_100Runtimes() throws Exception {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        AtomicInteger toolInvocations = new AtomicInteger(0);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("stress-tool"),
                "stress tool",
                ToolSchema.empty(),
                c -> {
                    toolInvocations.incrementAndGet();
                    return ToolResult.success("stress test ok");
                },
                new ToolMetadata(RiskLevel.HIGH, true, false, false, Duration.ofSeconds(5))
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean done = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("stress test ok"));
            if (done) {
                return new ChatResponse(ChatMessage.assistant("Done"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(ChatMessage.assistant(null, List.of(new ToolCall("call-1", "stress-tool", "{}"))), new TokenUsage(5, 5, 10), "TOOL_USE");
        });

        PermissionManager permissionManager = (action, context) ->
                new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, "Requires approval");

        int runtimeCount = 100;
        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < runtimeCount; i++) {
            Agent agent = AgentX.builder()
                    .model(model)
                    .tools(toolRegistry)
                    .permissionManager(permissionManager)
                    .executionStore(store)
                    .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Done")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                    .build();
            agents.add(agent);
        }

        // Setup execution in WAITING_APPROVAL
        AgentResponse initialResponse = ((AgentRuntime) agents.get(0)).execute(new AgentRequest("Stress task", execId, AgentOptions.defaultOptions()));
        assertEquals("WAITING_APPROVAL", initialResponse.state().status());

        CountDownLatch startBarrier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(runtimeCount);
        List<Future<Object>> futures = new ArrayList<>();

        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "user", "ok"));

        for (Agent a : agents) {
            futures.add(executor.submit(() -> {
                startBarrier.await();
                return ((ResumableAgentRuntime) a).resume(execId, input);
            }));
        }

        // Release all 100 threads simultaneously
        startBarrier.countDown();

        int ownerCount = 0;
        int rejectedCount = 0;

        for (Future<Object> f : futures) {
            try {
                Object res = f.get(10, TimeUnit.SECONDS);
                if (res instanceof AgentResponse) {
                    ownerCount++;
                }
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof AgentFailure af && "EXECUTION_ALREADY_RUNNING".equals(af.getCode())) {
                    rejectedCount++;
                } else if (ee.getCause() instanceof java.util.ConcurrentModificationException) {
                    rejectedCount++;
                }
            }
        }

        executor.shutdownNow();

        assertEquals(1, ownerCount, "Exactly 1 runtime must own and complete execution out of 100");
        assertEquals(99, rejectedCount, "Exactly 99 runtimes must be rejected out of 100");
        assertEquals(1, toolInvocations.get(), "Tool must be invoked exactly ONCE across all 100 concurrent resumes");
    }
}
