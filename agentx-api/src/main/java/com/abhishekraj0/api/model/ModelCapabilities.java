package com.abhishekraj0.api.model;

import java.io.Serializable;
import java.util.Set;

/**
 * Encapsulates the capabilities supported by a model.
 */
public record ModelCapabilities(
        Set<String> capabilities
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CHAT = "CHAT";
    public static final String STREAMING = "STREAMING";
    public static final String TOOL_CALLING = "TOOL_CALLING";
    public static final String STRUCTURED_OUTPUT = "STRUCTURED_OUTPUT";
    public static final String VISION = "VISION";
    public static final String EMBEDDINGS = "EMBEDDINGS";

    public boolean supports(String capability) {
        return capabilities != null && capabilities.contains(capability);
    }
}
