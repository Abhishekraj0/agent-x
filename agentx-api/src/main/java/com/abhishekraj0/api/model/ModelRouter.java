package com.abhishekraj0.api.model;

import java.util.Optional;
import java.util.Set;

/**
 * Interface to route chat model requests based on capabilities.
 */
public interface ModelRouter {

    Optional<ChatModel> route(Set<String> requiredCapabilities);
}
