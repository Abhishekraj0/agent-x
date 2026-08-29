package com.abhishekraj0.api.security;

import com.abhishekraj0.api.agent.AgentAction;
import com.abhishekraj0.api.context.AgentContext;

/**
 * Context payload containing the action and execution details requiring approval.
 */
public record ApprovalContext(
        String executionId,
        AgentAction action,
        AgentContext context
) {}
