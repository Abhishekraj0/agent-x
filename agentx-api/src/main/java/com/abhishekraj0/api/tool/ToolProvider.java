package com.abhishekraj0.api.tool;

import java.util.Collection;

/**
 * Interface representing a source that provides tools, such as an MCP server.
 */
public interface ToolProvider {

    /**
     * Returns a collection of tools provided by this source.
     *
     * @return the collection of tools
     */
    Collection<AgentTool> tools();
}
