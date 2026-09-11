package com.coreib.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Backend authorization facade used by application and domain services. */
public final class CoreIbAuthorizationService {
    private final CurrentAuthorizationProvider provider;
    private final CoreIbPermissionPolicies evaluator;

    public CoreIbAuthorizationService(CurrentAuthorizationProvider provider) {
        this(provider, new CoreIbPermissionPolicies());
    }

    public CoreIbAuthorizationService(
            CurrentAuthorizationProvider provider,
            CoreIbPermissionPolicies evaluator) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public PermissionDecision decide(
            String resource,
            PermissionAction action,
            PermissionRow row) {
        requireText("resource", resource);
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(row, "row");
        if (!provider.current().authenticated()) {
            return PermissionDecision.deny("anonymous-subject");
        }
        return evaluator.decide(
                provider.subject(resource),
                applicablePolicies(resource, action),
                action,
                row);
    }

    /** Checks the resource action before a list, aggregate or export query is executed. */
    public PermissionDecision decide(String resource, PermissionAction action) {
        requireText("resource", resource);
        Objects.requireNonNull(action, "action");
        if (!provider.current().authenticated()) {
            return PermissionDecision.deny("anonymous-subject");
        }
        if (applicablePolicies(resource, action).isEmpty()) {
            return PermissionDecision.deny("action-not-granted");
        }
        return PermissionDecision.allow("action-granted");
    }

    public void require(
            String resource,
            PermissionAction action,
            PermissionRow row) {
        PermissionDecision decision = decide(resource, action, row);
        if (!decision.allowed()) {
            throw new PermissionDeniedException(resource, action, decision.reason());
        }
    }

    public void require(String resource, PermissionAction action) {
        PermissionDecision decision = decide(resource, action);
        if (!decision.allowed()) {
            throw new PermissionDeniedException(resource, action, decision.reason());
        }
    }

    /**
     * Produces an OR-set of row scopes for list, aggregate, export and batch repositories.
     * An empty set means deny all; unresolved custom policies do not grant rows.
     */
    public RowAccessCriteria rowCriteria(String resource, PermissionAction action) {
        requireText("resource", resource);
        Objects.requireNonNull(action, "action");
        PermissionSnapshot snapshot = provider.current();
        if (!snapshot.authenticated()) {
            return RowAccessCriteria.denyAll(PermissionSubject.of(snapshot.subjectId(), null));
        }
        PermissionSubject subject = provider.subject(resource);
        return RowAccessCriteria.from(subject, applicablePolicies(resource, action));
    }

    /** Returns the strongest field access granted by any policy for the resource. */
    public ColumnAccess fieldAccess(String resource, String field) {
        return fieldAccess(resource, null, field);
    }

    /** Returns field access only from policies that also grant the requested action. */
    public ColumnAccess fieldAccess(
            String resource,
            PermissionAction action,
            String field) {
        requireText("resource", resource);
        requireText("field", field);
        if (!provider.current().authenticated()) {
            return ColumnAccess.HIDDEN;
        }
        Collection<PermissionPolicy> policies = action == null
                ? policiesFor(resource)
                : applicablePolicies(resource, action);
        ColumnAccess result = ColumnAccess.HIDDEN;
        for (PermissionPolicy policy : policies) {
            result = strongerAccess(result, policy.fields().getOrDefault(field, ColumnAccess.HIDDEN));
        }
        return result;
    }

    private List<PermissionPolicy> applicablePolicies(
            String resource,
            PermissionAction action) {
        return policiesFor(resource).stream()
                .filter(policy -> policy.allows(action))
                .toList();
    }

    private List<PermissionPolicy> policiesFor(String resource) {
        Collection<PermissionPolicy> policies = provider.policies(resource);
        if (policies == null) {
            return List.of();
        }
        return policies.stream()
                .filter(Objects::nonNull)
                .filter(policy -> resource.equals(policy.resource()))
                .toList();
    }

    private static ColumnAccess strongerAccess(ColumnAccess left, ColumnAccess right) {
        return accessRank(left) >= accessRank(right) ? left : right;
    }

    private static int accessRank(ColumnAccess access) {
        return switch (access) {
            case HIDDEN -> 0;
            case MASKED -> 1;
            case READ -> 2;
            case WRITE -> 3;
        };
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
