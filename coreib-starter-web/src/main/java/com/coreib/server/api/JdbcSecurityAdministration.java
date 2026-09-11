package com.coreib.server.api;

import com.coreib.platform.SecurityAdministration;
import com.coreib.security.ColumnAccess;
import com.coreib.security.PermissionAction;
import com.coreib.security.RowScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Portable JDBC implementation of the security configuration administration port. */
public final class JdbcSecurityAdministration implements SecurityAdministration {
    private static final String PERMISSIONS_SQL = """
            SELECT id, role_code, resource, action, row_scope, policy_key, enabled
            FROM coreib_sec_permission
            ORDER BY role_code, resource, action, row_scope, id
            """;
    private static final String FIELDS_SQL = """
            SELECT id, role_code, resource, field_name, access_mode
            FROM coreib_sec_field_permission
            ORDER BY role_code, resource, field_name, id
            """;
    private static final String USER_ROLES_SQL = """
            SELECT user_id, role_code
            FROM coreib_sec_user_role
            ORDER BY user_id, role_code
            """;
    private static final String SUPERVISORS_SQL = """
            SELECT id, user_id, supervisor_id, relation_type, active, effective_from, effective_to
            FROM coreib_sec_user_supervisor
            ORDER BY supervisor_id, user_id, relation_type, id
            """;
    private static final String ASSIGNMENTS_SQL = """
            SELECT id, resource, record_id, user_id, assignment_type, active, effective_from, effective_to
            FROM coreib_sec_record_assignment
            ORDER BY resource, record_id, user_id, assignment_type, id
            """;

    private final JdbcTemplate jdbc;

    public JdbcSecurityAdministration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SecurityConfiguration configuration() {
        List<RolePermissionGrant> permissions = jdbc.query(PERMISSIONS_SQL, (rs, row) -> new RolePermissionGrant(
                rs.getString("id"), rs.getString("role_code"), rs.getString("resource"),
                parse(PermissionAction.class, rs.getString("action")),
                parse(RowScope.class, rs.getString("row_scope")),
                rs.getString("policy_key"), rs.getBoolean("enabled")));
        List<FieldPermissionGrant> fields = jdbc.query(FIELDS_SQL, (rs, row) -> new FieldPermissionGrant(
                rs.getString("id"), rs.getString("role_code"), rs.getString("resource"),
                rs.getString("field_name"), parse(ColumnAccess.class, rs.getString("access_mode"))));
        List<UserRoleAssignment> userRoles = jdbc.query(USER_ROLES_SQL, (rs, row) -> new UserRoleAssignment(
                rs.getString("user_id"), rs.getString("role_code")));
        List<SupervisorRelationship> supervisors = jdbc.query(SUPERVISORS_SQL, (rs, row) -> new SupervisorRelationship(
                rs.getString("id"), rs.getString("user_id"), rs.getString("supervisor_id"),
                rs.getString("relation_type"), rs.getBoolean("active"),
                instant(rs.getTimestamp("effective_from")), instant(rs.getTimestamp("effective_to"))));
        List<RecordAssignment> assignments = jdbc.query(ASSIGNMENTS_SQL, (rs, row) -> new RecordAssignment(
                rs.getString("id"), rs.getString("resource"), rs.getString("record_id"),
                rs.getString("user_id"), rs.getString("assignment_type"), rs.getBoolean("active"),
                instant(rs.getTimestamp("effective_from")), instant(rs.getTimestamp("effective_to"))));
        return new SecurityConfiguration(permissions, fields, userRoles, supervisors, assignments);
    }

    @Override
    @Transactional
    public void replaceRolePolicy(String roleCode, RolePolicyDraft draft) {
        requireExisting("role", "coreib_sec_role", "role_code", roleCode);
        jdbc.update("DELETE FROM coreib_sec_field_permission WHERE role_code = ?", roleCode);
        jdbc.update("DELETE FROM coreib_sec_permission WHERE role_code = ?", roleCode);
        for (PermissionGrantDraft permission : draft.permissions()) {
            jdbc.update("INSERT INTO coreib_sec_permission (id, role_code, resource, action, row_scope, policy_key, enabled, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    id(), roleCode, permission.resource(), permission.action().name(), permission.rowScope().name(),
                    permission.policyKey(), permission.enabled());
        }
        for (FieldGrantDraft field : draft.fields()) {
            jdbc.update("INSERT INTO coreib_sec_field_permission (id, role_code, resource, field_name, access_mode, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    id(), roleCode, field.resource(), field.fieldName(), field.accessMode().name());
        }
    }

    @Override
    @Transactional
    public void replaceUserRoles(String userId, UserRolesDraft draft) {
        requireExisting("user", "coreib_sys_user", "id", userId);
        for (String roleCode : draft.roleCodes()) {
            requireExisting("role", "coreib_sec_role", "role_code", roleCode);
        }
        jdbc.update("DELETE FROM coreib_sec_user_role WHERE user_id = ?", userId);
        for (String roleCode : draft.roleCodes().stream().sorted().toList()) {
            jdbc.update("INSERT INTO coreib_sec_user_role (id, user_id, role_code, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    id(), userId, roleCode);
        }
    }

    @Override
    @Transactional
    public SupervisorRelationship saveSupervisorRelationship(SupervisorRelationshipDraft draft) {
        requireExisting("user", "coreib_sys_user", "id", draft.userId());
        requireExisting("supervisor", "coreib_sys_user", "id", draft.supervisorId());
        List<String> ids = jdbc.queryForList(
                "SELECT id FROM coreib_sec_user_supervisor WHERE user_id = ? AND supervisor_id = ? AND relation_type = ?",
                String.class, draft.userId(), draft.supervisorId(), draft.relationType());
        String relationshipId = ids.isEmpty() ? id() : ids.get(0);
        if (ids.isEmpty()) {
            jdbc.update("INSERT INTO coreib_sec_user_supervisor (id, user_id, supervisor_id, relation_type, active, effective_from, effective_to, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    relationshipId, draft.userId(), draft.supervisorId(), draft.relationType(), draft.active(),
                    timestampParameter(draft.effectiveFrom()), timestampParameter(draft.effectiveTo()));
        } else {
            jdbc.update("UPDATE coreib_sec_user_supervisor SET active = ?, effective_from = ?, effective_to = ? WHERE id = ?",
                    draft.active(), timestampParameter(draft.effectiveFrom()), timestampParameter(draft.effectiveTo()), relationshipId);
        }
        return new SupervisorRelationship(relationshipId, draft.userId(), draft.supervisorId(), draft.relationType(),
                draft.active(), draft.effectiveFrom(), draft.effectiveTo());
    }

    @Override
    @Transactional
    public void setSupervisorRelationshipActive(String id, boolean active) {
        updateActive("coreib_sec_user_supervisor", "supervisor relationship", id, active);
    }

    @Override
    @Transactional
    public RecordAssignment saveRecordAssignment(RecordAssignmentDraft draft) {
        requireExisting("user", "coreib_sys_user", "id", draft.userId());
        List<String> ids = jdbc.queryForList(
                "SELECT id FROM coreib_sec_record_assignment WHERE resource = ? AND record_id = ? AND user_id = ? AND assignment_type = ?",
                String.class, draft.resource(), draft.recordId(), draft.userId(), draft.assignmentType());
        String assignmentId = ids.isEmpty() ? id() : ids.get(0);
        if (ids.isEmpty()) {
            jdbc.update("INSERT INTO coreib_sec_record_assignment (id, resource, record_id, user_id, assignment_type, active, effective_from, effective_to, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    assignmentId, draft.resource(), draft.recordId(), draft.userId(), draft.assignmentType(),
                    draft.active(), timestampParameter(draft.effectiveFrom()), timestampParameter(draft.effectiveTo()));
        } else {
            jdbc.update("UPDATE coreib_sec_record_assignment SET active = ?, effective_from = ?, effective_to = ? WHERE id = ?",
                    draft.active(), timestampParameter(draft.effectiveFrom()), timestampParameter(draft.effectiveTo()), assignmentId);
        }
        return new RecordAssignment(assignmentId, draft.resource(), draft.recordId(), draft.userId(),
                draft.assignmentType(), draft.active(), draft.effectiveFrom(), draft.effectiveTo());
    }

    @Override
    @Transactional
    public void setRecordAssignmentActive(String id, boolean active) {
        updateActive("coreib_sec_record_assignment", "record assignment", id, active);
    }

    private void requireExisting(String type, String table, String column, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(type + " must not be blank");
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
        if (count == null || count != 1) throw new IllegalArgumentException("Unknown " + type + ": " + value);
    }

    private void updateActive(String table, String type, String id, boolean active) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        int updated = jdbc.update("UPDATE " + table + " SET active = ? WHERE id = ?", active, id);
        if (updated == 0) throw new IllegalArgumentException("Unknown " + type + ": " + id);
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static SqlParameterValue timestampParameter(Instant value) {
        return new SqlParameterValue(Types.TIMESTAMP, value == null ? null : Timestamp.from(value));
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static <T extends Enum<T>> T parse(Class<T> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unsupported " + type.getSimpleName() + ": " + value, exception);
        }
    }
}
