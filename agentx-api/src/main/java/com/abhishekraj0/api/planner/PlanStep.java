package com.abhishekraj0.api.planner;

/**
 * Represents a single step in an agent's execution plan.
 */
public record PlanStep(
        String stepId,
        String description,
        String status, // e.g. PENDING, RUNNING, COMPLETED, FAILED
        String result
) {
    public PlanStep(String stepId, String description) {
        this(stepId, description, "PENDING", null);
    }
}
