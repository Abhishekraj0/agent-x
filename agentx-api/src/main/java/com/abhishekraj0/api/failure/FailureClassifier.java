package com.abhishekraj0.api.failure;

/**
 * Classifies throwables into structured AgentFailure exceptions.
 */
public interface FailureClassifier {

    AgentFailure classify(
        Throwable throwable,
        FailureContext context
    );
}
