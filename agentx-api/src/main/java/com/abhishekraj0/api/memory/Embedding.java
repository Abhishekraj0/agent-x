package com.abhishekraj0.api.memory;

import java.util.List;

/**
 * Represents a vector embedding of text content.
 */
public record Embedding(
        List<Double> vector
) {}
