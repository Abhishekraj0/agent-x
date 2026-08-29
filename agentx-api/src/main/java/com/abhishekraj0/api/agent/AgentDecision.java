package com.abhishekraj0.api.agent;

/**
 * Represents a decision made by an agent reasoning loop, containing reasoning details and the chosen action.
 */
public record AgentDecision(
        String decisionId,
        String reason,
        AgentAction action
) {}
