package com.abhishekraj0.api.memory;

/**
 * Generates vector embeddings for input text.
 */
public interface EmbeddingModel {

    /**
     * Generates an embedding for the given text.
     *
     * @param text the input text to embed
     * @return the embedding
     */
    Embedding embed(String text);
}
