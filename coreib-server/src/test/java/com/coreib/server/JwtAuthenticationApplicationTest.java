package com.coreib.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "coreib.security.authentication.mode=JWT",
        "coreib.security.authentication.principal-claim=preferred_username"
})
@Import(JwtAuthenticationApplicationTest.JwtTestConfiguration.class)
class JwtAuthenticationApplicationTest {
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
    void protectedApiReturnsJsonUnauthorizedWithoutAToken() throws Exception {
        mvc.perform(get("/api/v1/permissions/effective"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void mapsTheConfiguredPrincipalClaimAndDoesNotCreateASession() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/session")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("JWT"))
                .andExpect(jsonPath("$.identityAuthenticated").value(true))
                .andExpect(jsonPath("$.platformSubjectResolved").value(false))
                .andExpect(jsonPath("$.identityName").value("alice"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("issuer-subject")
                    .claim("preferred_username", "alice")
                    .issuedAt(Instant.now().minusSeconds(30))
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
        }
    }
}
