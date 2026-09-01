package com.abhishekraj0.plugin.example;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.plugin.PluginContext;
import com.abhishekraj0.api.plugin.PluginMetadata;

/**
 * Third-party example plugin registering greeting tools.
 */
public class GreetingPlugin implements AgentPlugin {

    private final PluginMetadata metadata = new PluginMetadata(
            "greeting-plugin",
            "Greeting Plugin",
            "1.0.0",
            "Provides greeting capabilities for AgentX",
            "External Developer"
    );

    private boolean initialized = false;
    private boolean shutdown = false;

    @Override
    public PluginMetadata metadata() {
        return metadata;
    }

    @Override
    public void initialize(PluginContext context) {
        context.tools().register(new GreetingTool());
        this.initialized = true;
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
