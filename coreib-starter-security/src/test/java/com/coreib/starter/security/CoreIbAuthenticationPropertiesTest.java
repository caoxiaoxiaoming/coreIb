package com.coreib.starter.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreIbAuthenticationPropertiesTest {
    @Test
    void derivesOidcEndpointsFromTheRegistration() {
        CoreIbAuthenticationProperties properties = new CoreIbAuthenticationProperties();
        properties.setMode(CoreIbAuthenticationMode.OIDC);
        properties.setOidcRegistrationId("hospital-sso");

        properties.validate();

        assertThat(properties.loginUrl()).isEqualTo("/oauth2/authorization/hospital-sso");
        assertThat(properties.logoutUrl()).isEqualTo("/api/v1/auth/logout");
    }

    @Test
    void rejectsExternalSuccessRedirects() {
        CoreIbAuthenticationProperties properties = new CoreIbAuthenticationProperties();
        properties.setMode(CoreIbAuthenticationMode.OIDC);
        properties.setLoginSuccessUrl("https://malicious.example");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("application-local path");
    }

    @Test
    void requiresAJwtPrincipalClaim() {
        CoreIbAuthenticationProperties properties = new CoreIbAuthenticationProperties();
        properties.setMode(CoreIbAuthenticationMode.JWT);
        properties.setPrincipalClaim(" ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("principal-claim");
    }
}
