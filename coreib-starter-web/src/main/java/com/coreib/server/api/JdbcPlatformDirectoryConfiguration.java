package com.coreib.server.api;

import com.coreib.platform.PlatformDirectory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** Wires the portable directory port to the active database when JDBC is enabled. */
@Configuration(proxyBeanMethods = false)
@Profile("!demo")
@ConditionalOnBean(DataSource.class)
public class JdbcPlatformDirectoryConfiguration {
    @Bean
    @ConditionalOnMissingBean(PlatformDirectory.class)
    PlatformDirectory jdbcPlatformDirectory(DataSource dataSource) {
        return new JdbcPlatformDirectory(new JdbcTemplate(dataSource));
    }
}
