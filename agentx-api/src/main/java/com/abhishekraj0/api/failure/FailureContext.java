package com.abhishekraj0.api.failure;

import java.io.Serializable;
import java.util.Map;

/**
 * Contextual metadata about a failure to help classification.
 */
public record FailureContext(
    String executionId,
    String stepName,
    Map<String, String> metadata
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static FailureContext of(String executionId, String stepName) {
        return new FailureContext(executionId, stepName, Map.of());
    }

    public static FailureContext of(String executionId, String stepName, Map<String, String> metadata) {
        return new FailureContext(executionId, stepName, Map.copyOf(metadata));
    }
}
