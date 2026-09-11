package com.coreib.server.api;

import com.coreib.platform.SecurityAdministration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class SecurityAdministrationConfiguration {
    @Bean
    @Profile("demo")
    SecurityAdministration demoSecurityAdministration() {
        return new DemoSecurityAdministration();
    }

    @Bean
    @Profile("!demo")
    @ConditionalOnMissingBean(value = {SecurityAdministration.class, DataSource.class})
    SecurityAdministration unavailableSecurityAdministration() {
        return new UnavailableSecurityAdministration();
    }
}
