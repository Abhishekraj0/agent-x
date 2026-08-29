package com.abhishekraj0.api.agent;

/**
 * Decision to delegate a subtask to another agent.
 */
public record DelegateDecision(
        String decisionId,
        String reason,
        String targetAgentId,
        String task
) implements AgentDecision {}
