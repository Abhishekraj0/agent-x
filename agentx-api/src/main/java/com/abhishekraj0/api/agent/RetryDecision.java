package com.abhishekraj0.api.agent;

import java.time.Duration;

/**
 * Decision made by the agent reasoning loop to retry a failed action or step.
 */
public record RetryDecision(
        String decisionId,
        String reason,
        Duration delay
) implements AgentDecision {}
