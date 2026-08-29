package com.agentx.api.plugin;

/**
 * Interface representing a plugin that can extend the AgentX ecosystem.
 */
public interface AgentPlugin {

    /**
     * Returns the metadata description of the plugin.
     *
     * @return the metadata
     */
    PluginMetadata metadata();

    /**
     * Callback for initializing the plugin, registering extensions into the context.
     *
     * @param context the plugin initialization context
     */
    void initialize(PluginContext context);

    /**
     * Callback executed when the plugin is shut down.
     */
    void shutdown();
}
