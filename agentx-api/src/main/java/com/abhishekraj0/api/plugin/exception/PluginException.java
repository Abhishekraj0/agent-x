package com.abhishekraj0.api.plugin.exception;

/**
 * Base exception for AgentX plugin ecosystem errors.
 */
public class PluginException extends RuntimeException {
    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
