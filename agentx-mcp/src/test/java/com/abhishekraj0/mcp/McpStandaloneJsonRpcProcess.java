package com.abhishekraj0.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Independent, standalone MCP JSON-RPC 2.0 server process for wire interoperability testing (Phase J).
 * Communicates strictly over standard I/O (STDIO) without importing AgentX runtime internals.
 */
public class McpStandaloneJsonRpcProcess {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                JsonNode request = mapper.readTree(line);
                String method = request.path("method").asText();
                JsonNode idNode = request.path("id");

                if ("initialize".equals(method)) {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("jsonrpc", "2.0");
                    if (!idNode.isMissingNode()) {
                        response.set("id", idNode);
                    }
                    ObjectNode result = mapper.createObjectNode();
                    result.put("protocolVersion", "2024-11-05");
                    result.set("capabilities", mapper.createObjectNode().set("tools", mapper.createObjectNode()));
                    result.set("serverInfo", mapper.createObjectNode().put("name", "StandaloneInteropServer").put("version", "1.0.0"));
                    response.set("result", result);

                    writer.println(mapper.writeValueAsString(response));
                } else if ("notifications/initialized".equals(method)) {
                    // Notification, no response required
                } else if ("tools/list".equals(method)) {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("jsonrpc", "2.0");
                    if (!idNode.isMissingNode()) {
                        response.set("id", idNode);
                    }

                    ObjectNode tool1 = mapper.createObjectNode();
                    tool1.put("name", "interop.echo");
                    tool1.put("description", "Standalone wire echo tool");
                    ObjectNode schema = mapper.createObjectNode();
                    schema.put("type", "object");
                    ObjectNode props = mapper.createObjectNode();
                    props.set("message", mapper.createObjectNode().put("type", "string").put("description", "Message to echo"));
                    schema.set("properties", props);
                    tool1.set("inputSchema", schema);

                    ObjectNode result = mapper.createObjectNode();
                    result.set("tools", mapper.createArrayNode().add(tool1));
                    response.set("result", result);

                    writer.println(mapper.writeValueAsString(response));
                } else if ("tools/call".equals(method)) {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("jsonrpc", "2.0");
                    if (!idNode.isMissingNode()) {
                        response.set("id", idNode);
                    }

                    JsonNode params = request.path("params");
                    String toolName = params.path("name").asText();
                    String msg = params.path("arguments").path("message").asText("default-echo");

                    ObjectNode result = mapper.createObjectNode();
                    result.put("isError", false);

                    ObjectNode textContent = mapper.createObjectNode();
                    textContent.put("type", "text");
                    textContent.put("text", "INTEROP_OK: " + msg);

                    result.set("content", mapper.createArrayNode().add(textContent));
                    response.set("result", result);

                    writer.println(mapper.writeValueAsString(response));
                }
            } catch (Exception e) {
                // Ignore parse errors in process loop
            }
        }
    }
}
