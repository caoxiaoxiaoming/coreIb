package com.coreib.security;

import java.util.Objects;

/** Reusable service-layer guard for reads, writes, exports and batch operations. */
public final class CoreIbPermissionGuard {
    private final CoreIbPermissionEvaluator evaluator;

    public CoreIbPermissionGuard() {
        this(new CoreIbPermissionEvaluator());
    }

    public CoreIbPermissionGuard(CoreIbPermissionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public void require(
            PermissionSubject subject,
            PermissionPolicy policy,
            PermissionAction action,
            PermissionRow row) {
        PermissionDecision decision = evaluator.decide(subject, policy, action, row);
        if (!decision.allowed()) {
            throw new PermissionDeniedException(policy.resource(), action, decision.reason());
        }
    }
}
