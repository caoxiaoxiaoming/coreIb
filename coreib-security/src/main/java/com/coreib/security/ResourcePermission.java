package com.coreib.security;

import java.util.Map;
import java.util.Set;

/** Frontend-safe effective permission projection for one resource. */
public record ResourcePermission(
        Set<PermissionAction> actions,
        RowScope rowScope,
        Map<String, ColumnAccess> fields) {

    public ResourcePermission {
        actions = actions == null ? Set.of() : Set.copyOf(actions);
        if (rowScope == null) {
            throw new IllegalArgumentException("rowScope must not be null");
        }
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
