package com.coreib.server.api;

import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.PlatformDirectory;
import com.coreib.platform.PlatformDirectoryCommands;
import com.coreib.platform.AuditSink;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import javax.sql.DataSource;

import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class PlatformDirectoryConfiguration {
    @Bean
    @Profile("demo")
    DemoPlatformDirectory demoPlatformDirectoryStore() {
        return new DemoPlatformDirectory();
    }

    @Bean
    @Profile("demo")
    AuditSink demoAuditSink() {
        return new NoopAuditSink();
    }
    @Bean
    @Profile("!demo")
    @ConditionalOnMissingBean(value = {PlatformDirectoryCommands.class, DataSource.class})
    PlatformDirectoryCommands unavailablePlatformDirectoryCommands() {
        return new UnavailablePlatformDirectoryCommands();
    }

    @Bean
    @Profile("!demo")
    @ConditionalOnMissingBean(value = {AuditSink.class, DataSource.class})
    AuditSink unavailableAuditSink() {
        return new NoopAuditSink();
    }

    @Bean
    @Profile("!demo")
    @ConditionalOnMissingBean(value = {PlatformDirectory.class, DataSource.class})
    PlatformDirectory emptyPlatformDirectory() {
        return new PlatformDirectory() {
            @Override
            public List<OrganizationSummary> organizations() {
                return List.of();
            }

            @Override
            public List<UserSummary> users() {
                return List.of();
            }

            @Override
            public List<RoleSummary> roles() {
                return List.of();
            }
        };
    }
}
