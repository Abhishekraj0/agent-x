package com.abhishekraj0.api.model;

import java.util.Optional;

/**
 * Interface to determine fallback models on failure.
 */
public interface ModelFallbackStrategy {

    Optional<ChatModel> getFallback(ChatModel primaryModel, Throwable error);
}
