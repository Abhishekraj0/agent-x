package com.abhishekraj0.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Independent process simulating a strict MCP 2026-07-28 modern server over STDIO pipes.
 * Rejects legacy 'initialize' handshakes with JSON-RPC error -32601.
 * Supports 'server/discover', 'tools/list', and 'tools/call' statelessly.
 */
public class Mcp2026ModernServerProcess {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    JsonNode req = mapper.readTree(line);
                    String method = req.has("method") ? req.get("method").asText() : "";
                    JsonNode idNode = req.get("id");

                    if ("initialize".equals(method)) {
                        // REJECT LEGACY INITIALIZE HANDSHAKE
                        ObjectNode errResp = mapper.createObjectNode();
                        errResp.put("jsonrpc", "2.0");
                        if (idNode != null) errResp.set("id", idNode);
                        ObjectNode error = errResp.putObject("error");
                        error.put("code", -32601);
                        error.put("message", "Method 'initialize' not found: MCP 2026-07-28 era servers do not use legacy initialize handshake.");
                        writer.println(mapper.writeValueAsString(errResp));
                        writer.flush();
                    } else if ("server/discover".equals(method)) {
                        // 2026-07-28 MODERN DISCOVERY RESPONSE
                        ObjectNode resp = mapper.createObjectNode();
                        resp.put("jsonrpc", "2.0");
                        if (idNode != null) resp.set("id", idNode);

                        ObjectNode result = resp.putObject("result");
                        result.put("protocolVersion", "2026-07-28");
                        ObjectNode server = result.putObject("server");
                        server.put("name", "modern-2026-server");
                        server.put("version", "1.0.0");
                        ObjectNode caps = result.putObject("capabilities");
                        caps.putObject("tools");

                        writer.println(mapper.writeValueAsString(resp));
                        writer.flush();
                    } else if ("tools/list".equals(method)) {
                        ObjectNode resp = mapper.createObjectNode();
                        resp.put("jsonrpc", "2.0");
                        if (idNode != null) resp.set("id", idNode);

                        ObjectNode result = resp.putObject("result");
                        var tools = result.putArray("tools");
                        ObjectNode tool = tools.addObject();
                        tool.put("name", "modernCalculate");
                        tool.put("description", "Modern 2026 calculator tool");
                        ObjectNode schema = tool.putObject("inputSchema");
                        schema.put("type", "object");

                        writer.println(mapper.writeValueAsString(resp));
                        writer.flush();
                    } else if ("tools/call".equals(method)) {
                        ObjectNode resp = mapper.createObjectNode();
                        resp.put("jsonrpc", "2.0");
                        if (idNode != null) resp.set("id", idNode);

                        ObjectNode result = resp.putObject("result");
                        var content = result.putArray("content");
                        ObjectNode textContent = content.addObject();
                        textContent.put("type", "text");
                        textContent.put("text", "Modern 2026 tool execution successful");
                        result.put("isError", false);

                        writer.println(mapper.writeValueAsString(resp));
                        writer.flush();
                    } else {
                        ObjectNode errResp = mapper.createObjectNode();
                        errResp.put("jsonrpc", "2.0");
                        if (idNode != null) errResp.set("id", idNode);
                        ObjectNode error = errResp.putObject("error");
                        error.put("code", -32601);
                        error.put("message", "Method not found: " + method);
                        writer.println(mapper.writeValueAsString(errResp));
                        writer.flush();
                    }
                } catch (Exception e) {
                    ObjectNode errResp = mapper.createObjectNode();
                    errResp.put("jsonrpc", "2.0");
                    ObjectNode error = errResp.putObject("error");
                    error.put("code", -32700);
                    error.put("message", "Parse error: " + e.getMessage());
                    writer.println(mapper.writeValueAsString(errResp));
                    writer.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Mcp2026ModernServerProcess terminated: " + e.getMessage());
        }
    }
}
