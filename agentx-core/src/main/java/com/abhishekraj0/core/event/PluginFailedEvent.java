package com.abhishekraj0.core.event;

/**
 * Event emitted when a plugin fails during discovery, initialization, or shutdown.
 */
public class PluginFailedEvent extends BaseAgentEvent {

    private final String errorMessage;

    public PluginFailedEvent(String pluginId, String errorMessage) {
        super(pluginId);
        this.errorMessage = errorMessage;
    }

    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public String type() {
        return "PLUGIN_FAILED";
    }
}
