package com.abhishekraj0.api.plugin;

/**
 * Lifecycle states of an AgentX plugin.
 */
public enum PluginState {
    DISCOVERED,
    VALIDATED,
    INITIALIZED,
    ACTIVE,
    SHUTDOWN,
    FAILED
}
