package com.coreib.server.api;

import com.coreib.platform.AuditSink;
import com.coreib.platform.PlatformDirectoryCommands;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@Profile("!demo")
@ConditionalOnBean(DataSource.class)
public class JdbcPlatformCommandConfiguration {
    @Bean
    @ConditionalOnMissingBean(PlatformDirectoryCommands.class)
    PlatformDirectoryCommands jdbcPlatformDirectoryCommands(DataSource dataSource) {
        return new JdbcPlatformDirectoryCommands(new JdbcTemplate(dataSource));
    }

    @Bean
    @ConditionalOnMissingBean(AuditSink.class)
    AuditSink jdbcAuditSink(DataSource dataSource) {
        return new JdbcAuditSink(new JdbcTemplate(dataSource));
    }
}
