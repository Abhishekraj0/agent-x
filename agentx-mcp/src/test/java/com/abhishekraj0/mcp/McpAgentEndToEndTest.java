package com.abhishekraj0.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.PermissionDecision;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.agent.InMemoryAgentExecutionStore;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.InMemoryIdempotencyManager;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McpAgentEndToEndTest {

    private AgentXMcpTestServer mcpServer;
    private DefaultToolRegistry toolRegistry;
    private InMemoryIdempotencyManager idempotencyManager;
    private InMemoryAgentExecutionStore executionStore;

    @BeforeEach
    public void setUp() {
        mcpServer = new AgentXMcpTestServer("mcp");
        mcpServer.registerTool("payments.failed", "Get failed payments list", Map.of("type", "object"));
        mcpServer.registerTool("calculator.add", "Add numbers", Map.of("type", "object"));
        mcpServer.registerToolWithMetadata(
                "deleteCustomer",
                "Delete customer record",
                Map.of("type", "object", "properties", Map.of("customerId", Map.of("type", "string"))),
                new ToolMetadata(RiskLevel.HIGH, false, false, false, false, Duration.ofSeconds(5)) // non-idempotent, high-risk
        );

        toolRegistry = new DefaultToolRegistry();
        idempotencyManager = new InMemoryIdempotencyManager();
        executionStore = new InMemoryAgentExecutionStore();

        DefaultMcpClient mcpClient = mcpServer.createClient();
        mcpClient.tools().forEach(toolRegistry::register);
    }

    @Test
    public void testMcpAgentEndToEndCalculationLoop() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasFailedPayments = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("tx_101"));
            boolean hasAddResult = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("200.0"));

            if (hasAddResult) {
                return new ChatResponse(ChatMessage.assistant("The total of today's failed payments is $200.0."), new TokenUsage(10, 10, 20), "STOP");
            }
            if (hasFailedPayments) {
                return new ChatResponse(
                        ChatMessage.assistant(null, List.of(new ToolCall("call-add-001", "mcp.calculator.add", "{\"a\":50,\"b\":150}"))),
                        new TokenUsage(5, 5, 10),
                        "TOOL_USE"
                );
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-pmt-001", "mcp.payments.failed", "{}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("total of today's failed payments is $200.0")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Calculate total of today's failed payments", execId, AgentOptions.defaultOptions()));

        assertEquals("COMPLETED", response.state().status());
        assertTrue(response.state().history().stream().anyMatch(m -> m.content() != null && m.content().contains("$200.0")));
        assertEquals(2, mcpServer.toolCallCount(), "MCP tools payments.failed and calculator.add must be invoked");
    }

    @Test
    public void testHighRiskMcpToolApprovalWorkflow() {
        String execId = UUID.randomUUID().toString();

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean hasObs = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("deleted successfully"));
            if (hasObs) {
                return new ChatResponse(ChatMessage.assistant("Customer CUST_999 deleted"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-del-001", "mcp.deleteCustomer", "{\"customerId\":\"CUST_999\"}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        PermissionManager permManager = (action, context) -> {
            if (action != null && "TOOL_CALL".equals(action.type()) && action.details() != null && "mcp.deleteCustomer".equals(action.details().get("toolName"))) {
                return PermissionDecision.requireApproval("High-risk MCP tool deleteCustomer requires approval");
            }
            return PermissionDecision.allow();
        };

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permManager)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("deleted")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        // Execution 1: Enters WAITING_APPROVAL
        AgentResponse resp1 = ((AgentRuntime) agent).execute(new AgentRequest("Delete customer CUST_999", execId, AgentOptions.defaultOptions()));

        assertEquals("WAITING_APPROVAL", resp1.state().status());
        assertEquals(0, mcpServer.toolCallCount(), "High-risk MCP tool must NOT execute prior to approval");

        // Execution 2: Approved and resumed
        PermissionManager approveManager = (action, context) -> PermissionDecision.allow();

        Agent agentApproved = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(approveManager)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("deleted")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        AgentResponse resp2 = ((AgentRuntime) agentApproved).execute(new AgentRequest("Delete customer CUST_999", execId, AgentOptions.defaultOptions()));

        assertEquals("COMPLETED", resp2.state().status());
        assertEquals(1, mcpServer.toolCallCount(), "High-risk MCP tool must execute once approved");
    }

    @Test
    public void testMcpToolIdempotencyCrashRecoverySafety() {
        String execId = UUID.randomUUID().toString();
        String idempotencyKey = "mcp.deleteCustomer_" + "{\"customerId\":\"CUST_999\"}";

        // Record PENDING state simulating crash during tool execution
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-del-001", "mcp.deleteCustomer", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);

        MockChatModel model = new MockChatModel();
        model.setHandler(r -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-del-001", "mcp.deleteCustomer", "{\"customerId\":\"CUST_999\"}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> GoalStatus.IN_PROGRESS)
                .build();

        // Execute recovery -> must reject automatic RETRY on non-idempotent unsafe MCP tool
        AgentFailure failure = assertThrows(AgentFailure.class, () ->
                ((AgentRuntime) agent).execute(new AgentRequest("Delete customer CUST_999", execId, AgentOptions.defaultOptions()))
        );

        assertEquals("UNKNOWN_TOOL_RESULT", failure.getCode());
        assertEquals(0, mcpServer.toolCallCount(), "AgentX MUST NOT execute non-idempotent MCP tool after unknown outcome!");
    }
}
