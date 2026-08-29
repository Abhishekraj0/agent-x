package com.abhishekraj0.api.security;

/**
 * Interface responsible for requesting and waiting for human approval for sensitive actions.
 */
public interface ApprovalProvider {

    /**
     * Creates and registers an approval request for human review.
     *
     * @param context the approval context
     * @return the approval request
     */
    ApprovalRequest request(ApprovalContext context);

    /**
     * Waits (potentially blocking) for the human approval result.
     *
     * @param request the approval request details
     * @return the approval result
     */
    ApprovalResult waitFor(ApprovalRequest request);
}
