package com.coreib.starter.jdbc;

import java.util.List;

/** Parameterized SQL predicate produced from trusted backend authorization facts. */
public record JdbcRowAccessPredicate(String sql, List<Object> parameters) {
    public JdbcRowAccessPredicate {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
