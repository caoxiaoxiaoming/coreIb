package com.coreib.data;

/** First-class database profiles supported by the platform. */
public enum DatabaseVendor {
    POSTGRESQL("postgresql", "jdbc:postgresql:", "org.postgresql.Driver", "SELECT 1"),
    SQL_SERVER("sql-server", "jdbc:sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "SELECT 1"),
    ORACLE("oracle", "jdbc:oracle:", "oracle.jdbc.OracleDriver", "SELECT 1 FROM DUAL");

    private final String id;
    private final String jdbcUrlPrefix;
    private final String driverClassName;
    private final String validationQuery;

    DatabaseVendor(String id, String jdbcUrlPrefix, String driverClassName, String validationQuery) {
        this.id = id;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        this.driverClassName = driverClassName;
        this.validationQuery = validationQuery;
    }

    public String id() {
        return id;
    }

    public String jdbcUrlPrefix() {
        return jdbcUrlPrefix;
    }

    public String driverClassName() {
        return driverClassName;
    }

    public String validationQuery() {
        return validationQuery;
    }

    public boolean acceptsJdbcUrl(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.regionMatches(true, 0, jdbcUrlPrefix, 0, jdbcUrlPrefix.length());
    }

    public static DatabaseVendor fromId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Database vendor is required");
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        for (DatabaseVendor vendor : values()) {
            if (vendor.id.equals(normalized) || vendor.name().equalsIgnoreCase(normalized.replace('-', '_'))) {
                return vendor;
            }
        }
        throw new IllegalArgumentException("Unsupported database vendor: " + value);
    }

    public static DatabaseVendor fromJdbcUrl(String jdbcUrl) {
        for (DatabaseVendor vendor : values()) {
            if (vendor.acceptsJdbcUrl(jdbcUrl)) {
                return vendor;
            }
        }
        throw new IllegalArgumentException("Unsupported JDBC URL vendor");
    }
}
