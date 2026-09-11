package com.coreib.server.api;

import com.coreib.platform.SecurityAdministration;
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
public class JdbcSecurityAdministrationConfiguration {
    @Bean
    @ConditionalOnMissingBean(SecurityAdministration.class)
    SecurityAdministration jdbcSecurityAdministration(DataSource dataSource) {
        return new JdbcSecurityAdministration(new JdbcTemplate(dataSource));
    }
}
