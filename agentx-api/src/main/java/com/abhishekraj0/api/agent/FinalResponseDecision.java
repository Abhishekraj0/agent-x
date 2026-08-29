package com.abhishekraj0.api.agent;

/**
 * Decision containing the final response to satisfy the user goal.
 */
public record FinalResponseDecision(
        String decisionId,
        String reason,
        String response
) implements AgentDecision {}
