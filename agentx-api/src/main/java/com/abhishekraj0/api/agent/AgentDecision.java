package com.abhishekraj0.api.agent;

/**
 * Strongly typed decision made by the agent reasoning loop.
 */
public sealed interface AgentDecision
    permits ToolCallDecision,
            FinalResponseDecision,
            AskUserDecision,
            DelegateDecision,
            ReplanDecision,
            WaitForApprovalDecision,
            RetryDecision {
    
    String decisionId();
    String reason();
}
