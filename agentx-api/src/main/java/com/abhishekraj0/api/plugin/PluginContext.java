package com.abhishekraj0.api.plugin;

import com.abhishekraj0.api.agent.AgentRegistry;
import com.abhishekraj0.api.event.EventBus;
import com.abhishekraj0.api.memory.MemoryStore;
import com.abhishekraj0.api.tool.ToolRegistry;
import java.util.Map;

/**
 * Context provided during plugin initialization, giving access to registers and configuration.
 */
public interface PluginContext {

    /**
     * Gets the ToolRegistry to register custom tools.
     *
     * @return the tool registry
     */
    ToolRegistry tools();

    /**
     * Gets the AgentRegistry to register custom agents.
     *
     * @return the agent registry
     */
    AgentRegistry agents();

    /**
     * Gets the MemoryStore to interact with memory system.
     *
     * @return the memory store
     */
    MemoryStore memory();

    /**
     * Gets the EventBus to subscribe/publish events.
     *
     * @return the event bus
     */
    EventBus events();

    /**
     * Configuration properties provided for this plugin.
     *
     * @return the configuration map
     */
    Map<String, Object> configuration();
}
