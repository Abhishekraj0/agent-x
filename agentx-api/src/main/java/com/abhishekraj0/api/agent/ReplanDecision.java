package com.abhishekraj0.api.agent;

/**
 * Decision to perform dynamic replanning.
 */
public record ReplanDecision(
        String decisionId,
        String reason,
        String replanDetails
) implements AgentDecision {}
