package com.abhishekraj0.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.agent.InMemoryAgentExecutionStore;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.InMemoryIdempotencyManager;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Real Interoperability Test (Phase J).
 * Validates AgentX MCP client adapter over real STDIO transport pipes against an independent subprocess.
 */
public class McpRealProtocolInteroperabilityTest {

    @Test
    public void testRealSubprocessWireInteroperability() {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classPath = System.getProperty("java.class.path");

        List<String> commandArgs = List.of(
                "-cp", classPath,
                "com.abhishekraj0.mcp.McpStandaloneJsonRpcProcess"
        );

        DefaultMcpClient client = new DefaultMcpClient("realInteropServer", javaBin, commandArgs);

        try {
            client.connect();
            assertTrue(client.isConnected());
            assertEquals("2024-11-05", client.protocolVersion());

            List<AgentTool> tools = client.tools();
            assertFalse(tools.isEmpty());

            AgentTool interopEcho = tools.stream()
                    .filter(t -> t.id().name().equals("realInteropServer.interop.echo"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected tool realInteropServer.interop.echo not found"));

            DefaultToolRegistry registry = new DefaultToolRegistry();
            registry.register(interopEcho);

            MockChatModel model = new MockChatModel();
            model.setHandler(req -> {
                boolean hasObs = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("INTEROP_OK"));
                if (hasObs) {
                    return new ChatResponse(ChatMessage.assistant("Received wire response successfully."), new TokenUsage(5, 5, 10), "STOP");
                }
                return new ChatResponse(
                        ChatMessage.assistant(null, List.of(new ToolCall("call-1", "realInteropServer.interop.echo", "{\"message\":\"Hello AgentX Wire Interop\"}"))),
                        new TokenUsage(5, 5, 10),
                        "TOOL_USE"
                );
            });

            Agent agent = AgentX.builder()
                    .model(model)
                    .tools(registry)
                    .executionStore(new InMemoryAgentExecutionStore())
                    .idempotencyManager(new InMemoryIdempotencyManager())
                    .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Received wire response successfully")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                    .build();

            String execId = UUID.randomUUID().toString();
            AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Test real wire interop", execId, AgentOptions.defaultOptions()));

            assertEquals("COMPLETED", response.state().status());
            assertTrue(response.state().history().stream().anyMatch(m -> m.content() != null && m.content().contains("Received wire response successfully.")));

        } finally {
            client.disconnect();
            assertFalse(client.isConnected());
        }
    }
}
