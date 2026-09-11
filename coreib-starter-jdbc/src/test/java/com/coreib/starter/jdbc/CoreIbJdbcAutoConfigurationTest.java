package com.coreib.starter.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class CoreIbJdbcAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoreIbJdbcAutoConfiguration.class));

    @Test
    void remainsDisabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
    }

    @Test
    void createsConfiguredPoolWithoutOpeningAConnection() {
        contextRunner
                .withPropertyValues(
                        "coreib.datasource.enabled=true",
                        "coreib.datasource.vendor=postgresql",
                        "coreib.datasource.jdbc-url=jdbc:postgresql://localhost:5432/coreib",
                        "coreib.datasource.username=coreib",
                        "coreib.datasource.minimum-idle=0",
                        "coreib.datasource.initialization-fail-timeout=-1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    HikariDataSource dataSource = context.getBean(HikariDataSource.class);
                    assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/coreib");
                    assertThat(dataSource.getDriverClassName()).isEqualTo("org.postgresql.Driver");
                    assertThat(dataSource.getConnectionTestQuery()).isEqualTo("SELECT 1");
                });
    }

    @Test
    void rejectsVendorAndUrlMismatch() {
        contextRunner
                .withPropertyValues(
                        "coreib.datasource.enabled=true",
                        "coreib.datasource.vendor=oracle",
                        "coreib.datasource.jdbc-url=jdbc:postgresql://localhost:5432/coreib",
                        "coreib.datasource.username=coreib")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .hasRootCauseMessage("JDBC URL does not match database vendor oracle"));
    }
}
