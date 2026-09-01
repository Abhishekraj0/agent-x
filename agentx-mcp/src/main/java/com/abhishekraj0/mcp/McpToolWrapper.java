package com.abhishekraj0.mcp;

import com.abhishekraj0.api.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adapter mapping a Model Context Protocol tool to AgentX's AgentTool.
 */
public class McpToolWrapper implements AgentTool {

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|key|secret|password|passwd|token|auth|bearer|credentials|private[_-]?key)\\s*[:=]?\\s*[\"']?([^\"'\\s;,]+)[\"']?"
    );

    private final String serverName;
    private final McpSchema.Tool mcpTool;
    private final McpSyncClient client;
    private final ToolMetadata customMetadata;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpToolWrapper(McpSchema.Tool mcpTool, McpSyncClient client) {
        this(null, mcpTool, client, null);
    }

    public McpToolWrapper(String serverName, McpSchema.Tool mcpTool, McpSyncClient client) {
        this(serverName, mcpTool, client, null);
    }

    public McpToolWrapper(String serverName, McpSchema.Tool mcpTool, McpSyncClient client, ToolMetadata customMetadata) {
        this.serverName = serverName;
        this.mcpTool = mcpTool;
        this.client = client;
        this.customMetadata = customMetadata;
    }

    @Override
    public ToolId id() {
        if (serverName != null && !serverName.isBlank()) {
            return new ToolId(serverName + "." + mcpTool.name());
        }
        return new ToolId(mcpTool.name());
    }

    public String rawToolName() {
        return mcpTool.name();
    }

    @Override
    public String description() {
        return mcpTool.description() != null ? mcpTool.description() : "";
    }

    @Override
    public ToolSchema inputSchema() {
        try {
            if (mcpTool.inputSchema() == null) {
                return ToolSchema.empty();
            }
            List<ToolProperty> props = new ArrayList<>();
            if (mcpTool.inputSchema() instanceof Map<?, ?> mapObj) {
                List<String> requiredList = List.of();
                Object req = mapObj.get("required");
                if (req instanceof List<?> list) {
                    requiredList = list.stream().map(Object::toString).toList();
                }
                Object propertiesObj = mapObj.get("properties");
                if (propertiesObj instanceof Map<?, ?> propsMap) {
                    for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                        String propName = entry.getKey().toString();
                        String propType = "string";
                        String propDesc = "";
                        if (entry.getValue() instanceof Map<?, ?> propMeta) {
                            if (propMeta.get("type") != null) {
                                propType = propMeta.get("type").toString();
                            }
                            if (propMeta.get("description") != null) {
                                propDesc = propMeta.get("description").toString();
                            }
                        }
                        boolean isRequired = requiredList.contains(propName);
                        props.add(new ToolProperty(propName, propType, propDesc, isRequired));
                    }
                }
            }
            if (props.isEmpty()) {
                String schemaJson = objectMapper.writeValueAsString(mcpTool.inputSchema());
                props.add(new ToolProperty("schemaJson", "string", schemaJson, false));
            }
            return new ToolSchema("object", props);
        } catch (Exception e) {
            return ToolSchema.empty();
        }
    }

    @Override
    public ToolResult execute(ToolContext context) {
        // Step 1: Check cancellation token
        if (context != null && context.cancellationToken() != null && context.cancellationToken().isCancelled()) {
            return ToolResult.failure("CANCELLED", "MCP tool execution cancelled prior to call", null);
        }

        if (client == null) {
            return ToolResult.failure("MCP_DISCONNECTED", "MCP client is disconnected", null);
        }

        try {
            // Step 2: Call tool on MCP client
            CallToolResult result = client.callTool(new CallToolRequest(mcpTool.name(), context != null ? context.arguments() : Map.of()));

            // Check post-call cancellation
            if (context != null && context.cancellationToken() != null && context.cancellationToken().isCancelled()) {
                return ToolResult.failure("CANCELLED", "MCP tool execution cancelled after call", null);
            }

            if (result == null || result.content() == null || result.content().isEmpty()) {
                return ToolResult.failure("EMPTY_RESPONSE", "Received empty response from MCP Tool: " + id().name(), null);
            }

            if (Boolean.TRUE.equals(result.isError())) {
                String redactedError = redactSecrets(extractTextContent(result));
                String lowerError = redactedError.toLowerCase();
                if (lowerError.contains("timeout") || lowerError.contains("timed out")) {
                    return ToolResult.failure("TOOL_TIMEOUT", "MCP Tool execution timed out: " + redactedError, null);
                }
                return ToolResult.failure("MCP_ERROR", "MCP Tool returned error: " + redactedError, null);
            }

            String outputText = extractTextContent(result);
            String redactedOutput = redactSecrets(outputText);
            return ToolResult.success(redactedOutput);

        } catch (Exception e) {
            String msg = getFullErrorMessage(e);
            String redactedMsg = redactSecrets(msg);
            String lowerMsg = msg.toLowerCase();

            if (lowerMsg.contains("timeout") || lowerMsg.contains("timed out") || isCauseType(e, java.util.concurrent.TimeoutException.class)) {
                return ToolResult.failure("TOOL_TIMEOUT", "MCP Tool execution timed out: " + redactedMsg, e);
            }
            if (msg.contains("closed") || msg.contains("Disconnected") || msg.contains("Pipe closed")) {
                return ToolResult.failure("MCP_DISCONNECTED", "MCP connection failed during tool call: " + redactedMsg, e);
            }
            return ToolResult.failure("MCP_ERROR", "MCP execution error: " + redactedMsg, e);
        }
    }

    private boolean isCauseType(Throwable t, Class<?> targetClass) {
        Throwable curr = t;
        while (curr != null) {
            if (targetClass.isInstance(curr)) return true;
            curr = curr.getCause();
        }
        return false;
    }

    private String getFullErrorMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable curr = e;
        while (curr != null) {
            sb.append(curr.getClass().getName()).append(": ").append(curr.getMessage()).append(" ");
            curr = curr.getCause();
        }
        return sb.toString().trim();
    }

    private String extractTextContent(CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        for (var content : result.content()) {
            if (content instanceof TextContent textContent) {
                sb.append(textContent.text());
            } else if (content != null) {
                sb.append(content.toString());
            }
        }
        return sb.toString();
    }

    private String redactSecrets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return SECRET_PATTERN.matcher(text).replaceAll("$1=[REDACTED]");
    }

    @Override
    public ToolMetadata metadata() {
        if (customMetadata != null) {
            return customMetadata;
        }
        return ToolMetadata.defaultMetadata();
    }
}
