package com.coreib.starter.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, HttpSecurity.class})
@EnableConfigurationProperties(CoreIbAuthenticationProperties.class)
public class CoreIbAuthenticationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain coreIbSecurityFilterChain(
            HttpSecurity http,
            CoreIbAuthenticationProperties properties) throws Exception {
        properties.validate();
        boolean authenticationEnabled = properties.getMode() != CoreIbAuthenticationMode.DISABLED;

        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(properties.getPublicPaths().toArray(String[]::new)).permitAll();
            if (authenticationEnabled) {
                authorize.anyRequest().authenticated();
            } else {
                authorize.anyRequest().permitAll();
            }
        });
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                "AUTHENTICATION_REQUIRED", "Authentication is required"))
                .accessDeniedHandler((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                "ACCESS_DENIED", "Access is denied")));
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);

        switch (properties.getMode()) {
            case DISABLED -> {
                http.csrf(AbstractHttpConfigurer::disable);
                http.requestCache(AbstractHttpConfigurer::disable);
            }
            case JWT -> configureJwt(http, properties);
            case OIDC -> configureOidc(http, properties);
        }
        return http.build();
    }

    private static void configureJwt(
            HttpSecurity http,
            CoreIbAuthenticationProperties properties) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(properties.getPrincipalClaim());
        http.csrf(AbstractHttpConfigurer::disable);
        http.requestCache(AbstractHttpConfigurer::disable);
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.logout(AbstractHttpConfigurer::disable);
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(converter)));
    }

    private static void configureOidc(
            HttpSecurity http,
            CoreIbAuthenticationProperties properties) throws Exception {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        http.csrf(csrf -> csrf
                .csrfTokenRepository(repository)
                .csrfTokenRequestHandler(requestHandler));
        http.addFilterAfter(new CoreIbCsrfCookieFilter(), BasicAuthenticationFilter.class);
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        http.oauth2Login(login -> login
                .defaultSuccessUrl(properties.getLoginSuccessUrl(), false));
        http.logout(logout -> logout
                .logoutUrl(properties.logoutUrl())
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
