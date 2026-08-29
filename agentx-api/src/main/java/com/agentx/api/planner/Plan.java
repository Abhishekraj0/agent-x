package com.agentx.api.planner;

import java.util.List;

/**
 * Represents a plan created by a Planner containing a series of steps to achieve a goal.
 */
public record Plan(
        String planId,
        String goal,
        List<PlanStep> steps
) {}
