package com.agentx.mcp;

import com.agentx.api.mcp.McpClient;
import com.agentx.api.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of McpClient utilizing the Model Context Protocol sync client.
 */
public class DefaultMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClient.class);

    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private McpSyncClient syncClient;

    public DefaultMcpClient(String command, List<String> args) {
        this(command, args, Map.of());
    }

    public DefaultMcpClient(String command, List<String> args, Map<String, String> env) {
        this.command = command;
        this.args = args;
        this.env = env;
    }

    @Override
    public void connect() {
        if (syncClient != null) {
            return;
        }
        try {
            ServerParameters params = ServerParameters.builder(command)
                    .args(args)
                    .env(env)
                    .build();

            JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
            StdioClientTransport transport = new StdioClientTransport(params, jsonMapper);

            this.syncClient = io.modelcontextprotocol.client.McpClient.sync(transport)
                    .capabilities(ClientCapabilities.builder()
                            .roots(true)
                            .sampling()
                            .build())
                    .build();

            this.syncClient.initialize();
            log.info("Successfully connected to MCP Server running: {}", command);
        } catch (Exception e) {
            log.error("Failed to connect to MCP Server: {}", e.getMessage(), e);
            throw new RuntimeException("MCP connection failure", e);
        }
    }

    @Override
    public void disconnect() {
        if (syncClient != null) {
            try {
                syncClient.close();
            } catch (Exception e) {
                log.warn("Error closing MCP Sync Client: {}", e.getMessage());
            } finally {
                syncClient = null;
            }
        }
    }

    @Override
    public List<AgentTool> tools() {
        if (syncClient == null) {
            connect();
        }
        try {
            ListToolsResult toolsResult = syncClient.listTools();
            if (toolsResult == null || toolsResult.tools() == null) {
                return List.of();
            }
            return toolsResult.tools().stream()
                    .map(mcpTool -> new McpToolWrapper(mcpTool, syncClient))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list tools from MCP server", e);
            return List.of();
        }
    }
}
