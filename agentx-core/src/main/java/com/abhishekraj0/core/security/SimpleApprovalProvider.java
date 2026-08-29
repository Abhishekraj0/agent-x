package com.abhishekraj0.core.security;

import com.abhishekraj0.api.security.ApprovalContext;
import com.abhishekraj0.api.security.ApprovalProvider;
import com.abhishekraj0.api.security.ApprovalRequest;
import com.abhishekraj0.api.security.ApprovalResult;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory ApprovalProvider that allows programmatic or auto-approvals for testing and execution control.
 */
public class SimpleApprovalProvider implements ApprovalProvider {

    private final ConcurrentMap<String, ApprovalResult> pendingApprovals = new ConcurrentHashMap<>();
    private boolean autoApprove = false;

    public SimpleApprovalProvider() {}

    public SimpleApprovalProvider(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    public void respond(String requestId, boolean approved, String reason) {
        pendingApprovals.put(requestId, new ApprovalResult(approved, "UserApproval", reason));
    }

    @Override
    public ApprovalRequest request(ApprovalContext context) {
        String requestId = UUID.randomUUID().toString();
        String description = "Action of type " + context.action().type() + " requires approval.";
        String detailsJson = String.valueOf(context.action().details());
        return new ApprovalRequest(requestId, description, detailsJson);
    }

    @Override
    public ApprovalResult waitFor(ApprovalRequest request) {
        if (autoApprove) {
            return new ApprovalResult(true, "System", "Auto approved");
        }
        // Block and wait or poll until response is available
        int attempts = 0;
        while (!pendingApprovals.containsKey(request.id()) && attempts < 100) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ApprovalResult(false, "Approval thread interrupted", "System");
            }
            attempts++;
        }
        return pendingApprovals.getOrDefault(request.id(), new ApprovalResult(false, "Timeout waiting for approval", "System"));
    }
}
