package com.coreib.security;

import java.util.Set;

/** Row facts supplied by the domain repository to the policy evaluator. */
public record PermissionRow(
        String rowId,
        String ownerUserId,
        String organizationId,
        Set<String> assignedUserIds,
        Set<String> relatedUserIds) {

    public PermissionRow {
        requireText("rowId", rowId);
        assignedUserIds = immutableSet(assignedUserIds);
        relatedUserIds = immutableSet(relatedUserIds);
    }

    public static PermissionRow ownedBy(String rowId, String ownerUserId, String organizationId) {
        return new PermissionRow(rowId, ownerUserId, organizationId, Set.of(), Set.of());
    }

    private static Set<String> immutableSet(Set<String> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
