package com.coreib.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseDialectsTest {
    private final Map<DatabaseVendor, DatabaseDialect> dialects = DatabaseDialects.defaults();

    @Test
    void providesEveryFirstClassDatabase() {
        assertEquals(DatabaseVendor.values().length, dialects.size());
    }

    @Test
    void appliesVendorPaginationSyntax() {
        String orderedSql = "SELECT id FROM core_item ORDER BY id";

        assertEquals(orderedSql + " LIMIT 20 OFFSET 10",
                dialects.get(DatabaseVendor.POSTGRESQL).pagination(orderedSql, 10, 20));
        assertEquals(orderedSql + " OFFSET 10 ROWS FETCH NEXT 20 ROWS ONLY",
                dialects.get(DatabaseVendor.SQL_SERVER).pagination(orderedSql, 10, 20));
        assertEquals(orderedSql + " OFFSET 10 ROWS FETCH NEXT 20 ROWS ONLY",
                dialects.get(DatabaseVendor.ORACLE).pagination(orderedSql, 10, 20));
    }

    @Test
    void rejectsUnorderedPagination() {
        DatabaseDialect dialect = dialects.get(DatabaseVendor.POSTGRESQL);
        assertThrows(IllegalArgumentException.class,
                () -> dialect.pagination("SELECT id FROM core_item", 0, 20));
    }

    @Test
    void quotesIdentifiersPerVendor() {
        assertEquals("\"core_item\"", dialects.get(DatabaseVendor.POSTGRESQL).quoteIdentifier("core_item"));
        assertEquals("[core_item]", dialects.get(DatabaseVendor.SQL_SERVER).quoteIdentifier("core_item"));
        assertEquals("\"core_item\"", dialects.get(DatabaseVendor.ORACLE).quoteIdentifier("core_item"));
    }

    @Test
    void rejectsClosingIdentifierDelimiters() {
        assertThrows(IllegalArgumentException.class,
                () -> dialects.get(DatabaseVendor.SQL_SERVER).quoteIdentifier("core]item"));
    }
}
