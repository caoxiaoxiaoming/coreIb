package com.coreib.server.api;

import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionSnapshot;
import com.coreib.starter.security.CoreIbAuthenticationMode;
import com.coreib.starter.security.CoreIbAuthenticationProperties;
import com.coreib.platform.PlatformManagement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/** Authentication state consumed by the same-origin Vue shell. */
@RestController
@RequestMapping("/api/v1/auth")
public class CoreIbAuthenticationController {
    private final CoreIbAuthenticationProperties properties;
    private final CurrentPermissionProvider permissionProvider;
    private final PlatformManagement platformManagement;

    public CoreIbAuthenticationController(
            CoreIbAuthenticationProperties properties,
            CurrentPermissionProvider permissionProvider,
            PlatformManagement platformManagement) {
        this.properties = properties;
        this.permissionProvider = permissionProvider;
        this.platformManagement = platformManagement;
    }

    @GetMapping("/session")
    public AuthenticationSessionResponse session(Principal principal, HttpServletRequest request) {
        PermissionSnapshot permissions = permissionProvider.current();
        boolean identityAuthenticated = principal != null;
        String identityName = identityAuthenticated ? principal.getName() : permissions.subjectId();
        platformManagement.appendLoginEvent(new PlatformManagement.LoginEvent(
                UUID.randomUUID().toString(), permissions.subjectId(), identityName,
                request.getRemoteAddr(), permissions.authenticated() || identityAuthenticated ? "SUCCESS" : "ANONYMOUS",
                properties.getMode().name(), Instant.now()));
        return new AuthenticationSessionResponse(
                properties.getMode(),
                identityAuthenticated,
                permissions.authenticated(),
                identityName,
                permissions.subjectId(),
                properties.loginUrl(),
                properties.logoutUrl(),
                properties.getMode() == CoreIbAuthenticationMode.OIDC
                        ? properties.getLogoutSuccessUrl()
                        : null,
                properties.getMode() == CoreIbAuthenticationMode.OIDC ? "XSRF-TOKEN" : null,
                properties.getMode() == CoreIbAuthenticationMode.OIDC ? "X-XSRF-TOKEN" : null);
    }

    public record AuthenticationSessionResponse(
            CoreIbAuthenticationMode mode,
            boolean identityAuthenticated,
            boolean platformSubjectResolved,
            String identityName,
            String subjectId,
            String loginUrl,
            String logoutUrl,
            String postLogoutRedirectUrl,
            String csrfCookieName,
            String csrfHeaderName) {
    }
}
