package com.coreib.server;

import com.coreib.security.ColumnAccess;
import com.coreib.security.CoreIbAuthorizationService;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionSnapshot;
import com.coreib.security.ResourcePermission;
import com.coreib.security.RowScope;
import com.coreib.server.api.CoreIbSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionControllerTest {
    @Test
    void installsBackendAuthorizationFacadeWithTheDefaultFailClosedProvider() {
        new ApplicationContextRunner()
                .withUserConfiguration(CoreIbSecurityConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CurrentPermissionProvider.class);
                    assertThat(context).hasSingleBean(CoreIbAuthorizationService.class);
                });
    }

    @Test
    void doesNotInstallAnonymousProviderWhenJdbcPermissionsAreEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(CoreIbSecurityConfiguration.class)
                .withPropertyValues("coreib.security.jdbc.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(CurrentPermissionProvider.class));
    }

    @Test
    void usesAuthenticationProviderWhenOneIsInstalled() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestProviderConfiguration.class, CoreIbSecurityConfiguration.class)
                .run(context -> {
                    CurrentPermissionProvider provider = context.getBean(CurrentPermissionProvider.class);
                    assertThat(provider.current().authenticated()).isTrue();
                    assertThat(provider.current().resources()).containsKey("business-data");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestProviderConfiguration {
        @Bean
        CurrentPermissionProvider permissionProvider() {
            return () -> new PermissionSnapshot(
                    true,
                    "info-leader-1",
                    Set.of("information-leader"),
                    Map.of("business-data", new ResourcePermission(
                            Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                            RowScope.ORGANIZATION,
                            Map.of("amount", ColumnAccess.WRITE))));
        }
    }
}
