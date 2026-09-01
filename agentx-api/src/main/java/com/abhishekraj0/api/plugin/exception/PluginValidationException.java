package com.abhishekraj0.api.plugin.exception;

/**
 * Exception thrown when plugin metadata is null, blank, or malformed.
 */
public class PluginValidationException extends PluginException {
    public PluginValidationException(String message) {
        super(message);
    }
}
