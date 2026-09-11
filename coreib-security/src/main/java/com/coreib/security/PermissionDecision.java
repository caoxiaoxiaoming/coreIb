package com.coreib.security;

/** Explainable result used by service guards and audit logging. */
public record PermissionDecision(boolean allowed, String reason) {
    public PermissionDecision {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    public static PermissionDecision allow(String reason) {
        return new PermissionDecision(true, reason);
    }

    public static PermissionDecision deny(String reason) {
        return new PermissionDecision(false, reason);
    }
}
