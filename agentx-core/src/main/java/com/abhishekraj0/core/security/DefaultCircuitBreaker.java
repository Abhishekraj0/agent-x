package com.abhishekraj0.core.security;

import com.abhishekraj0.api.security.CircuitBreaker;
import com.abhishekraj0.api.security.Permission;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standard implementation of a CircuitBreaker for fault tolerance.
 */
public class DefaultCircuitBreaker implements CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration cooldownPeriod;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final int halfOpenSuccessThreshold = 2;
    private Instant openStateTimestamp = null;

    public DefaultCircuitBreaker(int failureThreshold, Duration cooldownPeriod) {
        this.failureThreshold = failureThreshold;
        this.cooldownPeriod = cooldownPeriod;
    }

    @Override
    public synchronized Permission acquire() {
        State current = state.get();
        if (current == State.OPEN) {
            if (Instant.now().isAfter(openStateTimestamp.plus(cooldownPeriod))) {
                state.set(State.HALF_OPEN);
                successCount.set(0);
                return () -> true;
            }
            return () -> false;
        }
        return () -> true;
    }

    @Override
    public synchronized void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            if (successCount.incrementAndGet() >= halfOpenSuccessThreshold) {
                state.set(State.CLOSED);
                failureCount.set(0);
            }
        } else if (state.get() == State.CLOSED) {
            failureCount.set(0);
        }
    }

    @Override
    public synchronized void recordFailure(Throwable failure) {
        if (state.get() == State.CLOSED) {
            if (failureCount.incrementAndGet() >= failureThreshold) {
                state.set(State.OPEN);
                openStateTimestamp = Instant.now();
            }
        } else if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
            openStateTimestamp = Instant.now();
        }
    }

    public State getState() {
        return state.get();
    }
}
