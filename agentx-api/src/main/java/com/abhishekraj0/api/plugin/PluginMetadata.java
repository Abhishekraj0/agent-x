package com.abhishekraj0.api.plugin;

import com.abhishekraj0.api.plugin.exception.PluginValidationException;

/**
 * Metadata associated with an AgentPlugin.
 */
public record PluginMetadata(
        String id,
        String name,
        String version,
        String description,
        String author
) {
    /**
     * Validates that essential metadata fields (id and version) are non-null and non-blank.
     *
     * @throws PluginValidationException if validation fails
     */
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new PluginValidationException("Plugin ID cannot be null or blank");
        }
        if (version == null || version.isBlank()) {
            throw new PluginValidationException("Plugin version cannot be null or blank for plugin: " + id);
        }
    }
}
