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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UnknownResultRecoveryPolicyTest {

    private InMemoryIdempotencyManager idempotencyManager;
    private InMemoryAgentExecutionStore executionStore;
    private DefaultToolRegistry toolRegistry;
    private AtomicInteger paymentExecutionCount;
    private AtomicInteger queryExecutionCount;

    @BeforeEach
    public void setUp() {
        idempotencyManager = new InMemoryIdempotencyManager();
        executionStore = new InMemoryAgentExecutionStore();
        toolRegistry = new DefaultToolRegistry();
        paymentExecutionCount = new AtomicInteger(0);
        queryExecutionCount = new AtomicInteger(0);

        // Unsafe non-idempotent tool
        toolRegistry.register(new FunctionTool(
                new ToolId("sendPayment"),
                "send payment tool",
                ToolSchema.empty(),
                c -> {
                    paymentExecutionCount.incrementAndGet();
                    return ToolResult.success("payment_success_tx_999");
                },
                new ToolMetadata(RiskLevel.HIGH, false, false, false, false, Duration.ofSeconds(5)) // safeAfterUnknownResult = false
        ));

        // Safe status query tool
        toolRegistry.register(new FunctionTool(
                new ToolId("queryStatus"),
                "query payment status tool",
                ToolSchema.empty(),
                c -> {
                    queryExecutionCount.incrementAndGet();
                    return ToolResult.success("status: SUCCESS");
                },
                new ToolMetadata(RiskLevel.LOW, false, true, true, true, Duration.ofSeconds(5)) // safeAfterUnknownResult = true
        ));
    }

    @Test
    public void testRequireApprovalPolicy_EntersWaitingApprovalStatusForInterruptedNonIdempotentTool() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-payment-req", "sendPayment", "{\"amount\":500}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        String idempotencyKey = "sendPayment_" + "{\"amount\":500}";
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-payment-req", "sendPayment", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> GoalStatus.IN_PROGRESS)
                .build();

        AgentOptions options = new AgentOptions(
                10,
                20,
                Duration.ofMinutes(5),
                0.7,
                Map.of("unknownResultRecoveryPolicy", UnknownResultRecoveryPolicy.REQUIRE_APPROVAL)
        );

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Send $500", execId, options));

        assertEquals("WAITING_APPROVAL", response.state().status());
        assertEquals(0, paymentExecutionCount.get(), "Payment tool must NOT be executed while waiting for approval");
        assertNotNull(response.state().variables().get("pending_unknown_result_tool"));
    }

    @Test
    public void testRetryPolicy_ReExecutesSafeToolWhenConfigured() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasObs = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("status: SUCCESS"));
            if (hasObs) {
                return new ChatResponse(ChatMessage.assistant("Status query done"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-query-retry", "queryStatus", "{}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        String idempotencyKey = "queryStatus_" + "{}";
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-query-retry", "queryStatus", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Status query done")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        AgentOptions options = new AgentOptions(
                10,
                20,
                Duration.ofMinutes(5),
                0.7,
                Map.of("unknownResultRecoveryPolicy", UnknownResultRecoveryPolicy.RETRY)
        );

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Query status", execId, options));

        assertEquals("COMPLETED", response.state().status());
        assertEquals(1, queryExecutionCount.get(), "Query status tool must be re-executed because safeAfterUnknownResult is true");
    }

    @Test
    public void testRetryPolicy_RejectsAutomaticRetryForUnsafeTool() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-payment-retry", "sendPayment", "{\"amount\":500}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        String idempotencyKey = "sendPayment_" + "{\"amount\":500}";
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-payment-retry", "sendPayment", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> GoalStatus.IN_PROGRESS)
                .build();

        AgentOptions options = new AgentOptions(
                10,
                20,
                Duration.ofMinutes(5),
                0.7,
                Map.of("unknownResultRecoveryPolicy", UnknownResultRecoveryPolicy.RETRY)
        );

        AgentFailure failure = assertThrows(AgentFailure.class, () ->
                ((AgentRuntime) agent).execute(new AgentRequest("Send $500", execId, options))
        );

        assertEquals("UNKNOWN_TOOL_RESULT", failure.getCode());
        assertEquals(0, paymentExecutionCount.get(), "Automatic RETRY policy must be rejected for non-idempotent unsafe tools");
    }

    @Test
    public void testOperatorResolution_RetryAuthorizedExecutesUnsafeTool() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasObs = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("payment_success_tx_999"));
            if (hasObs) {
                return new ChatResponse(ChatMessage.assistant("Done authorized retry"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-payment-auth", "sendPayment", "{\"amount\":500}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        String idempotencyKey = "sendPayment_" + "{\"amount\":500}";
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-payment-auth", "sendPayment", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Done authorized retry")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        Map<String, Object> vars = new HashMap<>();
        vars.put("unknown_result_resolution_call-payment-auth", UnknownResultResolution.retryAuthorized("Operator verified payment did not register on provider"));

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Send $500", execId, AgentOptions.defaultOptions(), vars));

        assertEquals("COMPLETED", response.state().status());
        assertEquals(1, paymentExecutionCount.get(), "Payment tool must be executed when operator explicitly provides RETRY_AUTHORIZED resolution");
    }
}
