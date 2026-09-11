package com.coreib.security;

import java.util.Map;
import java.util.Set;

/** A resource policy resolved from one role or an equivalent authorization source. */
public record PermissionPolicy(
        String resource,
        Set<PermissionAction> actions,
        RowScope rowScope,
        Map<String, ColumnAccess> fields,
        String policyKey) {

    public PermissionPolicy(
            String resource,
            Set<PermissionAction> actions,
            RowScope rowScope,
            Map<String, ColumnAccess> fields) {
        this(resource, actions, rowScope, fields, null);
    }

    public PermissionPolicy {
        requireText("resource", resource);
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        if (rowScope == null) {
            throw new IllegalArgumentException("rowScope must not be null");
        }
        actions = Set.copyOf(actions);
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        policyKey = policyKey == null || policyKey.isBlank() ? null : policyKey.trim();
    }

    public boolean allows(PermissionAction action) {
        return action != null && actions.contains(action);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
