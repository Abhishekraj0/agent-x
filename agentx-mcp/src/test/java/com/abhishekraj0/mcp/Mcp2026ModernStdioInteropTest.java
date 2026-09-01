package com.abhishekraj0.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Phase D & Phase E: Adversarial certification test for MCP 2026-07-28 wire protocol compliance.
 * Proves actual SDK 2.0.1 behavior against modern vs legacy protocol expectations.
 */
public class Mcp2026ModernStdioInteropTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSDK201RejectsModernServerWithoutLegacyInitializeHandshake() {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");

        // Attempt connecting DefaultMcpClient to a modern MCP 2026-07-28 server process
        DefaultMcpClient client = new DefaultMcpClient(
                "modern-2026",
                javaBin,
                List.of("-cp", classpath, "com.abhishekraj0.mcp.Mcp2026ModernServerProcess")
        );

        // Expect connection failure because DefaultMcpClient / SDK 2.0.1 sends legacy 'initialize'
        // which modern 2026-07-28 server process rejects with -32601.
        Exception exception = assertThrows(RuntimeException.class, client::connect);
        assertTrue(exception.getMessage().contains("MCP connection failure") || exception.getMessage().contains("initialize"));
        assertFalse(client.isConnected());
    }

    @Test
    public void testDirectWireModern2026DiscoveryAndExecutionWithoutInitializeHandshake() throws Exception {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, "com.abhishekraj0.mcp.Mcp2026ModernServerProcess");
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(process.getOutputStream(), true, StandardCharsets.UTF_8)) {

            // Step 1: Send modern 'server/discover' request without legacy initialize handshake
            ObjectNode discoverReq = mapper.createObjectNode();
            discoverReq.put("jsonrpc", "2.0");
            discoverReq.put("id", "req-disc-1");
            discoverReq.put("method", "server/discover");

            writer.println(mapper.writeValueAsString(discoverReq));
            writer.flush();

            String line1 = reader.readLine();
            assertNotNull(line1, "Modern server response line 1 should not be null");
            JsonNode discoverResp = mapper.readTree(line1);

            assertEquals("req-disc-1", discoverResp.get("id").asText());
            assertEquals("2026-07-28", discoverResp.get("result").get("protocolVersion").asText());
            assertEquals("modern-2026-server", discoverResp.get("result").get("server").get("name").asText());

            // Step 2: Send stateless 'tools/list' request without session ID or prior initialize
            ObjectNode listReq = mapper.createObjectNode();
            listReq.put("jsonrpc", "2.0");
            listReq.put("id", "req-tools-1");
            listReq.put("method", "tools/list");

            writer.println(mapper.writeValueAsString(listReq));
            writer.flush();

            String line2 = reader.readLine();
            assertNotNull(line2, "Modern server response line 2 should not be null");
            JsonNode listResp = mapper.readTree(line2);

            assertEquals("req-tools-1", listResp.get("id").asText());
            assertTrue(listResp.get("result").get("tools").isArray());
            assertEquals("modernCalculate", listResp.get("result").get("tools").get(0).get("name").asText());

            // Step 3: Send stateless 'tools/call' request
            ObjectNode callReq = mapper.createObjectNode();
            callReq.put("jsonrpc", "2.0");
            callReq.put("id", "req-call-1");
            callReq.put("method", "tools/call");
            ObjectNode params = callReq.putObject("params");
            params.put("name", "modernCalculate");
            params.putObject("arguments");

            writer.println(mapper.writeValueAsString(callReq));
            writer.flush();

            String line3 = reader.readLine();
            assertNotNull(line3, "Modern server response line 3 should not be null");
            JsonNode callResp = mapper.readTree(line3);

            assertEquals("req-call-1", callResp.get("id").asText());
            assertFalse(callResp.get("result").get("isError").asBoolean());
            assertEquals("Modern 2026 tool execution successful", callResp.get("result").get("content").get(0).get("text").asText());
        } finally {
            process.destroyForcibly();
        }
    }
}
