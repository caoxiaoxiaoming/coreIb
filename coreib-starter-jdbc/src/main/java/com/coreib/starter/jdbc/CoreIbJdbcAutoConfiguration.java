package com.coreib.starter.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
@ConditionalOnClass({HikariDataSource.class, DataSource.class})
@EnableConfigurationProperties(CoreIbDataSourceProperties.class)
public class CoreIbJdbcAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "coreib.datasource", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(DataSource.class)
    HikariDataSource coreIbDataSource(CoreIbDataSourceProperties properties) {
        properties.validate();

        HikariConfig config = new HikariConfig();
        config.setPoolName(properties.getPoolName());
        config.setJdbcUrl(properties.getJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getVendor().driverClassName());
        config.setConnectionTestQuery(properties.getVendor().validationQuery());
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        config.setConnectionTimeout(properties.getConnectionTimeout().toMillis());
        config.setValidationTimeout(properties.getValidationTimeout().toMillis());
        config.setIdleTimeout(properties.getIdleTimeout().toMillis());
        config.setMaxLifetime(properties.getMaxLifetime().toMillis());
        config.setInitializationFailTimeout(properties.getInitializationFailTimeout());
        return new HikariDataSource(config);
    }
}
