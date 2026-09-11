package com.coreib.security;

import java.util.Set;

/** Request subject facts resolved by authentication and organization services. */
public record PermissionSubject(
        String userId,
        String organizationId,
        Set<String> visibleOrganizationIds,
        Set<String> managedUserIds,
        Set<String> relatedRecordIds) {

    public PermissionSubject {
        requireText("userId", userId);
        visibleOrganizationIds = immutableSet(visibleOrganizationIds);
        managedUserIds = immutableSet(managedUserIds);
        relatedRecordIds = immutableSet(relatedRecordIds);
    }

    public static PermissionSubject of(String userId, String organizationId) {
        return new PermissionSubject(userId, organizationId, Set.of(), Set.of(), Set.of());
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
