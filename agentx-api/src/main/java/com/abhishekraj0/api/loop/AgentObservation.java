package com.abhishekraj0.api.loop;

/**
 * Represents the observed result of an action (e.g. tool execution outcome).
 */
public record AgentObservation(
        String observationId,
        String toolName,
        String output,
        boolean success,
        Throwable error
) {}
