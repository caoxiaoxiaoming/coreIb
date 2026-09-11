package com.coreib.server.api;

import com.coreib.platform.SecurityAdministration;
import com.coreib.security.ColumnAccess;
import com.coreib.security.PermissionAction;
import com.coreib.security.RowScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Mutable development fixture used to exercise the complete administration workflow. */
final class DemoSecurityAdministration implements SecurityAdministration {
    private static final Set<String> ROLES = Set.of(
            "platform-admin", "nurse", "head-nurse", "information-clerk", "information-leader");
    private static final Set<String> USERS = Set.of(
            "platform-admin-001", "nurse-001", "head-nurse-001", "info-clerk-001", "info-leader-001");

    private final List<RolePermissionGrant> permissions = new ArrayList<>();
    private final List<FieldPermissionGrant> fields = new ArrayList<>();
    private final List<UserRoleAssignment> userRoles = new ArrayList<>();
    private final List<SupervisorRelationship> supervisors = new ArrayList<>();
    private final List<RecordAssignment> assignments = new ArrayList<>();

    DemoSecurityAdministration() {
        addPermission("platform-admin", "platform-directory", PermissionAction.READ, RowScope.ALL);
        addPermission("platform-admin", "platform-directory", PermissionAction.UPDATE, RowScope.ALL);
        addPermission("platform-admin", "platform-directory", PermissionAction.DELETE, RowScope.ALL);
        addPermission("platform-admin", "security-administration", PermissionAction.READ, RowScope.ALL);
        addPermission("platform-admin", "security-administration", PermissionAction.UPDATE, RowScope.ALL);
        addPermission("platform-admin", "platform-management", PermissionAction.READ, RowScope.ALL);
        addPermission("platform-admin", "platform-management", PermissionAction.UPDATE, RowScope.ALL);
        addPermission("platform-admin", "platform-management", PermissionAction.DELETE, RowScope.ALL);
        addPermission("platform-admin", "file-management", PermissionAction.READ, RowScope.ALL);
        addPermission("platform-admin", "file-management", PermissionAction.CREATE, RowScope.ALL);
        addPermission("platform-admin", "file-management", PermissionAction.DELETE, RowScope.ALL);
        addPermission("platform-admin", "code-generation", PermissionAction.READ, RowScope.ALL);
        addPermission("platform-admin", "code-generation", PermissionAction.EXPORT, RowScope.ALL);
        addPermission("nurse", "patient", PermissionAction.READ, RowScope.RELATED_RECORDS);
        addPermission("head-nurse", "patient", PermissionAction.READ, RowScope.MANAGED_USERS);
        addPermission("information-clerk", "business-data", PermissionAction.READ, RowScope.ORGANIZATION);
        addPermission("information-leader", "business-data", PermissionAction.READ, RowScope.ORGANIZATION);
        addPermission("information-leader", "business-data", PermissionAction.UPDATE, RowScope.ORGANIZATION);
        fields.add(new FieldPermissionGrant(id(), "nurse", "patient", "name", ColumnAccess.READ));
        fields.add(new FieldPermissionGrant(id(), "nurse", "patient", "idCard", ColumnAccess.MASKED));
        fields.add(new FieldPermissionGrant(id(), "information-leader", "business-data", "amount", ColumnAccess.WRITE));
        userRoles.add(new UserRoleAssignment("platform-admin-001", "platform-admin"));
        userRoles.add(new UserRoleAssignment("nurse-001", "nurse"));
        userRoles.add(new UserRoleAssignment("head-nurse-001", "head-nurse"));
        userRoles.add(new UserRoleAssignment("info-clerk-001", "information-clerk"));
        userRoles.add(new UserRoleAssignment("info-leader-001", "information-leader"));
        supervisors.add(new SupervisorRelationship(id(), "nurse-001", "head-nurse-001", "DIRECT", true, null, null));
        assignments.add(new RecordAssignment(id(), "patient", "patient-001", "nurse-001", "PRIMARY", true, null, null));
    }

    @Override
    public synchronized SecurityConfiguration configuration() {
        return new SecurityConfiguration(permissions, fields, userRoles, supervisors, assignments);
    }

    @Override
    public synchronized void replaceRolePolicy(String roleCode, RolePolicyDraft draft) {
        requireKnown(ROLES, "role", roleCode);
        permissions.removeIf(permission -> permission.roleCode().equals(roleCode));
        fields.removeIf(field -> field.roleCode().equals(roleCode));
        draft.permissions().forEach(permission -> permissions.add(new RolePermissionGrant(
                id(), roleCode, permission.resource(), permission.action(), permission.rowScope(),
                permission.policyKey(), permission.enabled())));
        draft.fields().forEach(field -> fields.add(new FieldPermissionGrant(
                id(), roleCode, field.resource(), field.fieldName(), field.accessMode())));
    }

    @Override
    public synchronized void replaceUserRoles(String userId, UserRolesDraft draft) {
        requireKnown(USERS, "user", userId);
        draft.roleCodes().forEach(role -> requireKnown(ROLES, "role", role));
        userRoles.removeIf(assignment -> assignment.userId().equals(userId));
        draft.roleCodes().stream().sorted().forEach(role -> userRoles.add(new UserRoleAssignment(userId, role)));
    }

    @Override
    public synchronized SupervisorRelationship saveSupervisorRelationship(SupervisorRelationshipDraft draft) {
        requireKnown(USERS, "user", draft.userId());
        requireKnown(USERS, "supervisor", draft.supervisorId());
        SupervisorRelationship relationship = supervisors.stream()
                .filter(value -> value.userId().equals(draft.userId())
                        && value.supervisorId().equals(draft.supervisorId())
                        && value.relationType().equals(draft.relationType()))
                .findFirst()
                .orElse(null);
        if (relationship != null) supervisors.remove(relationship);
        SupervisorRelationship saved = new SupervisorRelationship(
                relationship == null ? id() : relationship.id(), draft.userId(), draft.supervisorId(),
                draft.relationType(), draft.active(), draft.effectiveFrom(), draft.effectiveTo());
        supervisors.add(saved);
        return saved;
    }

    @Override
    public synchronized void setSupervisorRelationshipActive(String id, boolean active) {
        int index = indexOfSupervisor(id);
        SupervisorRelationship current = supervisors.get(index);
        supervisors.set(index, new SupervisorRelationship(current.id(), current.userId(), current.supervisorId(),
                current.relationType(), active, current.effectiveFrom(), current.effectiveTo()));
    }

    @Override
    public synchronized RecordAssignment saveRecordAssignment(RecordAssignmentDraft draft) {
        requireKnown(USERS, "user", draft.userId());
        RecordAssignment assignment = assignments.stream()
                .filter(value -> value.resource().equals(draft.resource())
                        && value.recordId().equals(draft.recordId())
                        && value.userId().equals(draft.userId())
                        && value.assignmentType().equals(draft.assignmentType()))
                .findFirst()
                .orElse(null);
        if (assignment != null) assignments.remove(assignment);
        RecordAssignment saved = new RecordAssignment(
                assignment == null ? id() : assignment.id(), draft.resource(), draft.recordId(), draft.userId(),
                draft.assignmentType(), draft.active(), draft.effectiveFrom(), draft.effectiveTo());
        assignments.add(saved);
        return saved;
    }

    @Override
    public synchronized void setRecordAssignmentActive(String id, boolean active) {
        int index = indexOfAssignment(id);
        RecordAssignment current = assignments.get(index);
        assignments.set(index, new RecordAssignment(current.id(), current.resource(), current.recordId(),
                current.userId(), current.assignmentType(), active, current.effectiveFrom(), current.effectiveTo()));
    }

    private void addPermission(String role, String resource, PermissionAction action, RowScope scope) {
        permissions.add(new RolePermissionGrant(id(), role, resource, action, scope, null, true));
    }

    private int indexOfSupervisor(String id) {
        for (int index = 0; index < supervisors.size(); index++) {
            if (supervisors.get(index).id().equals(id)) return index;
        }
        throw new IllegalArgumentException("Unknown supervisor relationship: " + id);
    }

    private int indexOfAssignment(String id) {
        for (int index = 0; index < assignments.size(); index++) {
            if (assignments.get(index).id().equals(id)) return index;
        }
        throw new IllegalArgumentException("Unknown record assignment: " + id);
    }

    private static void requireKnown(Set<String> values, String type, String value) {
        if (!values.contains(value)) throw new IllegalArgumentException("Unknown " + type + ": " + value);
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }
}
