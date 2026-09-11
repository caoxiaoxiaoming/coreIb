package com.coreib.server.api;

import com.coreib.security.ColumnAccess;
import com.coreib.security.CurrentAuthorizationProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionPolicy;
import com.coreib.security.PermissionSnapshot;
import com.coreib.security.PermissionSubject;
import com.coreib.security.ResourcePermission;
import com.coreib.security.RowScope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development-only authentication adapter for exercising the permission model. */
@Profile("demo")
@Component
@RequestScope
public class DemoPermissionProvider implements CurrentAuthorizationProvider {
    public static final String SUBJECT_HEADER = "X-CoreIb-Demo-Subject";

    private final HttpServletRequest request;

    public DemoPermissionProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public PermissionSnapshot current() {
        return definition(subjectKey()).snapshot();
    }

    @Override
    public PermissionSubject subject() {
        return definition(subjectKey()).subject();
    }

    @Override
    public Collection<PermissionPolicy> policies(String resource) {
        ResourcePermission permission = definition(subjectKey()).resources().get(resource);
        if (permission == null) {
            return List.of();
        }
        return List.of(new PermissionPolicy(
                resource,
                permission.actions(),
                permission.rowScope(),
                permission.fields()));
    }

    private String subjectKey() {
        String value = request.getHeader(SUBJECT_HEADER);
        return value == null || value.isBlank() ? "anonymous" : value.trim().toLowerCase();
    }

    private static DemoSubject definition(String key) {
        return switch (key) {
            case "nurse", "nurse-001" -> new DemoSubject(
                    "nurse-001", Set.of("nurse"),
                    PermissionSubject.of("nurse-001", "ward-a"),
                    Map.of("patient", new ResourcePermission(
                            Set.of(PermissionAction.READ), RowScope.RELATED_RECORDS,
                            Map.of("name", ColumnAccess.READ, "idCard", ColumnAccess.MASKED))));
            case "head-nurse", "head-nurse-001" -> new DemoSubject(
                    "head-nurse-001", Set.of("head-nurse"),
                    new PermissionSubject("head-nurse-001", "ward-a", Set.of("ward-a"),
                            Set.of("nurse-001", "nurse-002"), Set.of()),
                    Map.of("patient", new ResourcePermission(
                            Set.of(PermissionAction.READ), RowScope.MANAGED_USERS,
                            Map.of("name", ColumnAccess.READ, "idCard", ColumnAccess.MASKED))));
            case "information-clerk", "info-clerk", "info-clerk-001" -> new DemoSubject(
                    "info-clerk-001", Set.of("information-clerk"),
                    PermissionSubject.of("info-clerk-001", "information"),
                    Map.of("business-data", new ResourcePermission(
                            Set.of(PermissionAction.READ), RowScope.ORGANIZATION, Map.of())));
            case "information-leader", "info-leader", "info-leader-001" -> new DemoSubject(
                    "info-leader-001", Set.of("information-leader"),
                    PermissionSubject.of("info-leader-001", "information"),
                    Map.of("business-data", new ResourcePermission(
                            Set.of(PermissionAction.READ, PermissionAction.UPDATE), RowScope.ORGANIZATION,
                            Map.of("amount", ColumnAccess.WRITE))));
            case "platform-admin", "platform-admin-001" -> new DemoSubject(
                    "platform-admin-001", Set.of("platform-admin"),
                    PermissionSubject.of("platform-admin-001", "information"),
                    Map.of(
                            "platform-directory", new ResourcePermission(
                                    Set.of(PermissionAction.READ, PermissionAction.UPDATE, PermissionAction.DELETE), RowScope.ALL, Map.of()),
                            "security-administration", new ResourcePermission(
                                    Set.of(PermissionAction.READ, PermissionAction.UPDATE), RowScope.ALL, Map.of()),
                            "platform-management", new ResourcePermission(
                                    Set.of(PermissionAction.READ, PermissionAction.UPDATE, PermissionAction.DELETE), RowScope.ALL, Map.of()),
                            "file-management", new ResourcePermission(
                                    Set.of(PermissionAction.READ, PermissionAction.CREATE, PermissionAction.DELETE), RowScope.ALL, Map.of()),
                            "code-generation", new ResourcePermission(
                                    Set.of(PermissionAction.READ, PermissionAction.EXPORT), RowScope.ALL, Map.of())));
            default -> new DemoSubject("anonymous", Set.of(), PermissionSubject.of("anonymous", null), Map.of());
        };
    }

    private record DemoSubject(
            String subjectId,
            Set<String> roles,
            PermissionSubject subject,
            Map<String, ResourcePermission> resources) {
        PermissionSnapshot snapshot() {
            return new PermissionSnapshot(!"anonymous".equals(subjectId), subjectId, roles, resources);
        }
    }
}
