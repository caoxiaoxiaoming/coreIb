package com.coreib.security;

import java.util.Map;
import java.util.Set;

/** Effective permissions exposed to a UI shell; row predicates remain server-side. */
public record PermissionSnapshot(
        boolean authenticated,
        String subjectId,
        Set<String> roleCodes,
        Map<String, ResourcePermission> resources) {

    public PermissionSnapshot {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must not be blank");
        }
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
        resources = resources == null ? Map.of() : Map.copyOf(resources);
    }

    public static PermissionSnapshot anonymous() {
        return new PermissionSnapshot(false, "anonymous", Set.of(), Map.of());
    }
}
