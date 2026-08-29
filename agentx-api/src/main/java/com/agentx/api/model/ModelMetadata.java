package com.agentx.api.model;

/**
 * Metadata containing capabilities and constraints of a ChatModel.
 */
public record ModelMetadata(
        String id,
        String provider,
        int maxContextTokens,
        boolean supportsStreaming,
        boolean supportsToolCalling
) {}
