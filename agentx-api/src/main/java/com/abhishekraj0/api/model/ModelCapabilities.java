package com.abhishekraj0.api.model;

import java.io.Serializable;

/**
 * Encapsulates the capabilities supported by a model.
 */
public record ModelCapabilities(
        boolean streaming,
        boolean toolCalling,
        boolean structuredOutput,
        boolean vision,
        boolean embeddings
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
