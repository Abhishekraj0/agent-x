package com.abhishekraj0.api.plugin.exception;

/**
 * Exception thrown when two plugins register conflicting tools with the same name.
 */
public class PluginToolCollisionException extends PluginException {
    public PluginToolCollisionException(String message) {
        super(message);
    }
}
