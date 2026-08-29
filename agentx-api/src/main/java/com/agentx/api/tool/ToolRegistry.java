package com.agentx.api.tool;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry interface to manage registering and retrieving AgentTools.
 */
public interface ToolRegistry {

    /**
     * Registers a tool.
     *
     * @param tool the tool to register
     */
    void register(AgentTool tool);

    /**
     * Unregisters a tool by ID.
     *
     * @param id the tool ID
     */
    void unregister(ToolId id);

    /**
     * Retrieves a tool by ID.
     *
     * @param id the tool ID
     * @return the tool if found
     */
    Optional<AgentTool> get(ToolId id);

    /**
     * Returns all registered tools.
     *
     * @return the collection of registered tools
     */
    Collection<AgentTool> all();

    /**
     * Finds tools matching the search query.
     *
     * @param query the query criteria
     * @return the matched tools
     */
    Collection<AgentTool> find(ToolQuery query);
}
