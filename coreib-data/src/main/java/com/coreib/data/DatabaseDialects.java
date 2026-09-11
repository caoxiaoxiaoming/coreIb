package com.coreib.data;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Registry for the database profiles certified by a deployment. */
public final class DatabaseDialects {
    private DatabaseDialects() {
    }

    public static Map<DatabaseVendor, DatabaseDialect> defaults() {
        Map<DatabaseVendor, DatabaseDialect> dialects = new EnumMap<>(DatabaseVendor.class);
        dialects.put(DatabaseVendor.POSTGRESQL, new SimpleDialect(DatabaseVendor.POSTGRESQL, '"', '"',
                (sql, offset, limit) -> sql + " LIMIT " + limit + " OFFSET " + offset));
        dialects.put(DatabaseVendor.SQL_SERVER, new SimpleDialect(DatabaseVendor.SQL_SERVER, '[', ']',
                (sql, offset, limit) -> sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY"));
        dialects.put(DatabaseVendor.ORACLE, new SimpleDialect(DatabaseVendor.ORACLE, '"', '"',
                (sql, offset, limit) -> sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY"));
        return Map.copyOf(dialects);
    }

    @FunctionalInterface
    private interface Pagination {
        String apply(String sql, int offset, int limit);
    }

    private record SimpleDialect(DatabaseVendor vendor, char openQuote, char closeQuote, Pagination pagination)
            implements DatabaseDialect {
        @Override
        public String quoteIdentifier(String identifier) {
            if (identifier == null || identifier.isBlank()
                    || identifier.indexOf(openQuote) >= 0 || identifier.indexOf(closeQuote) >= 0) {
                throw new IllegalArgumentException("Invalid database identifier");
            }
            return openQuote == '[' ? "[" + identifier + "]" : openQuote + identifier + closeQuote;
        }

        @Override
        public String pagination(String orderedSql, int offset, int limit) {
            if (orderedSql == null || orderedSql.isBlank() || offset < 0 || limit <= 0) {
                throw new IllegalArgumentException("Invalid pagination arguments");
            }
            if (!orderedSql.toUpperCase(Locale.ROOT).contains("ORDER BY")) {
                throw new IllegalArgumentException("Pagination requires deterministic ORDER BY");
            }
            return pagination.apply(orderedSql, offset, limit);
        }
    }
}
