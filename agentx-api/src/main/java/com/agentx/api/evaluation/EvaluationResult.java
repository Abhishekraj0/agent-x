package com.agentx.api.evaluation;

import java.util.Map;

/**
 * Result representing the evaluation details, metrics, and score.
 */
public record EvaluationResult(
        double score, // normalized score between 0.0 and 1.0
        boolean passed,
        String feedback,
        Map<String, Object> metrics
) {}
