package com.agentx.api.security;

import com.agentx.api.agent.AgentAction;
import com.agentx.api.context.AgentContext;

/**
 * Interface representing a guardrail that validates agent actions before or after model processing.
 */
public interface Guardrail {

    /**
     * Validates an action within the current context.
     *
     * @param action  the action to validate
     * @param context the execution context
     * @return the guardrail validation result
     */
    GuardrailResult validate(AgentAction action, AgentContext context);
}
