package com.abhishekraj0.api.plugin.exception;

/**
 * Exception thrown when a duplicate plugin ID is detected.
 */
public class DuplicatePluginException extends PluginException {
    public DuplicatePluginException(String message) {
        super(message);
    }
}
