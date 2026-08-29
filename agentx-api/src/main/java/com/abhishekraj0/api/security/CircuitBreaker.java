package com.abhishekraj0.api.security;

/**
 * Interface to manage call routing and fault tolerance for external providers.
 */
public interface CircuitBreaker {

    Permission acquire();

    void recordSuccess();

    void recordFailure(Throwable failure);
}
