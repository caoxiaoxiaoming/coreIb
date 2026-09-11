package com.coreib.starter.jdbc;

import java.util.regex.Pattern;

/** Trusted business-table column mapping used by the row predicate compiler. */
public record JdbcRowColumns(
        String rowId,
        String ownerUserId,
        String organizationId) {
    private static final Pattern QUALIFIED_IDENTIFIER = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    public JdbcRowColumns {
        rowId = requireIdentifier("rowId", rowId);
        ownerUserId = requireIdentifier("ownerUserId", ownerUserId);
        organizationId = requireIdentifier("organizationId", organizationId);
    }

    private static String requireIdentifier(String name, String value) {
        if (value == null || !QUALIFIED_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be a simple SQL identifier");
        }
        return value;
    }
}
