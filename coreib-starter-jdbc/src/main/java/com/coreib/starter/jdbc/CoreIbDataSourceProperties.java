package com.coreib.starter.jdbc;

import com.coreib.data.DatabaseVendor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Typed, vendor-neutral connection pool settings for an enabled deployment. */
@ConfigurationProperties(prefix = "coreib.datasource")
public class CoreIbDataSourceProperties {
    private boolean enabled;
    private DatabaseVendor vendor;
    private String jdbcUrl;
    private String username;
    private String password;
    private String poolName = "coreIbHikariPool";
    private int maximumPoolSize = 20;
    private int minimumIdle = 2;
    private Duration connectionTimeout = Duration.ofSeconds(30);
    private Duration validationTimeout = Duration.ofSeconds(5);
    private Duration idleTimeout = Duration.ofMinutes(10);
    private Duration maxLifetime = Duration.ofMinutes(30);
    private long initializationFailTimeout = 1;

    public void validate() {
        if (!enabled) {
            return;
        }
        if (vendor == null) {
            throw new IllegalArgumentException("coreib.datasource.vendor is required when the datasource is enabled");
        }
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("coreib.datasource.jdbc-url is required when the datasource is enabled");
        }
        if (!vendor.acceptsJdbcUrl(jdbcUrl)) {
            throw new IllegalArgumentException("JDBC URL does not match database vendor " + vendor.id());
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("coreib.datasource.username is required when the datasource is enabled");
        }
        if (poolName == null || poolName.isBlank()) {
            throw new IllegalArgumentException("coreib.datasource.pool-name must not be blank");
        }
        if (maximumPoolSize < 1 || minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("coreib.datasource pool sizes are invalid");
        }
        requirePositive("connection-timeout", connectionTimeout);
        requirePositive("validation-timeout", validationTimeout);
        requirePositive("idle-timeout", idleTimeout);
        requirePositive("max-lifetime", maxLifetime);
        if (connectionTimeout.toMillis() < 250 || validationTimeout.toMillis() < 250
                || idleTimeout.toMillis() < 10_000 || maxLifetime.toMillis() < 30_000) {
            throw new IllegalArgumentException("coreib.datasource timeout values are below HikariCP minimums");
        }
        if (initializationFailTimeout < -1) {
            throw new IllegalArgumentException("coreib.datasource.initialization-fail-timeout must be -1 or greater");
        }
    }

    private static void requirePositive(String name, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("coreib.datasource." + name + " must be positive");
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public DatabaseVendor getVendor() { return vendor; }
    public void setVendor(DatabaseVendor vendor) { this.vendor = vendor; }
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPoolName() { return poolName; }
    public void setPoolName(String poolName) { this.poolName = poolName; }
    public int getMaximumPoolSize() { return maximumPoolSize; }
    public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
    public int getMinimumIdle() { return minimumIdle; }
    public void setMinimumIdle(int minimumIdle) { this.minimumIdle = minimumIdle; }
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(Duration connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public Duration getValidationTimeout() { return validationTimeout; }
    public void setValidationTimeout(Duration validationTimeout) { this.validationTimeout = validationTimeout; }
    public Duration getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(Duration idleTimeout) { this.idleTimeout = idleTimeout; }
    public Duration getMaxLifetime() { return maxLifetime; }
    public void setMaxLifetime(Duration maxLifetime) { this.maxLifetime = maxLifetime; }
    public long getInitializationFailTimeout() { return initializationFailTimeout; }
    public void setInitializationFailTimeout(long initializationFailTimeout) { this.initializationFailTimeout = initializationFailTimeout; }
}
