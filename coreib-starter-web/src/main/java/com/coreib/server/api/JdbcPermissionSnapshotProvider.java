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
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.Principal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Request-scoped JDBC authorization source for a Servlet-authenticated subject.
 * Original grants remain available to backend services; the UI snapshot is derived separately.
 */
public final class JdbcPermissionSnapshotProvider implements CurrentAuthorizationProvider {
    private static final String USER_SQL = """
            SELECT id, organization_id
            FROM coreib_sys_user
            WHERE id = ? AND enabled = ?
            """;
    private static final String USER_BY_LOGIN_SQL = """
            SELECT id, organization_id
            FROM coreib_sys_user
            WHERE login_name = ? AND enabled = ?
            """;
    private static final String USER_BY_EXTERNAL_IDENTITY_SQL = """
            SELECT id, organization_id
            FROM coreib_sys_user
            WHERE external_identity = ? AND enabled = ?
            """;
    private static final String ROLES_SQL = """
            SELECT r.role_code
            FROM coreib_sec_user_role ur
            JOIN coreib_sec_role r ON r.role_code = ur.role_code
            WHERE ur.user_id = ? AND r.enabled = ?
            ORDER BY r.role_code
            """;
    private static final String PERMISSIONS_SQL = """
            SELECT p.role_code, p.resource, p.action, p.row_scope, p.policy_key
            FROM coreib_sec_permission p
            WHERE p.role_code IN (%s) AND p.enabled = ?
            ORDER BY p.resource, p.action, p.role_code
            """;
    private static final String FIELD_PERMISSIONS_SQL = """
            SELECT fp.role_code, fp.resource, fp.field_name, fp.access_mode
            FROM coreib_sec_field_permission fp
            WHERE fp.role_code IN (%s)
            ORDER BY fp.resource, fp.field_name, fp.role_code
            """;
    private static final String SUPERVISOR_SQL = """
            SELECT user_id, supervisor_id
            FROM coreib_sec_user_supervisor
            WHERE active = ?
              AND (effective_from IS NULL OR effective_from <= CURRENT_TIMESTAMP)
              AND (effective_to IS NULL OR effective_to >= CURRENT_TIMESTAMP)
            """;
    private static final String ORGANIZATION_SQL = """
            SELECT id, parent_id
            FROM coreib_sys_organization
            WHERE enabled = ?
            """;
    private static final String ASSIGNMENT_SQL = """
            SELECT resource, record_id
            FROM coreib_sec_record_assignment
            WHERE user_id = ? AND active = ?
              AND (effective_from IS NULL OR effective_from <= CURRENT_TIMESTAMP)
              AND (effective_to IS NULL OR effective_to >= CURRENT_TIMESTAMP)
            """;

    private final HttpServletRequest request;
    private final JdbcTemplate jdbc;
    private final JdbcPrincipalLookup principalLookup;
    private boolean identityResolved;
    private boolean authorizationResolved;
    private String subjectId;
    private UserFacts user;
    private Set<String> roleCodes = Set.of();
    private Map<String, List<PermissionPolicy>> policiesByResource = Map.of();
    private PermissionSnapshot snapshot;
    private PermissionSubject permissionSubject;
    private final Map<String, PermissionSubject> resourceSubjects = new HashMap<>();
    private boolean relationshipsResolved;
    private Set<String> visibleOrganizationIds = Set.of();
    private Set<String> managedUserIds = Set.of();
    private Map<String, Set<String>> relatedRecordIdsByResource = Map.of();

    public JdbcPermissionSnapshotProvider(HttpServletRequest request, JdbcTemplate jdbc) {
        this(request, jdbc, JdbcPrincipalLookup.ID);
    }

    public JdbcPermissionSnapshotProvider(
            HttpServletRequest request,
            JdbcTemplate jdbc,
            JdbcPrincipalLookup principalLookup) {
        this.request = request;
        this.jdbc = jdbc;
        this.principalLookup = principalLookup == null ? JdbcPrincipalLookup.ID : principalLookup;
    }

    @Override
    public synchronized PermissionSnapshot current() {
        if (snapshot != null) {
            return snapshot;
        }
        resolveAuthorization();
        if (user == null) {
            snapshot = PermissionSnapshot.anonymous();
            return snapshot;
        }
        snapshot = new PermissionSnapshot(
                true,
                subjectId,
                roleCodes,
                snapshotResources());
        return snapshot;
    }

    @Override
    public synchronized Collection<PermissionPolicy> policies(String resource) {
        requireText("resource", resource);
        resolveAuthorization();
        return policiesByResource.getOrDefault(resource, List.of());
    }

    @Override
    public synchronized PermissionSubject subject() {
        if (permissionSubject == null) {
            permissionSubject = buildSubject(null);
        }
        return permissionSubject;
    }

    @Override
    public synchronized PermissionSubject subject(String resource) {
        requireText("resource", resource);
        return resourceSubjects.computeIfAbsent(resource, this::buildSubject);
    }

    private PermissionSubject buildSubject(String resource) {
        resolveIdentity();
        if (user == null) {
            return PermissionSubject.of("anonymous", null);
        }
        resolveRelationships();
        return new PermissionSubject(
                subjectId,
                user.organizationId(),
                visibleOrganizationIds,
                managedUserIds,
                relatedRecords(resource));
    }

    private void resolveAuthorization() {
        if (authorizationResolved) {
            return;
        }
        authorizationResolved = true;
        resolveIdentity();
        if (user == null) {
            return;
        }
        List<String> roles = jdbc.queryForList(ROLES_SQL, String.class, subjectId, Boolean.TRUE);
        roleCodes = Set.copyOf(new LinkedHashSet<>(roles));
        if (!roles.isEmpty()) {
            policiesByResource = loadPolicies(roles);
        }
    }

    private void resolveIdentity() {
        if (identityResolved) {
            return;
        }
        identityResolved = true;
        Principal principal = request.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return;
        }
        String principalName = principal.getName().trim();
        user = findUser(principalName);
        subjectId = user == null ? null : user.id();
    }

    private UserFacts findUser(String id) {
        String sql = switch (principalLookup) {
            case ID -> USER_SQL;
            case LOGIN_NAME -> USER_BY_LOGIN_SQL;
            case EXTERNAL_IDENTITY -> USER_BY_EXTERNAL_IDENTITY_SQL;
        };
        List<UserFacts> users = jdbc.query(sql,
                (rs, row) -> new UserFacts(rs.getString("id"), rs.getString("organization_id")),
                id, Boolean.TRUE);
        // A non-unique identity mapping is unsafe even if a database constraint is missing.
        return users.size() == 1 ? users.get(0) : null;
    }

    private Map<String, List<PermissionPolicy>> loadPolicies(List<String> roles) {
        String placeholders = String.join(",", roles.stream().map(ignored -> "?").toList());
        Map<RoleResource, Map<String, ColumnAccess>> fields = loadFields(roles, placeholders);
        List<Object> arguments = new ArrayList<>(roles);
        arguments.add(Boolean.TRUE);
        Map<String, List<PermissionPolicy>> policies = new LinkedHashMap<>();
        jdbc.query(PERMISSIONS_SQL.formatted(placeholders), rs -> {
            String role = rs.getString("role_code");
            String resource = rs.getString("resource");
            PermissionPolicy policy = new PermissionPolicy(
                    resource,
                    Set.of(parseAction(rs.getString("action"))),
                    parseRowScope(rs.getString("row_scope")),
                    fields.getOrDefault(new RoleResource(role, resource), Map.of()),
                    rs.getString("policy_key"));
            policies.computeIfAbsent(resource, ignored -> new ArrayList<>()).add(policy);
        }, arguments.toArray());
        Map<String, List<PermissionPolicy>> immutable = new LinkedHashMap<>();
        policies.forEach((resource, grants) -> immutable.put(resource, List.copyOf(grants)));
        return Map.copyOf(immutable);
    }

    private Map<RoleResource, Map<String, ColumnAccess>> loadFields(
            List<String> roles,
            String placeholders) {
        Map<RoleResource, Map<String, ColumnAccess>> fields = new LinkedHashMap<>();
        jdbc.query(FIELD_PERMISSIONS_SQL.formatted(placeholders), rs -> {
            RoleResource key = new RoleResource(
                    rs.getString("role_code"),
                    rs.getString("resource"));
            fields.computeIfAbsent(key, ignored -> new LinkedHashMap<>())
                    .merge(
                            rs.getString("field_name"),
                            parseColumnAccess(rs.getString("access_mode")),
                            JdbcPermissionSnapshotProvider::strongerAccess);
        }, roles.toArray());
        return fields;
    }

    private void resolveRelationships() {
        if (relationshipsResolved) {
            return;
        }
        relationshipsResolved = true;
        visibleOrganizationIds = visibleOrganizations(user.organizationId());
        managedUserIds = managedUsers(subjectId);
        relatedRecordIdsByResource = relatedRecordsByResource(subjectId);
    }

    private Map<String, ResourcePermission> snapshotResources() {
        Map<String, ResourcePermission> resources = new LinkedHashMap<>();
        policiesByResource.forEach((resource, policies) -> {
            ResourceAccumulator accumulator = new ResourceAccumulator();
            for (PermissionPolicy policy : policies) {
                accumulator.actions.addAll(policy.actions());
                accumulator.rowScopes.add(policy.rowScope());
                policy.fields().forEach((field, access) ->
                        accumulator.fields.merge(
                                field,
                                access,
                                JdbcPermissionSnapshotProvider::strongerAccess));
            }
            resources.put(resource, accumulator.toPermission());
        });
        return resources;
    }

    private Set<String> visibleOrganizations(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return Set.of();
        }
        Map<String, Set<String>> children = new HashMap<>();
        jdbc.query(ORGANIZATION_SQL, rs -> {
            String id = rs.getString("id");
            String parentId = rs.getString("parent_id");
            if (parentId != null && !parentId.isBlank()) {
                children.computeIfAbsent(parentId, ignored -> new LinkedHashSet<>()).add(id);
            }
        }, Boolean.TRUE);
        Set<String> visible = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(organizationId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visible.add(current)) {
                continue;
            }
            queue.addAll(children.getOrDefault(current, Set.of()));
        }
        return visible;
    }

    private Set<String> managedUsers(String supervisorId) {
        Map<String, Set<String>> reports = new HashMap<>();
        jdbc.query(SUPERVISOR_SQL, rs -> {
            reports.computeIfAbsent(
                            rs.getString("supervisor_id"),
                            ignored -> new LinkedHashSet<>())
                    .add(rs.getString("user_id"));
        }, Boolean.TRUE);
        Set<String> managed = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(supervisorId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String report : reports.getOrDefault(current, Set.of())) {
                if (managed.add(report)) {
                    queue.addLast(report);
                }
            }
        }
        return managed;
    }

    private Map<String, Set<String>> relatedRecordsByResource(String userId) {
        Map<String, Set<String>> records = new LinkedHashMap<>();
        jdbc.query(ASSIGNMENT_SQL, rs -> {
            records.computeIfAbsent(
                            rs.getString("resource"),
                            ignored -> new LinkedHashSet<>())
                    .add(rs.getString("record_id"));
        }, userId, Boolean.TRUE);
        Map<String, Set<String>> immutable = new LinkedHashMap<>();
        records.forEach((resource, ids) -> immutable.put(resource, Set.copyOf(ids)));
        return Map.copyOf(immutable);
    }

    private Set<String> relatedRecords(String resource) {
        if (resource != null) {
            return relatedRecordIdsByResource.getOrDefault(resource, Set.of());
        }
        Set<String> all = new LinkedHashSet<>();
        relatedRecordIdsByResource.values().forEach(all::addAll);
        return Set.copyOf(all);
    }

    private static PermissionAction parseAction(String value) {
        try {
            return PermissionAction.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unsupported permission action: " + value, exception);
        }
    }

    private static RowScope parseRowScope(String value) {
        try {
            return RowScope.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unsupported row scope: " + value, exception);
        }
    }

    private static ColumnAccess parseColumnAccess(String value) {
        try {
            return ColumnAccess.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unsupported column access: " + value, exception);
        }
    }

    private static ColumnAccess strongerAccess(ColumnAccess left, ColumnAccess right) {
        return accessRank(left) >= accessRank(right) ? left : right;
    }

    private static int accessRank(ColumnAccess access) {
        return switch (access) {
            case HIDDEN -> 0;
            case MASKED -> 1;
            case READ -> 2;
            case WRITE -> 3;
        };
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private record UserFacts(String id, String organizationId) { }

    private record RoleResource(String role, String resource) { }

    private static final class ResourceAccumulator {
        private final Set<PermissionAction> actions = new LinkedHashSet<>();
        private final Set<RowScope> rowScopes = new LinkedHashSet<>();
        private final Map<String, ColumnAccess> fields = new LinkedHashMap<>();

        private ResourcePermission toPermission() {
            return new ResourcePermission(actions, projectedScope(rowScopes), fields);
        }

        /** UI-only summary; backend decisions always use the original policy collection. */
        private static RowScope projectedScope(Set<RowScope> scopes) {
            if (scopes.contains(RowScope.ALL)) {
                return RowScope.ALL;
            }
            if (scopes.size() == 1) {
                return scopes.iterator().next();
            }
            return RowScope.CUSTOM;
        }
    }
}
