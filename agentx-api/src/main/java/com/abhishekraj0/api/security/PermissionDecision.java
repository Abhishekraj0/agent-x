package com.abhishekraj0.api.security;

/**
 * Decision returned by a PermissionManager checking whether an action is authorized.
 */
public record PermissionDecision(
        PermissionStatus status,
        String reason
) {
    public static PermissionDecision allow() {
        return new PermissionDecision(PermissionStatus.ALLOW, "Allowed");
    }

    public static PermissionDecision deny(String reason) {
        return new PermissionDecision(PermissionStatus.DENY, reason);
    }

    public static PermissionDecision requireApproval(String reason) {
        return new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, reason);
    }
}
