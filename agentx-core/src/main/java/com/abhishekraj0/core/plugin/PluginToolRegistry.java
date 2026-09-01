package com.abhishekraj0.core.plugin;

import com.abhishekraj0.api.plugin.exception.PluginToolCollisionException;
import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolQuery;
import com.abhishekraj0.api.tool.ToolRegistry;
import java.util.Collection;
import java.util.Optional;

/**
 * ToolRegistry wrapper provided to plugins that enforces strict collision detection.
 */
public class PluginToolRegistry implements ToolRegistry {

    private final ToolRegistry delegate;
    private final String pluginId;

    public PluginToolRegistry(ToolRegistry delegate, String pluginId) {
        this.delegate = delegate;
        this.pluginId = pluginId;
    }

    @Override
    public void register(AgentTool tool) {
        if (tool == null || tool.id() == null) {
            return;
        }
        Optional<AgentTool> existing = delegate.get(tool.id());
        if (existing.isPresent()) {
            throw new PluginToolCollisionException(
                    "Tool collision detected for plugin [" + pluginId + "]: tool [" + tool.id().getFullName() + "] is already registered."
            );
        }
        delegate.register(tool);
    }

    @Override
    public void unregister(ToolId id) {
        delegate.unregister(id);
    }

    @Override
    public Optional<AgentTool> get(ToolId id) {
        return delegate.get(id);
    }

    @Override
    public Collection<AgentTool> all() {
        return delegate.all();
    }

    @Override
    public Collection<AgentTool> find(ToolQuery query) {
        return delegate.find(query);
    }
}
