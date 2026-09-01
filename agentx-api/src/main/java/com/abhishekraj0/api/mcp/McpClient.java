package com.abhishekraj0.api.mcp;

import com.abhishekraj0.api.tool.ToolProvider;

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
}
