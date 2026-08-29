package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.ApprovalProvider;
import com.abhishekraj0.api.security.ApprovalResult;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import com.abhishekraj0.core.tool.InMemoryIdempotencyManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DurableExecutionReliabilityTest {

    @Test
    public void testCrashAndDurableResumeReliability() {
        String executionId = UUID.randomUUID().toString();
        InMemoryAgentExecutionStore store = new InMemoryAgentExecutionStore();
        DefaultCheckpointManager checkpointManager = new DefaultCheckpointManager();
        InMemoryIdempotencyManager idempotencyManager = new InMemoryIdempotencyManager();

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        AtomicInteger toolCallCount = new AtomicInteger(0);

        ToolId toolId = new ToolId("secure_payment");
        ToolMetadata metadataObj = new ToolMetadata(
                RiskLevel.HIGH,
                true, // requiresApproval
                false, // readOnly
                false, // idempotent
                Duration.ofSeconds(30)
        );

        AgentTool paymentTool = new FunctionTool(
                toolId,
                "Requires approval before sending payment",
                ToolSchema.empty(),
                context -> {
                    toolCallCount.incrementAndGet();
                    return ToolResult.success("Payment of $100 succeeded");
                },
                metadataObj
        );
        toolRegistry.register(paymentTool);

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            List<ChatMessage> history = request.messages();
            boolean hasToolResponse = history.stream()
                    .anyMatch(msg -> msg.role() == ChatMessageRole.TOOL);

            if (!hasToolResponse) {
                ToolCall call = new ToolCall("pay-1", "secure_payment", "{}");
                return new ChatResponse(
                        ChatMessage.assistant(null, List.of(call)),
                        new TokenUsage(5, 5, 10),
                        "TOOL_CALL"
                );
            } else {
                return new ChatResponse(
                        ChatMessage.assistant("The payment is complete and verified."),
                        new TokenUsage(10, 5, 15),
                        "STOP"
                );
            }
        });

        // Set up PermissionManager to require approval
        PermissionManager permissionManager = (action, context) -> 
                new com.abhishekraj0.api.security.PermissionDecision(
                        com.abhishekraj0.api.security.PermissionStatus.REQUIRE_APPROVAL, 
                        "Payment requires human verification"
                );

        // 1. Initial build and run of the Agent
        Agent agentInstance1 = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .checkpointManager(checkpointManager)
                .idempotencyManager(idempotencyManager)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("complete and verified"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        AgentRequest request = new AgentRequest("Pay $100 to merchant", executionId, AgentOptions.defaultOptions());
        AgentResponse response1 = agentInstance1.run(request);

        // Verify that the run paused synchronously and returned waiting for approval
        assertNotNull(response1);
        assertEquals("WAITING_APPROVAL", response1.state().status());
        assertEquals(0, toolCallCount.get(), "Tool should not be called yet because it is waiting for approval");

        // Verify that snapshot has been saved to the store
        Optional<AgentExecutionSnapshot> savedSnapshot = store.find(executionId);
        assertTrue(savedSnapshot.isPresent());
        assertEquals("WAITING_FOR_APPROVAL", savedSnapshot.get().loopState());
        assertNotNull(savedSnapshot.get().pendingDecision());

        // 2. MOCK A CRASH: Completely discard agentInstance1 and memory.
        // We will build a completely new Agent instance 2 with the same store
        Agent agentInstance2 = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store)
                .checkpointManager(checkpointManager)
                .idempotencyManager(idempotencyManager)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("complete and verified"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        // 3. Resume the execution with approval granted
        ResumeInput resumeInput = ResumeInput.ofApproval(new ApprovalResult(true, "Admin", "Payment approved by Admin"));

        // Call resume on the new instance
        assertTrue(agentInstance2 instanceof ResumableAgentRuntime);
        AgentResponse response2 = ((ResumableAgentRuntime) agentInstance2).resume(executionId, resumeInput);

        // Verify that the agent completed successfully after resuming
        assertNotNull(response2);
        assertEquals("COMPLETED", response2.state().status());
        assertEquals(1, toolCallCount.get(), "Tool should have been called exactly once upon resuming");
        assertTrue(response2.output().contains("complete and verified"));

        // Verify that the execution store is updated with the completed state
        Optional<AgentExecutionSnapshot> finalSnapshot = store.find(executionId);
        assertTrue(finalSnapshot.isPresent());
        assertEquals("COMPLETED", finalSnapshot.get().state().status());
    }
}
