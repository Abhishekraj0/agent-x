package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import com.abhishekraj0.core.tool.InMemoryIdempotencyManager;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IdempotencyFailureScenarioTest {

    private InMemoryIdempotencyManager idempotencyManager;
    private InMemoryAgentExecutionStore executionStore;
    private DefaultToolRegistry toolRegistry;
    private AtomicInteger paymentExecutionCount;

    @BeforeEach
    public void setUp() {
        idempotencyManager = new InMemoryIdempotencyManager();
        executionStore = new InMemoryAgentExecutionStore();
        toolRegistry = new DefaultToolRegistry();
        paymentExecutionCount = new AtomicInteger(0);

        // Register a non-idempotent tool (sendPayment)
        toolRegistry.register(new FunctionTool(
                new ToolId("sendPayment"),
                "send payment tool",
                ToolSchema.empty(),
                c -> {
                    paymentExecutionCount.incrementAndGet();
                    return ToolResult.success("payment_success_tx_123");
                },
                new ToolMetadata(RiskLevel.HIGH, false, false, false, Duration.ofSeconds(5)) // idempotent = false
        ));
    }

    @Test
    public void test1_NonIdempotentToolInterrupted_FailSafePolicyPreventsDuplicatePayment() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-payment-1", "sendPayment", "{\"amount\":100}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        // Simulate crash scenario: tool execution request was initiated & recorded as PENDING in idempotency store
        String idempotencyKey = "sendPayment_" + "{\"amount\":100}";
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-payment-1", "sendPayment", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> GoalStatus.IN_PROGRESS)
                .build();

        // Execution attempt must fail safely and NOT blindly execute sendPayment again
        AgentFailure failure = assertThrows(AgentFailure.class, () ->
                ((AgentRuntime) agent).execute(new AgentRequest("Send $100", execId, AgentOptions.defaultOptions()))
        );

        assertEquals("UNKNOWN_TOOL_RESULT", failure.getCode());
        assertEquals(0, paymentExecutionCount.get(), "AgentX MUST NOT blindly execute non-idempotent tool again when result is UNKNOWN");
    }

    @Test
    public void test2_CompletedIdempotentResult_ReusesCachedResultWithoutReExecution() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasObs = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("payment_success_tx_123"));
            if (hasObs) {
                return new ChatResponse(ChatMessage.assistant("Payment already processed"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-payment-1", "sendPayment", "{\"amount\":100}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        // Pre-record COMPLETED idempotency result
        String idempotencyKey = "sendPayment_" + "{\"amount\":100}";
        ToolExecutionResult cachedResult = new ToolExecutionResult(
                execId, "call-payment-1", "sendPayment", idempotencyKey, true, "payment_success_tx_123", null, java.time.Instant.now(), "COMPLETED"
        );
        idempotencyManager.record(cachedResult);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Payment already processed")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Send $100", execId, AgentOptions.defaultOptions()));

        assertEquals("COMPLETED", response.state().status());
        assertEquals(0, paymentExecutionCount.get(), "Cached result must be reused without invoking payment tool");
    }

    @Test
    public void test3_IdempotentToolInterrupted_AllowsSafeReExecution() {
        String execId = UUID.randomUUID().toString();
        AtomicInteger readBalanceCount = new AtomicInteger(0);

        toolRegistry.register(new FunctionTool(
                new ToolId("readBalance"),
                "read balance tool",
                ToolSchema.empty(),
                c -> {
                    readBalanceCount.incrementAndGet();
                    return ToolResult.success("balance: $500");
                },
                new ToolMetadata(RiskLevel.LOW, false, true, true, Duration.ofSeconds(5)) // idempotent = true
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasObs = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("balance: $500"));
            if (hasObs) {
                return new ChatResponse(ChatMessage.assistant("Done balance check"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-bal-1", "readBalance", "{}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        // Pre-record PENDING state for idempotent tool
        String idempotencyKey = "readBalance_" + "{}";
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-bal-1", "readBalance", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Done balance check")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Check balance", execId, AgentOptions.defaultOptions()));

        assertEquals("COMPLETED", response.state().status());
        assertEquals(1, readBalanceCount.get(), "Idempotent tool should be safely re-executed upon restart");
    }
}
