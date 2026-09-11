package com.coreib.data;

/**
 * Database-specific behavior belongs here, never in a business module.
 * Implementations should keep the common domain SQL portable.
 */
public interface DatabaseDialect {
    DatabaseVendor vendor();

    String quoteIdentifier(String identifier);

    /** Applies vendor syntax to a query that already has a deterministic ORDER BY clause. */
    String pagination(String orderedSql, int offset, int limit);
}
