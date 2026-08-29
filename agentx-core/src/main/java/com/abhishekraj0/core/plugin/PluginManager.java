package com.abhishekraj0.core.plugin;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.plugin.PluginContext;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Manager to discover, load, and manage the lifecycle of AgentX plugins.
 */
public class PluginManager {

    private final PluginContext context;
    private final List<AgentPlugin> activePlugins = new ArrayList<>();

    public PluginManager(PluginContext context) {
        this.context = context;
    }

    /**
     * Loads plugins using ServiceLoader and initializes them.
     */
    public void loadPlugins() {
        ServiceLoader<AgentPlugin> loader = ServiceLoader.load(AgentPlugin.class);
        for (AgentPlugin plugin : loader) {
            plugin.initialize(context);
            activePlugins.add(plugin);
        }
    }

    /**
     * Shuts down all active plugins.
     */
    public void shutdownPlugins() {
        for (AgentPlugin plugin : activePlugins) {
            try {
                plugin.shutdown();
            } catch (Exception e) {
                // Keep shutting down remaining plugins
            }
        }
        activePlugins.clear();
    }

    /**
     * Returns the list of currently active plugins.
     *
     * @return list of active plugins
     */
    public List<AgentPlugin> getActivePlugins() {
        return activePlugins;
    }
}
