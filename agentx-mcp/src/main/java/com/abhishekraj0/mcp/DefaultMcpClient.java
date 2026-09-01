package com.abhishekraj0.mcp;

import com.abhishekraj0.api.mcp.McpClient;
import com.abhishekraj0.api.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of McpClient utilizing the Model Context Protocol sync client.
 */
public class DefaultMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClient.class);

    private final String serverName;
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private final McpClientTransport customTransport;
    private final Function<McpSchema.Tool, ToolMetadata> metadataProvider;
    private final String activeProtocolVersion;
    private McpSyncClient syncClient;
    private volatile boolean connected = false;

    public DefaultMcpClient(String command, List<String> args) {
        this(null, command, args, Map.of(), null);
    }

    public DefaultMcpClient(String command, List<String> args, Map<String, String> env) {
        this(null, command, args, env, null);
    }

    public DefaultMcpClient(String serverName, String command, List<String> args) {
        this(serverName, command, args, Map.of(), null);
    }

    public DefaultMcpClient(String serverName, String command, List<String> args, Map<String, String> env) {
        this(serverName, command, args, env, null);
    }

    public DefaultMcpClient(String serverName, String command, List<String> args, Map<String, String> env, Function<McpSchema.Tool, ToolMetadata> metadataProvider) {
        this.serverName = serverName;
        this.command = command;
        this.args = args;
        this.env = env;
        this.customTransport = null;
        this.metadataProvider = metadataProvider;
        this.activeProtocolVersion = "2024-11-05";
    }

    public DefaultMcpClient(McpClientTransport customTransport) {
        this(null, customTransport, null);
    }

    public DefaultMcpClient(String serverName, McpClientTransport customTransport, Function<McpSchema.Tool, ToolMetadata> metadataProvider) {
        this.serverName = serverName;
        this.command = null;
        this.args = List.of();
        this.env = Map.of();
        this.customTransport = customTransport;
        this.metadataProvider = metadataProvider;
        this.activeProtocolVersion = "2024-11-05";
    }

    public DefaultMcpClient(McpSyncClient syncClient) {
        this(null, syncClient, null);
    }

    public DefaultMcpClient(String serverName, McpSyncClient syncClient) {
        this(serverName, syncClient, null);
    }

    public DefaultMcpClient(String serverName, McpSyncClient syncClient, Function<McpSchema.Tool, ToolMetadata> metadataProvider) {
        this.serverName = serverName;
        this.command = null;
        this.args = List.of();
        this.env = Map.of();
        this.customTransport = null;
        this.syncClient = syncClient;
        this.metadataProvider = metadataProvider;
        this.connected = (syncClient != null);
        this.activeProtocolVersion = "2024-11-05";
    }

    public String serverName() {
        return serverName;
    }

    @Override
    public String protocolVersion() {
        return activeProtocolVersion;
    }

    @Override
    public List<String> supportedProtocolVersions() {
        return List.of("2024-11-05", "2026-07-28");
    }

    @Override
    public void connect() {
        if (connected && syncClient != null) {
            return;
        }
        if (command == null && customTransport == null) {
            if (syncClient != null) {
                this.connected = true;
            }
            return;
        }
        try {
            McpClientTransport transportToUse = customTransport;
            if (transportToUse == null) {
                ServerParameters params = ServerParameters.builder(command)
                        .args(args)
                        .env(env)
                        .build();
                JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
                transportToUse = new StdioClientTransport(params, jsonMapper);
            }

            JsonSchemaValidator noopValidator = (schema, input) -> new JsonSchemaValidator.ValidationResponse(true, null, null);

            this.syncClient = io.modelcontextprotocol.client.McpClient.sync(transportToUse)
                    .jsonSchemaValidator(noopValidator)
                    .capabilities(ClientCapabilities.builder()
                            .roots(true)
                            .sampling()
                            .build())
                    .build();

            this.syncClient.initialize();
            this.connected = true;
            log.info("Successfully connected to MCP Server [{}]", serverName != null ? serverName : "default");
        } catch (Exception e) {
            this.connected = false;
            log.error("Failed to connect to MCP Server [{}]: {}", serverName != null ? serverName : "default", e.getMessage(), e);
            throw new RuntimeException("MCP connection failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        if (syncClient != null) {
            try {
                if (command != null || customTransport != null) {
                    syncClient.close();
                }
            } catch (Exception e) {
                log.warn("Error closing MCP Sync Client [{}]: {}", serverName != null ? serverName : "default", e.getMessage());
            } finally {
                if (command != null || customTransport != null) {
                    syncClient = null;
                }
                connected = false;
            }
        } else {
            connected = false;
        }
    }

    @Override
    public void reconnect() {
        if (command == null && customTransport == null && syncClient != null) {
            this.connected = true;
            return;
        }
        disconnect();
        connect();
    }

    @Override
    public boolean isConnected() {
        return connected && syncClient != null;
    }

    @Override
    public List<AgentTool> tools() {
        if (!connected || syncClient == null) {
            if (command != null || customTransport != null) {
                connect();
            } else if (!connected) {
                return List.of();
            }
        }
        try {
            ListToolsResult toolsResult = syncClient.listTools();
            if (toolsResult == null || toolsResult.tools() == null) {
                return List.of();
            }
            return toolsResult.tools().stream()
                    .map(mcpTool -> {
                        ToolMetadata meta = (metadataProvider != null) ? metadataProvider.apply(mcpTool) : null;
                        return new McpToolWrapper(serverName, mcpTool, syncClient, meta);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list tools from MCP server [{}]", serverName != null ? serverName : "default", e);
            return List.of();
        }
    }
}
