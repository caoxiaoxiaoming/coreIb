package com.coreib.server;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "coreib.security.authentication.mode=OIDC",
        "coreib.security.authentication.oidc-registration-id=coreib",
        "coreib.security.authentication.logout-success-url=/signed-out",
        "spring.security.oauth2.client.registration.coreib.client-id=test-client",
        "spring.security.oauth2.client.registration.coreib.client-secret=test-secret",
        "spring.security.oauth2.client.registration.coreib.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.coreib.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.registration.coreib.scope=openid",
        "spring.security.oauth2.client.registration.coreib.provider=coreib",
        "spring.security.oauth2.client.provider.coreib.authorization-uri=https://identity.example/authorize",
        "spring.security.oauth2.client.provider.coreib.token-uri=https://identity.example/token",
        "spring.security.oauth2.client.provider.coreib.jwk-set-uri=https://identity.example/jwks",
        "spring.security.oauth2.client.provider.coreib.user-info-uri=https://identity.example/userinfo",
        "spring.security.oauth2.client.provider.coreib.user-name-attribute=sub"
})
class OidcAuthenticationApplicationTest {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedApiUsesJsonInsteadOfAnOidcRedirect() throws Exception {
        mvc.perform(get("/api/v1/permissions/effective"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void publicSessionIssuesCsrfCookieAndPublishesOidcEndpoints() throws Exception {
        mvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.mode").value("OIDC"))
                .andExpect(jsonPath("$.loginUrl").value("/oauth2/authorization/coreib"))
                .andExpect(jsonPath("$.logoutUrl").value("/api/v1/auth/logout"))
                .andExpect(jsonPath("$.postLogoutRedirectUrl").value("/signed-out"));
    }

    @Test
    void reportsIdentityAndPlatformResolutionSeparately() throws Exception {
        mvc.perform(get("/api/v1/auth/session").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityAuthenticated").value(true))
                .andExpect(jsonPath("$.platformSubjectResolved").value(false));
    }

    @Test
    void rejectsWritesWithoutCsrfAndAllowsProtectedLogoutWithCsrf() throws Exception {
        mvc.perform(post("/api/v1/platform/directory/organizations")
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        MvcResult session = mvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie csrfCookie = session.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        mvc.perform(post("/api/v1/auth/logout")
                        .with(oauth2Login())
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }
}
