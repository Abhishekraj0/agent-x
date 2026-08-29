package com.abhishekraj0.api.model;

/**
 * Represents the estimated financial cost of an execution step.
 */
public record Cost(
        double inputCost,
        double outputCost,
        double totalCost
) {}
