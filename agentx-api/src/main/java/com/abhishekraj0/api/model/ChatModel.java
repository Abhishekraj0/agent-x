package com.abhishekraj0.api.model;

/**
 * Interface representing a synchronous Chat Model (LLM adapter).
 */
public interface ChatModel {

    /**
     * Executes a chat prompt against the model.
     *
     * @param request the chat request details
     * @return the chat response
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Returns the metadata and capabilities of the model.
     *
     * @return the model metadata
     */
    ModelMetadata metadata();
}
