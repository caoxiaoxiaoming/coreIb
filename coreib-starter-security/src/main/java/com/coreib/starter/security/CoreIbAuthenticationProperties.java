package com.coreib.starter.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("coreib.security.authentication")
public class CoreIbAuthenticationProperties {
    private CoreIbAuthenticationMode mode = CoreIbAuthenticationMode.DISABLED;
    private String principalClaim = "sub";
    private String oidcRegistrationId = "coreib";
    private String loginSuccessUrl = "/";
    private String logoutSuccessUrl = "/";
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/",
            "/index.html",
            "/assets/**",
            "/favicon.ico",
            "/error",
            "/api/v1/auth/session",
            "/api/v1/system/info",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/login/**",
            "/oauth2/**"));

    public void validate() {
        if (mode == null) {
            throw new IllegalStateException("coreib.security.authentication.mode must be configured");
        }
        if (mode == CoreIbAuthenticationMode.JWT) {
            requireText("principal-claim", principalClaim);
        }
        if (mode == CoreIbAuthenticationMode.OIDC) {
            requireText("oidc-registration-id", oidcRegistrationId);
            requireLocalPath("login-success-url", loginSuccessUrl);
            requireLocalPath("logout-success-url", logoutSuccessUrl);
        }
        if (publicPaths == null || publicPaths.stream().anyMatch(path -> path == null || path.isBlank())) {
            throw new IllegalStateException("coreib.security.authentication.public-paths must not contain blanks");
        }
    }

    public String loginUrl() {
        return mode == CoreIbAuthenticationMode.OIDC
                ? "/oauth2/authorization/" + oidcRegistrationId
                : null;
    }

    public String logoutUrl() {
        return mode == CoreIbAuthenticationMode.OIDC ? "/api/v1/auth/logout" : null;
    }

    public CoreIbAuthenticationMode getMode() {
        return mode;
    }

    public void setMode(CoreIbAuthenticationMode mode) {
        this.mode = mode;
    }

    public String getPrincipalClaim() {
        return principalClaim;
    }

    public void setPrincipalClaim(String principalClaim) {
        this.principalClaim = principalClaim;
    }

    public String getOidcRegistrationId() {
        return oidcRegistrationId;
    }

    public void setOidcRegistrationId(String oidcRegistrationId) {
        this.oidcRegistrationId = oidcRegistrationId;
    }

    public String getLoginSuccessUrl() {
        return loginSuccessUrl;
    }

    public void setLoginSuccessUrl(String loginSuccessUrl) {
        this.loginSuccessUrl = loginSuccessUrl;
    }

    public String getLogoutSuccessUrl() {
        return logoutSuccessUrl;
    }

    public void setLogoutSuccessUrl(String logoutSuccessUrl) {
        this.logoutSuccessUrl = logoutSuccessUrl;
    }

    public List<String> getPublicPaths() {
        return List.copyOf(publicPaths);
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths == null ? null : new ArrayList<>(publicPaths);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("coreib.security.authentication." + name + " must not be blank");
        }
    }

    private static void requireLocalPath(String name, String value) {
        requireText(name, value);
        if (!value.startsWith("/") || value.startsWith("//")) {
            throw new IllegalStateException(
                    "coreib.security.authentication." + name + " must be an application-local path");
        }
    }
}
