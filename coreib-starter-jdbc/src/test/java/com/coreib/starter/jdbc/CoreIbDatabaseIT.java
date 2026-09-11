package com.coreib.starter.jdbc;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs only under the Maven integration profile against a real database.
 * The three CI jobs provide coreib.it.* system properties for their vendor.
 */
@SpringBootTest(classes = CoreIbDatabaseIT.TestApplication.class)
class CoreIbDatabaseIT {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private SpringLiquibase liquibase;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("coreib.datasource.enabled", () -> true);
        registry.add("coreib.datasource.vendor", () -> required("coreib.it.vendor"));
        registry.add("coreib.datasource.jdbc-url", () -> required("coreib.it.url"));
        registry.add("coreib.datasource.username", () -> required("coreib.it.username"));
        registry.add("coreib.datasource.password", () -> System.getProperty("coreib.it.password", ""));
        registry.add("coreib.datasource.minimum-idle", () -> 0);
        registry.add("coreib.datasource.initialization-fail-timeout", () -> 1);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    @Test
    void appliesPlatformBaselineAndRecordsChangeSet() throws Exception {
        assertThat(liquibase).isNotNull();
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM coreib_platform_metadata WHERE id = 'coreib'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void rejectsAuthorizationRowsWithUnknownUsersAndRoles() {
        String suffix = UUID.randomUUID().toString();
        assertThatThrownBy(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement("""
                         INSERT INTO coreib_sec_user_role (id, user_id, role_code, created_at)
                         VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                         """)) {
                statement.setString(1, "it-" + suffix);
                statement.setString(2, "missing-user-" + suffix);
                statement.setString(3, "missing-role-" + suffix);
                statement.executeUpdate();
            }
        }).isInstanceOf(SQLException.class);
    }

    private static String required(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing integration property: " + name);
        }
        return value;
    }

    @org.springframework.boot.autoconfigure.SpringBootApplication(
            excludeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
    @Import({CoreIbJdbcAutoConfiguration.class, CoreIbLiquibaseAutoConfiguration.class})
    static class TestApplication {
    }
}
