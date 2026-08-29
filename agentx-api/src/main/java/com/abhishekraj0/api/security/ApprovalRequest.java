package com.abhishekraj0.api.security;

/**
 * Request describing an action that requires human review.
 */
public record ApprovalRequest(
        String id,
        String description,
        String actionDetailsJson
) {}
