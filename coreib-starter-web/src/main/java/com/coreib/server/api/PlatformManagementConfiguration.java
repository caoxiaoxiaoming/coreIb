package com.coreib.server.api;

import com.coreib.platform.PlatformManagement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class PlatformManagementConfiguration {
    @Bean
    @Profile("demo")
    PlatformManagement demoPlatformManagement() {
        return new DemoPlatformManagement();
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("!demo")
    @ConditionalOnBean(DataSource.class)
    static class JdbcConfiguration {
        @Bean
        @ConditionalOnMissingBean(PlatformManagement.class)
        PlatformManagement jdbcPlatformManagement(DataSource dataSource) {
            return new JdbcPlatformManagement(new JdbcTemplate(dataSource));
        }
    }

    @Bean
    @Profile("!demo")
    @ConditionalOnMissingBean(value = {PlatformManagement.class, DataSource.class})
    PlatformManagement standalonePlatformManagement() {
        return new DemoPlatformManagement();
    }
}
