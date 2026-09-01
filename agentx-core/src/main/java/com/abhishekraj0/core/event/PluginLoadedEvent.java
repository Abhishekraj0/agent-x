package com.abhishekraj0.core.event;

import com.abhishekraj0.api.plugin.PluginMetadata;

/**
 * Event emitted when a plugin is successfully loaded and initialized.
 */
public class PluginLoadedEvent extends BaseAgentEvent {

    private final PluginMetadata metadata;

    public PluginLoadedEvent(PluginMetadata metadata) {
        super(metadata != null ? metadata.id() : "unknown");
        this.metadata = metadata;
    }

    public PluginMetadata metadata() {
        return metadata;
    }

    @Override
    public String type() {
        return "PLUGIN_LOADED";
    }
}
