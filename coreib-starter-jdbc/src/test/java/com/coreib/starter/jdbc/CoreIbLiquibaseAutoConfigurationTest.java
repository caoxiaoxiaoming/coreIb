package com.coreib.starter.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreIbLiquibaseAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoreIbLiquibaseAutoConfiguration.class))
            .withUserConfiguration(H2DataSourceConfiguration.class)
            .withPropertyValues(
                    "spring.liquibase.enabled=true",
                    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml");

    @Test
    void runsBaselineAgainstUnitTestDatabase() {
        contextRunner.withPropertyValues("spring.liquibase.test-rollback-on-update=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SpringLiquibase.class);
            DataSource dataSource = context.getBean(DataSource.class);
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM coreib_platform_metadata")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isOne();
            }
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM coreib_sys_organization")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM coreib_sys_menu")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            assertThatThrownBy(() -> insertUnknownUserRole(dataSource))
                    .isInstanceOf(SQLException.class);
        });
    }

    @Test
    void canDisableMigrationWithoutRemovingDataSource() {
        contextRunner.withPropertyValues("spring.liquibase.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SpringLiquibase.class);
            assertThat(context).hasSingleBean(DataSource.class);
        });
    }

    private static void insertUnknownUserRole(DataSource dataSource) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO coreib_sec_user_role (id, user_id, role_code, created_at)
                     VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                     """)) {
            statement.setString(1, "invalid-user-role");
            statement.setString(2, "missing-user");
            statement.setString(3, "missing-role");
            statement.executeUpdate();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class H2DataSourceConfiguration {
        @Bean(destroyMethod = "close")
        HikariDataSource dataSource() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:coreib_liquibase;DB_CLOSE_DELAY=-1");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
            return new HikariDataSource(config);
        }
    }
}
