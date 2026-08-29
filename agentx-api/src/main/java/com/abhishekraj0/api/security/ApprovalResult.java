package com.abhishekraj0.api.security;

/**
 * Result representing the outcome of a human approval request.
 */
public record ApprovalResult(
        boolean approved,
        String reviewer,
        String reason
) {}
