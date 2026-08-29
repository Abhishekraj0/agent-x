package com.abhishekraj0.api.agent;

import com.abhishekraj0.api.security.ApprovalResult;
import java.io.Serializable;
import java.util.Map;

/**
 * Payload data used to resume suspended agent executions.
 */
public record ResumeInput(
        String userInput,
        ApprovalResult approvalResult,
        Map<String, Object> additionalVariables
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static ResumeInput ofUserResponse(String response) {
        return new ResumeInput(response, null, Map.of());
    }

    public static ResumeInput ofApproval(ApprovalResult result) {
        return new ResumeInput(null, result, Map.of());
    }
}
