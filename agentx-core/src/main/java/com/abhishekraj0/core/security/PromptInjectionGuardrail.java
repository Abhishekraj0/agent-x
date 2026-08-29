package com.abhishekraj0.core.security;

import com.abhishekraj0.api.agent.AgentAction;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.security.Guardrail;
import com.abhishekraj0.api.security.GuardrailResult;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Guardrail that scans messages and actions for potential prompt injection attempts.
 */
public class PromptInjectionGuardrail implements Guardrail {

    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+all\\s+previous\\s+instructions"),
            Pattern.compile("(?i)system\\s+override"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+in\\s+developer\\s+mode"),
            Pattern.compile("(?i)override\\s+system\\s+prompt"),
            Pattern.compile("(?i)disregard\\s+previous\\s+instructions"),
            Pattern.compile("(?i)dan\\s+mode"),
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)new\\s+instructions\\s+are")
    );

    @Override
    public GuardrailResult validate(AgentAction action, AgentContext context) {
        if (context != null && context.messages() != null) {
            for (ChatMessage message : context.messages()) {
                if (message.content() != null && matchesSuspiciousPattern(message.content())) {
                    return GuardrailResult.fail("PromptInjectionPolicy", "Security Violation: Prompt Injection Attempt Detected");
                }
            }
        }

        if (action != null && action.details() != null) {
            for (Object val : action.details().values()) {
                if (val instanceof String && matchesSuspiciousPattern((String) val)) {
                    return GuardrailResult.fail("PromptInjectionPolicy", "Security Violation: Prompt Injection Attempt Detected in tool arguments");
                }
            }
        }

        return GuardrailResult.pass();
    }

    private boolean matchesSuspiciousPattern(String text) {
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
