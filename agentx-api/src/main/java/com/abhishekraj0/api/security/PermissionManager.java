package com.abhishekraj0.api.security;

import com.abhishekraj0.api.agent.AgentAction;
import com.abhishekraj0.api.context.AgentContext;

/**
 * Interface to verify security permissions for agent actions.
 */
public interface PermissionManager {

    /**
     * Checks if the agent action is permitted under the current context.
     *
     * @param action  the action to check
     * @param context the execution context
     * @return the permission decision
     */
    PermissionDecision check(AgentAction action, AgentContext context);
}
