package com.abhishekraj0.core.plugin;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.plugin.PluginContext;
import com.abhishekraj0.api.plugin.PluginMetadata;
import com.abhishekraj0.api.plugin.PluginState;
import com.abhishekraj0.api.plugin.exception.DuplicatePluginException;
import com.abhishekraj0.api.plugin.exception.PluginException;
import com.abhishekraj0.api.plugin.exception.PluginValidationException;
import com.abhishekraj0.core.event.PluginFailedEvent;
import com.abhishekraj0.core.event.PluginLoadedEvent;
import com.abhishekraj0.core.event.PluginShutdownEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Robust PluginManager to discover, validate, initialize, and manage the lifecycle of AgentX plugins.
 */
public class PluginManager {

    private final PluginContext baseContext;
    private final Map<String, AgentPlugin> activePlugins = new HashMap<>();
    private final Map<String, PluginState> pluginStates = new HashMap<>();

    public PluginManager(PluginContext context) {
        this.baseContext = context;
    }

    /**
     * Loads plugins using ServiceLoader from the current thread context classloader.
     */
    public void loadPlugins() {
        loadPlugins(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads plugins using ServiceLoader from a specified ClassLoader.
     *
     * @param classLoader classloader to discover plugins from
     */
    public void loadPlugins(ClassLoader classLoader) {
        ClassLoader cl = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        ServiceLoader<AgentPlugin> loader = ServiceLoader.load(AgentPlugin.class, cl);

        for (AgentPlugin plugin : loader) {
            registerPlugin(plugin);
        }
    }

    /**
     * Registers and initializes an explicit plugin instance.
     *
     * @param plugin plugin instance to register
     */
    public void registerPlugin(AgentPlugin plugin) {
        if (plugin == null) {
            throw new PluginValidationException("Cannot register null plugin");
        }

        PluginMetadata metadata = plugin.metadata();
        if (metadata == null) {
            throw new PluginValidationException("Plugin metadata cannot be null");
        }

        // Validate metadata
        metadata.validate();
        String pluginId = metadata.id();

        // Check duplicate plugin ID
        if (activePlugins.containsKey(pluginId)) {
            throw new DuplicatePluginException("Duplicate plugin ID detected: " + pluginId);
        }

        pluginStates.put(pluginId, PluginState.DISCOVERED);
        pluginStates.put(pluginId, PluginState.VALIDATED);

        // Create isolated tool registry wrapper with collision detection for this plugin
        PluginToolRegistry pluginToolRegistry = new PluginToolRegistry(baseContext.tools(), pluginId);
        PluginContext scopedContext = new DefaultPluginContext(
                pluginToolRegistry,
                baseContext.agents(),
                baseContext.memory(),
                baseContext.events(),
                baseContext.configuration()
        );

        try {
            plugin.initialize(scopedContext);
            pluginStates.put(pluginId, PluginState.INITIALIZED);
            pluginStates.put(pluginId, PluginState.ACTIVE);
            activePlugins.put(pluginId, plugin);

            if (baseContext.events() != null) {
                baseContext.events().publish(new PluginLoadedEvent(metadata));
            }
        } catch (Exception e) {
            pluginStates.put(pluginId, PluginState.FAILED);
            if (baseContext.events() != null) {
                baseContext.events().publish(new PluginFailedEvent(pluginId, e.getMessage()));
            }
            if (e instanceof PluginException pe) {
                throw pe;
            }
            throw new PluginException("Failed to initialize plugin [" + pluginId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Shuts down all active plugins gracefully.
     */
    public void shutdownPlugins() {
        List<String> ids = new ArrayList<>(activePlugins.keySet());
        for (String id : ids) {
            AgentPlugin plugin = activePlugins.get(id);
            try {
                plugin.shutdown();
                pluginStates.put(id, PluginState.SHUTDOWN);
                if (baseContext.events() != null) {
                    baseContext.events().publish(new PluginShutdownEvent(id));
                }
            } catch (Exception e) {
                pluginStates.put(id, PluginState.FAILED);
                if (baseContext.events() != null) {
                    baseContext.events().publish(new PluginFailedEvent(id, "Shutdown error: " + e.getMessage()));
                }
            }
        }
        activePlugins.clear();
    }

    public List<AgentPlugin> getActivePlugins() {
        return Collections.unmodifiableList(new ArrayList<>(activePlugins.values()));
    }

    public PluginState getPluginState(String pluginId) {
        return pluginStates.getOrDefault(pluginId, PluginState.FAILED);
    }
}
