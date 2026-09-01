package com.abhishekraj0.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.mcp.McpClient;
import com.abhishekraj0.api.security.PermissionDecision;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.agent.DefaultCancellationToken;
import com.abhishekraj0.core.agent.InMemoryAgentExecutionStore;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.InMemoryIdempotencyManager;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive failure matrix test suite for AgentX MCP Integration (Phase I).
 */
public class McpFailureMatrixTest {

    private AgentXMcpTestServer serverA;
    private AgentXMcpTestServer serverB;

    @BeforeEach
    public void setUp() {
        serverA = new AgentXMcpTestServer("serverA");
        serverA.registerTool("echo", "Echo tool", Map.of("type", "object"));
        serverA.registerTool("slowTool", "Slow tool", Map.of("type", "object"));
        serverA.registerTool("secretTool", "Secret tool", Map.of("type", "object"));
        serverA.registerTool("malformedTool", "Malformed response tool", Map.of("type", "object"));
        serverA.registerTool("invalidArgTool", "Required arg tool", Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string")), "required", List.of("id")));

        serverB = new AgentXMcpTestServer("serverB");
        serverB.registerTool("echo", "Echo tool B", Map.of("type", "object"));
    }

    @Test
    public void test1_serverUnavailable() {
        DefaultMcpClient client = new DefaultMcpClient("non-existent-cmd", List.of());
        assertThrows(Throwable.class, client::connect);
        assertFalse(client.isConnected());
    }

    @Test
    public void test2_connectionTimeout() {
        DefaultMcpClient client = serverA.createClient();
        client.disconnect();
        List<AgentTool> tools = client.tools();
        assertTrue(tools.isEmpty());
    }

    @Test
    public void test3_toolTimeout() {
        DefaultMcpClient clientA = serverA.createClient();
        AgentTool slow = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.slowTool")).findFirst().orElseThrow();

        ToolContext context = new ToolContext("session-1", Map.of());
        ToolResult result = slow.execute(context);

        assertFalse(result.success());
        assertEquals("TOOL_TIMEOUT", result.error().code());
    }

    @Test
    public void test4_malformedResponse() {
        DefaultMcpClient clientA = serverA.createClient();
        AgentTool tool = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.malformedTool")).findFirst().orElseThrow();

        ToolContext context = new ToolContext("session-1", Map.of());
        ToolResult result = tool.execute(context);

        assertFalse(result.success());
        assertTrue(result.error().code().equals("EMPTY_RESPONSE") || result.error().code().equals("MCP_ERROR"));
    }

    @Test
    public void test5_unknownTool() {
        DefaultMcpClient clientA = serverA.createClient();
        List<AgentTool> tools = clientA.tools();
        assertFalse(tools.stream().anyMatch(t -> t.id().name().equals("serverA.nonExistentTool")));
    }

    @Test
    public void test6_invalidArguments() {
        DefaultMcpClient clientA = serverA.createClient();
        AgentTool tool = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.invalidArgTool")).findFirst().orElseThrow();

        ToolContext context = new ToolContext("session-1", Map.of());
        ToolResult result = tool.execute(context);

        assertFalse(result.success());
        assertEquals("MCP_ERROR", result.error().code());
    }

    @Test
    public void test7_serverDisconnect() {
        DefaultMcpClient clientA = serverA.createClient();
        AgentTool echo = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.echo")).findFirst().orElseThrow();

        serverA.simulateDisconnect();
        ToolContext context = new ToolContext("session-1", Map.of());
        ToolResult result = echo.execute(context);

        assertFalse(result.success());
        assertEquals("MCP_DISCONNECTED", result.error().code());
    }

    @Test
    public void test8_reconnectFailure() {
        DefaultMcpClient client = new DefaultMcpClient("non-existent-binary-cmd", List.of());
        assertThrows(Throwable.class, client::reconnect);
        assertFalse(client.isConnected());
    }

    @Test
    public void test9_cancellation() {
        DefaultMcpClient clientA = serverA.createClient();
        AgentTool echo = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.echo")).findFirst().orElseThrow();

        DefaultCancellationToken cancellationToken = new DefaultCancellationToken();
        cancellationToken.cancel();

        ToolContext context = new ToolContext("session-1", Map.of(), Map.of(), cancellationToken);
        ToolResult result = echo.execute(context);

        assertFalse(result.success());
        assertEquals("CANCELLED", result.error().code());
    }

    @Test
    public void test10_lostResponseUnknownResult() {
        AgentTool nonIdempotentMcpTool = new McpToolWrapper(
                "serverA",
                new McpSchema.Tool("payment.charge", "payment.charge", "Charge payment", Map.of("type", "object"), null, null, null),
                null,
                new ToolMetadata(RiskLevel.HIGH, false, false, false, false, Duration.ofSeconds(5))
        );

        ToolContext context = new ToolContext("session-1", Map.of("amount", 100));
        ToolResult result = nonIdempotentMcpTool.execute(context);

        assertFalse(result.success());
        assertEquals("MCP_DISCONNECTED", result.error().code());
    }

    @Test
    public void test11_unauthorizedToolPermissionDenied() {
        DefaultMcpClient clientA = serverA.createClient();
        DefaultToolRegistry registry = new DefaultToolRegistry();
        clientA.tools().forEach(registry::register);

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant(null, List.of(new ToolCall("call-1", "serverA.echo", "{\"msg\":\"hello\"}"))),
                new TokenUsage(5, 5, 10),
                "TOOL_USE"
        ));

        PermissionManager permManager = (action, context) -> {
            if (action != null && "TOOL_CALL".equals(action.type())) {
                return PermissionDecision.deny("MCP Tool execution unauthorized by policy");
            }
            return PermissionDecision.allow();
        };

        Agent agent = AgentX.builder()
                .model(model)
                .tools(registry)
                .permissionManager(permManager)
                .executionStore(new InMemoryAgentExecutionStore())
                .idempotencyManager(new InMemoryIdempotencyManager())
                .goalEvaluator(s -> GoalStatus.IN_PROGRESS)
                .build();

        String execId = UUID.randomUUID().toString();
        AgentResponse resp = ((AgentRuntime) agent).execute(new AgentRequest("Execute echo", execId, AgentOptions.defaultOptions()));

        assertNotNull(resp.state().status());
        assertNotEquals("COMPLETED", resp.state().status());
    }

    @Test
    public void test12_approvalWorkflow() {
        AgentTool mcpApprovalTool = new McpToolWrapper(
                "serverA",
                new McpSchema.Tool("serverA.adminAction", "serverA.adminAction", "Admin Action", Map.of("type", "object"), null, null, null),
                null,
                new ToolMetadata(RiskLevel.HIGH, true, true, false, false, Duration.ofSeconds(5))
        );

        assertNotNull(mcpApprovalTool.metadata());
        assertEquals(RiskLevel.HIGH, mcpApprovalTool.metadata().riskLevel());
        assertTrue(mcpApprovalTool.metadata().requiresApproval());
    }

    @Test
    public void test13_secretInOutputRedacted() {
        DefaultMcpClient clientA = serverA.createClient();
        AgentTool secretTool = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.secretTool")).findFirst().orElseThrow();

        ToolContext context = new ToolContext("session-1", Map.of("apiKey", "TOP_SECRET_PASSPHRASE_12345"));
        ToolResult result = secretTool.execute(context);

        assertTrue(result.success());
        assertFalse(result.output().contains("TOP_SECRET_PASSPHRASE_12345"));
        assertTrue(result.output().contains("[REDACTED]"));
    }

    @Test
    public void test14_duplicateServerToolNamespacing() {
        DefaultMcpClient clientA = serverA.createClient();
        DefaultMcpClient clientB = serverB.createClient();

        AgentTool echoA = clientA.tools().stream().filter(t -> t.id().name().equals("serverA.echo")).findFirst().orElseThrow();
        AgentTool echoB = clientB.tools().stream().filter(t -> t.id().name().equals("serverB.echo")).findFirst().orElseThrow();

        assertNotEquals(echoA.id().name(), echoB.id().name());
        assertEquals("serverA.echo", echoA.id().name());
        assertEquals("serverB.echo", echoB.id().name());
    }

    @Test
    public void test15_dynamicToolRemoval() {
        DefaultMcpClient clientA = serverA.createClient();
        int initialCount = clientA.tools().size();

        serverA.unregisterTool("malformedTool");

        List<AgentTool> updatedTools = clientA.tools();
        assertEquals(initialCount - 1, updatedTools.size());
        assertFalse(updatedTools.stream().anyMatch(t -> t.id().name().equals("serverA.malformedTool")));
    }

    @Test
    public void test16_dynamicToolAddition() {
        DefaultMcpClient clientA = serverA.createClient();
        assertFalse(clientA.tools().stream().anyMatch(t -> t.id().name().equals("serverA.brandNewTool")));

        serverA.registerTool("brandNewTool", "Newly registered tool", Map.of("type", "object"));

        assertTrue(clientA.tools().stream().anyMatch(t -> t.id().name().equals("serverA.brandNewTool")));
    }
}
