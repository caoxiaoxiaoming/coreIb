package com.coreib.security;

import java.util.Map;
import java.util.Set;

/**
 * Named policy examples used by documentation and integration tests.
 * Production policies must be loaded from the authorization store.
 */
public final class PermissionPolicyPresets {
    private PermissionPolicyPresets() {
    }

    public static PermissionPolicy nursePatients() {
        return new PermissionPolicy(
                "patient",
                Set.of(PermissionAction.READ),
                RowScope.RELATED_RECORDS,
                Map.of("name", ColumnAccess.READ, "idCard", ColumnAccess.MASKED));
    }

    public static PermissionPolicy headNursePatients() {
        return new PermissionPolicy(
                "patient",
                Set.of(PermissionAction.READ),
                RowScope.MANAGED_USERS,
                Map.of("name", ColumnAccess.READ, "idCard", ColumnAccess.MASKED));
    }

    public static PermissionPolicy informationClerkBusinessData() {
        return new PermissionPolicy(
                "business-data",
                Set.of(PermissionAction.READ),
                RowScope.ORGANIZATION,
                Map.of());
    }

    public static PermissionPolicy informationLeaderBusinessData() {
        return new PermissionPolicy(
                "business-data",
                Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                RowScope.ORGANIZATION,
                Map.of());
    }
}
