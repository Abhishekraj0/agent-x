package com.agentx.core.plugin;

import com.agentx.api.agent.AgentRegistry;
import com.agentx.api.event.EventBus;
import com.agentx.api.memory.MemoryStore;
import com.agentx.api.plugin.PluginContext;
import com.agentx.api.tool.ToolRegistry;
import java.util.Map;

/**
 * Default implementation of PluginContext.
 */
public class DefaultPluginContext implements PluginContext {

    private final ToolRegistry toolRegistry;
    private final AgentRegistry agentRegistry;
    private final MemoryStore memoryStore;
    private final EventBus eventBus;
    private final Map<String, Object> configuration;

    public DefaultPluginContext(
            ToolRegistry toolRegistry,
            AgentRegistry agentRegistry,
            MemoryStore memoryStore,
            EventBus eventBus,
            Map<String, Object> configuration) {
        this.toolRegistry = toolRegistry;
        this.agentRegistry = agentRegistry;
        this.memoryStore = memoryStore;
        this.eventBus = eventBus;
        this.configuration = configuration != null ? configuration : Map.of();
    }

    @Override
    public ToolRegistry tools() {
        return toolRegistry;
    }

    @Override
    public AgentRegistry agents() {
        return agentRegistry;
    }

    @Override
    public MemoryStore memory() {
        return memoryStore;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }

    @Override
    public Map<String, Object> configuration() {
        return configuration;
    }
}
