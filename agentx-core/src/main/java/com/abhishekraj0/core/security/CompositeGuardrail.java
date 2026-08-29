package com.abhishekraj0.core.security;

import com.abhishekraj0.api.agent.AgentAction;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.security.Guardrail;
import com.abhishekraj0.api.security.GuardrailResult;
import java.util.List;

/**
 * CompositeGuardrail evaluates a list of Guardrails in sequence.
 * It passes only if all constituent guardrails pass.
 */
public class CompositeGuardrail implements Guardrail {

    private final List<Guardrail> guardrails;

    public CompositeGuardrail(List<Guardrail> guardrails) {
        this.guardrails = guardrails != null ? guardrails : List.of();
    }

    @Override
    public GuardrailResult validate(AgentAction action, AgentContext context) {
        for (Guardrail guardrail : guardrails) {
            GuardrailResult result = guardrail.validate(action, context);
            if (!result.passed()) {
                return result;
            }
        }
        return GuardrailResult.pass();
    }
}
