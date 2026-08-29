package com.agentx.mcp;

import com.agentx.api.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Adapter mapping a Model Context Protocol tool to AgentX's AgentTool.
 */
public class McpToolWrapper implements AgentTool {

    private final io.modelcontextprotocol.spec.McpSchema.Tool mcpTool;
    private final McpSyncClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpToolWrapper(io.modelcontextprotocol.spec.McpSchema.Tool mcpTool, McpSyncClient client) {
        this.mcpTool = mcpTool;
        this.client = client;
    }

    @Override
    public ToolId id() {
        return new ToolId(mcpTool.name());
    }

    @Override
    public String description() {
        return mcpTool.description();
    }

    @Override
    public ToolSchema inputSchema() {
        try {
            String schemaJson = objectMapper.writeValueAsString(mcpTool.inputSchema());
            return new ToolSchema(schemaJson, java.util.List.of());
        } catch (Exception e) {
            return ToolSchema.empty();
        }
    }

    @Override
    public ToolResult execute(ToolContext context) {
        try {
            CallToolResult result = client.callTool(new CallToolRequest(mcpTool.name(), context.arguments()));
            if (result == null || result.content() == null || result.content().isEmpty()) {
                return ToolResult.failure("EMPTY_RESPONSE", "Received empty response from MCP Tool", null);
            }
            StringBuilder output = new StringBuilder();
            for (var content : result.content()) {
                if (content instanceof TextContent textContent) {
                    output.append(textContent.text());
                }
            }
            return ToolResult.success(output.toString());
        } catch (Exception e) {
            return ToolResult.failure("MCP_ERROR", e.getMessage(), e);
        }
    }

    @Override
    public ToolMetadata metadata() {
        return ToolMetadata.defaultMetadata();
    }
}
