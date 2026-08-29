package com.abhishekraj0.api.plugin;

/**
 * Metadata associated with an AgentPlugin.
 */
public record PluginMetadata(
        String id,
        String name,
        String version,
        String description,
        String author
) {}
