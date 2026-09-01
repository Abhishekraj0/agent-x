package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.ApprovalResult;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ConcurrentResumeVersionTest {

    @Test
    public void test1_BasicVersionConflict() {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        AgentState state = new AgentState(execId, List.of(), null, Map.of("pending_tool_call_id", "call-1"), 0, 0, "WAITING_APPROVAL");
        AgentExecutionSnapshot snap1 = new AgentExecutionSnapshot(
                execId, "agent-1", "test goal", state, "WAITING_FOR_APPROVAL", null, 0, 0, List.of(), List.of(), null, null, null, Instant.now(), Map.of(), 1
        );
        store.save(snap1);

        // Load snapshot version 1 for Runtime A and Runtime B
        AgentExecutionSnapshot snapA = store.find(execId).orElseThrow();
        AgentExecutionSnapshot snapB = store.find(execId).orElseThrow();
        assertEquals(1, snapA.version());
        assertEquals(1, snapB.version());

        // Runtime A claims execution
        AgentExecutionSnapshot updatedA = new AgentExecutionSnapshot(
                snapA.executionId(), snapA.agentId(), snapA.goal(), snapA.state(), "RUNNING",
                snapA.plan(), snapA.iteration(), snapA.toolCallCount(), snapA.observations(),
                snapA.memoryReferences(), snapA.pendingDecision(), snapA.approvalState(),
                snapA.budgets(), snapA.timestamp(), snapA.metadata(), snapA.version()
        );
        store.save(updatedA);

        // Database/Store version is now updated (2)
        AgentExecutionSnapshot currentInStore = store.find(execId).orElseThrow();
        assertEquals(2, currentInStore.version());

        // Runtime B attempts update using outdated snapshot version 1
        AgentExecutionSnapshot updatedB = new AgentExecutionSnapshot(
                snapB.executionId(), snapB.agentId(), snapB.goal(), snapB.state(), "RUNNING",
                snapB.plan(), snapB.iteration(), snapB.toolCallCount(), snapB.observations(),
                snapB.memoryReferences(), snapB.pendingDecision(), snapB.approvalState(),
                snapB.budgets(), snapB.timestamp(), snapB.metadata(), snapB.version()
        );

        assertThrows(ConcurrentModificationException.class, () -> store.save(updatedB));
    }

    @Test
    public void test8_and_9_ClaimFailureMustNotExecute() {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        AtomicInteger modelCallCount = new AtomicInteger(0);
        AtomicInteger toolCallCount = new AtomicInteger(0);

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            modelCallCount.incrementAndGet();
            return new ChatResponse(ChatMessage.assistant("Done"), new TokenUsage(1, 1, 2), "STOP");
        });

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("counting-tool"),
                "counting tool",
                ToolSchema.empty(),
                c -> {
                    toolCallCount.incrementAndGet();
                    return ToolResult.success("counted");
                }
        ));

        AgentState state = new AgentState(execId, List.of(), null, Map.of("pending_tool_call_id", "call-1"), 0, 0, "WAITING_APPROVAL");
        AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                execId, "agent-1", "test goal", state, "WAITING_FOR_APPROVAL", null, 0, 0, List.of(), List.of(), null, null, null, Instant.now(), Map.of(), 1
        );
        store.save(snapshot);

        // Wrap store to force atomic claim failure when save is invoked during resume
        AgentExecutionStore failingStore = new AgentExecutionStore() {
            @Override
            public void save(AgentExecutionSnapshot snapshotToSave) {
                if ("RUNNING".equals(snapshotToSave.loopState())) {
                    throw new ConcurrentModificationException("Simulated atomic claim collision");
                }
                store.save(snapshotToSave);
            }

            @Override
            public java.util.Optional<AgentExecutionSnapshot> find(String executionId) {
                return store.find(executionId);
            }

            @Override
            public void delete(String executionId) {
                store.delete(executionId);
            }
        };

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .executionStore(failingStore)
                .build();

        // Attempt resume which will fail during atomic claim
        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "admin", "approved"));
        assertThrows(AgentFailure.class, () -> ((ResumableAgentRuntime) agent).resume(execId, input));

        // Verify NO model calls, NO tool calls, NO loop execution occurred
        assertEquals(0, modelCallCount.get(), "Model must not be called when claim fails");
        assertEquals(0, toolCallCount.get(), "Tools must not be called when claim fails");
    }

    @Test
    public void test12_TerminalStateCannotBeResumed() {
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        Agent agent = AgentX.builder().model(model).executionStore(store).build();

        // Terminal COMPLETED state
        AgentState completedState = new AgentState(execId, List.of(), null, Map.of(), 1, 0, "COMPLETED");
        AgentExecutionSnapshot completedSnapshot = new AgentExecutionSnapshot(
                execId, "agent-1", "test goal", completedState, "COMPLETED", null, 1, 0, List.of(), List.of(), null, null, null, Instant.now(), Map.of(), 1
        );
        store.save(completedSnapshot);

        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "admin", "approved"));
        AgentFailure failure = assertThrows(AgentFailure.class, () -> ((ResumableAgentRuntime) agent).resume(execId, input));
        assertTrue(failure.getMessage().contains("Cannot resume execution in status: COMPLETED"));
    }
}
