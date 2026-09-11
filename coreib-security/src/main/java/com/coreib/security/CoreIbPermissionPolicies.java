package com.coreib.security;

import java.util.Collection;
import java.util.Objects;

/** Combines role grants without embedding role names in business services. */
public final class CoreIbPermissionPolicies {
    private final CoreIbPermissionEvaluator evaluator;

    public CoreIbPermissionPolicies() {
        this(new CoreIbPermissionEvaluator());
    }

    public CoreIbPermissionPolicies(CoreIbPermissionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public PermissionDecision decide(
            PermissionSubject subject,
            Collection<PermissionPolicy> policies,
            PermissionAction action,
            PermissionRow row) {
        Objects.requireNonNull(policies, "policies");
        for (PermissionPolicy policy : policies) {
            PermissionDecision decision = evaluator.decide(subject, policy, action, row);
            if (decision.allowed()) {
                return decision;
            }
        }
        return PermissionDecision.deny("no-grant-matched");
    }
}
