package com.abhishekraj0.api.agent;

import com.abhishekraj0.api.security.ApprovalRequest;

/**
 * Decision to halt execution and wait for human approval of a tool/action.
 */
public record WaitForApprovalDecision(
        String decisionId,
        String reason,
        ApprovalRequest approvalRequest
) implements AgentDecision {}
