package com.abhishekraj0.api.mcp;

import com.abhishekraj0.api.tool.ToolProvider;
import java.util.List;

/**
 * Interface representing a Model Context Protocol (MCP) client.
 */
public interface McpClient extends ToolProvider {

    /**
     * Establishes a connection to the MCP Server.
     */
    void connect();

    /**
     * Disconnects from the MCP Server.
     */
    void disconnect();

    /**
     * Reconnects to the MCP Server.
     */
    default void reconnect() {
        disconnect();
        connect();
    }

    /**
     * Checks if the client is currently connected.
     *
     * @return true if connected
     */
    default boolean isConnected() {
        return true;
    }

    /**
     * Returns the active negotiated MCP protocol version string.
     *
     * @return protocol version string (e.g., "2024-11-05" or "2026-07-28")
     */
    default String protocolVersion() {
        return "2024-11-05";
    }

    /**
     * Returns all protocol revisions supported by this AgentX MCP client adapter.
     *
     * @return list of supported protocol revision identifiers
     */
    default List<String> supportedProtocolVersions() {
        return List.of("2024-11-05", "2026-07-28");
    }
}
