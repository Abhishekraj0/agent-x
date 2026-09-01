package com.abhishekraj0.core.event;

/**
 * Event emitted when a plugin is shut down.
 */
public class PluginShutdownEvent extends BaseAgentEvent {

    public PluginShutdownEvent(String pluginId) {
        super(pluginId);
    }

    @Override
    public String type() {
        return "PLUGIN_SHUTDOWN";
    }
}
