package com.coreib.server.api;

import com.coreib.security.CurrentAuthorizationProvider;
import com.coreib.security.CurrentPermissionProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.annotation.RequestScope;

import javax.sql.DataSource;

/** Enables database-backed permissions only when a real authentication adapter opts in. */
@Configuration(proxyBeanMethods = false)
@Profile("!demo")
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "coreib.security.jdbc", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CoreIbJdbcPermissionProperties.class)
public class JdbcPermissionSnapshotConfiguration {
    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
    @ConditionalOnMissingBean(CurrentPermissionProvider.class)
    CurrentAuthorizationProvider jdbcPermissionSnapshotProvider(
            HttpServletRequest request,
            DataSource dataSource,
            CoreIbJdbcPermissionProperties properties) {
        return new JdbcPermissionSnapshotProvider(
                request,
                new JdbcTemplate(dataSource),
                properties.getPrincipalLookup());
    }
}
