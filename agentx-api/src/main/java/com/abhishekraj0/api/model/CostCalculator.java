package com.abhishekraj0.api.model;

/**
 * Interface to calculate cost based on token usage and model metadata.
 */
public interface CostCalculator {
    Cost calculate(ModelMetadata model, TokenUsage usage);
}
