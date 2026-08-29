package com.abhishekraj0.api.evaluation;

import java.util.Map;

/**
 * Context criteria against which an agent execution is evaluated.
 */
public record EvaluationContext(
        String expectedOutput,
        Map<String, Object> criteria
) {
    public static EvaluationContext expected(String expectedOutput) {
        return new EvaluationContext(expectedOutput, Map.of());
    }
}
