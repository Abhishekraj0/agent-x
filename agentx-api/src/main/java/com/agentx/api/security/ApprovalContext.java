package com.agentx.api.security;

import com.agentx.api.agent.AgentAction;
import com.agentx.api.context.AgentContext;

/**
 * Context payload containing the action and execution details requiring approval.
 */
public record ApprovalContext(
        String executionId,
        AgentAction action,
        AgentContext context
) {}
