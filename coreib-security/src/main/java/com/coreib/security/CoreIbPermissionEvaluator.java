package com.coreib.security;

import java.util.Objects;

/**
 * Vendor-neutral row and action evaluator. SQL generation remains in the domain repository,
 * so this module does not concatenate SQL or depend on one database dialect.
 */
public final class CoreIbPermissionEvaluator {
    public PermissionDecision decide(
            PermissionSubject subject,
            PermissionPolicy policy,
            PermissionAction action,
            PermissionRow row) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(row, "row");

        if (!policy.allows(action)) {
            return PermissionDecision.deny("action-not-granted");
        }
        if (RowAccessCriteria.from(subject, java.util.List.of(policy)).matches(row)) {
            return PermissionDecision.allow("action-and-row-scope-granted");
        }
        return PermissionDecision.deny("row-outside-scope");
    }
}
