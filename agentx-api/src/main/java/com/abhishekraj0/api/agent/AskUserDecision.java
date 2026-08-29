package com.abhishekraj0.api.agent;

/**
 * Decision to request clarification or input from the user.
 */
public record AskUserDecision(
        String decisionId,
        String reason,
        String question
) implements AgentDecision {}
