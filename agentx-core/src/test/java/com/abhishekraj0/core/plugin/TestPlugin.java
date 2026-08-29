package com.abhishekraj0.core.plugin;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.plugin.PluginContext;
import com.abhishekraj0.api.plugin.PluginMetadata;

/**
 * Test implementation of AgentPlugin to verify dynamic service loading.
 */
public class TestPlugin implements AgentPlugin {

    private boolean initialized = false;
    private boolean shutdown = false;

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata("test-plugin", "Test Plugin", "1.0.0", "A plugin for unit testing", "AgentX Developer");
    }

    @Override
    public void initialize(PluginContext context) {
        initialized = true;
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
