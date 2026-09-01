package com.abhishekraj0.plugin.example;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.plugin.PluginContext;
import com.abhishekraj0.api.plugin.PluginMetadata;

/**
 * Third-party example plugin registering calculator tools.
 */
public class CalculatorPlugin implements AgentPlugin {

    private final PluginMetadata metadata = new PluginMetadata(
            "calculator-plugin",
            "Calculator Plugin",
            "1.0.0",
            "Provides basic mathematical tools for AgentX",
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
        context.tools().register(new CalculatorTool());
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
