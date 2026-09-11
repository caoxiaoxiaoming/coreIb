package com.coreib.server.api;

import com.coreib.security.CoreIbAuthorizationService;
import com.coreib.security.CurrentAuthorizationProvider;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionPolicy;
import com.coreib.security.PermissionSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

/** Default security integration point until an authentication module supplies a provider. */
@Configuration
public class CoreIbSecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(CurrentPermissionProvider.class)
    @ConditionalOnProperty(prefix = "coreib.security.jdbc", name = "enabled", havingValue = "false", matchIfMissing = true)
    CurrentAuthorizationProvider anonymousPermissionProvider() {
        return new CurrentAuthorizationProvider() {
            @Override
            public PermissionSnapshot current() {
                return PermissionSnapshot.anonymous();
            }

            @Override
            public Collection<PermissionPolicy> policies(String resource) {
                return List.of();
            }
        };
    }

    @Bean
    @ConditionalOnBean(CurrentAuthorizationProvider.class)
    @ConditionalOnMissingBean(CoreIbAuthorizationService.class)
    CoreIbAuthorizationService coreIbAuthorizationService(CurrentAuthorizationProvider provider) {
        return new CoreIbAuthorizationService(provider);
    }
}
