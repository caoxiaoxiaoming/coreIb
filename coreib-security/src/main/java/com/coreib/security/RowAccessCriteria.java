package com.coreib.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Structured, database-neutral row criteria for a repository query.
 *
 * <p>Scopes have OR semantics. Repositories translate these facts into parameterized,
 * domain-specific predicates; this type deliberately contains no SQL.</p>
 */
public record RowAccessCriteria(
        Set<RowScope> scopes,
        Set<String> unresolvedCustomPolicyKeys,
        String subjectId,
        String organizationId,
        Set<String> visibleOrganizationIds,
        Set<String> managedUserIds,
        Set<String> relatedRecordIds) {

    public static final String MISSING_POLICY_KEY = "<missing-policy-key>";

    public RowAccessCriteria {
        requireText("subjectId", subjectId);
        scopes = immutableScopes(scopes);
        if (scopes.contains(RowScope.CUSTOM)) {
            throw new IllegalArgumentException("CUSTOM must remain unresolved, not executable");
        }
        unresolvedCustomPolicyKeys = immutableStrings(unresolvedCustomPolicyKeys);
        visibleOrganizationIds = immutableStrings(visibleOrganizationIds);
        managedUserIds = immutableStrings(managedUserIds);
        relatedRecordIds = immutableStrings(relatedRecordIds);
    }

    public static RowAccessCriteria from(
            PermissionSubject subject,
            Collection<PermissionPolicy> policies) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(policies, "policies");
        Set<RowScope> executableScopes = EnumSet.noneOf(RowScope.class);
        Set<String> unresolvedKeys = new LinkedHashSet<>();
        for (PermissionPolicy policy : policies) {
            if (policy.rowScope() == RowScope.CUSTOM) {
                unresolvedKeys.add(policy.policyKey() == null
                        ? MISSING_POLICY_KEY
                        : policy.policyKey());
            } else {
                executableScopes.add(policy.rowScope());
            }
        }
        return new RowAccessCriteria(
                executableScopes,
                unresolvedKeys,
                subject.userId(),
                subject.organizationId(),
                subject.visibleOrganizationIds(),
                subject.managedUserIds(),
                subject.relatedRecordIds());
    }

    public static RowAccessCriteria denyAll(PermissionSubject subject) {
        return from(subject, Set.of());
    }

    /** True when no standard scope can grant a row. */
    public boolean denied() {
        return scopes.isEmpty();
    }

    public boolean unrestricted() {
        return scopes.contains(RowScope.ALL);
    }

    public boolean hasUnresolvedCustomPolicies() {
        return !unresolvedCustomPolicyKeys.isEmpty();
    }

    /** In-memory equivalent of the OR predicates a repository must generate. */
    public boolean matches(PermissionRow row) {
        Objects.requireNonNull(row, "row");
        if (unrestricted()) {
            return true;
        }
        for (RowScope scope : scopes) {
            if (matches(scope, row)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(RowScope scope, PermissionRow row) {
        return switch (scope) {
            case ALL -> true;
            case ORGANIZATION -> Objects.equals(organizationId, row.organizationId());
            case ORGANIZATION_TREE -> visibleOrganizationIds.contains(row.organizationId());
            case SELF -> Objects.equals(subjectId, row.ownerUserId());
            case MANAGED_USERS -> managedUserRow(row);
            case RELATED_RECORDS -> relatedRecordIds.contains(row.rowId())
                    || row.assignedUserIds().contains(subjectId)
                    || row.relatedUserIds().contains(subjectId);
            case CUSTOM -> false;
        };
    }

    private boolean managedUserRow(PermissionRow row) {
        return (row.ownerUserId() != null && managedUserIds.contains(row.ownerUserId()))
                || managedUserIds.stream().anyMatch(row.assignedUserIds()::contains)
                || Objects.equals(subjectId, row.ownerUserId())
                || row.assignedUserIds().contains(subjectId);
    }

    private static Set<RowScope> immutableScopes(Set<RowScope> values) {
        return values == null || values.isEmpty()
                ? Set.of()
                : Set.copyOf(values);
    }

    private static Set<String> immutableStrings(Set<String> values) {
        return values == null || values.isEmpty()
                ? Set.of()
                : Set.copyOf(values);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
