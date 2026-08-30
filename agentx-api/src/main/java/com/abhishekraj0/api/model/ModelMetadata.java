package com.abhishekraj0.api.model;

/**
 * Metadata containing capabilities and constraints of a ChatModel.
 */
public record ModelMetadata(
        String id,
        String provider,
        int maxContextTokens,
        boolean supportsStreaming,
        boolean supportsToolCalling,
        ModelCapabilities capabilities
) {
    public ModelMetadata(String id, String provider, int maxContextTokens, boolean supportsStreaming, boolean supportsToolCalling) {
        this(id, provider, maxContextTokens, supportsStreaming, supportsToolCalling,
             new ModelCapabilities(supportsStreaming, supportsToolCalling, false, false, false));
    }
}
