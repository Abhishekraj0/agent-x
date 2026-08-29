package com.abhishekraj0.api.security;

/**
 * Result returned by a Guardrail validation check.
 */
public record GuardrailResult(
        boolean passed,
        String failureReason,
        String policyName
) {
    public static GuardrailResult pass() {
        return new GuardrailResult(true, null, "DefaultPolicy");
    }

    public static GuardrailResult fail(String policyName, String reason) {
        return new GuardrailResult(false, reason, policyName);
    }
}
