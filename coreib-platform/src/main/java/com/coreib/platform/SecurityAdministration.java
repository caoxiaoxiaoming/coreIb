package com.coreib.platform;

import com.coreib.security.ColumnAccess;
import com.coreib.security.PermissionAction;
import com.coreib.security.RowScope;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Administrative port for role policies and the relationships used by row authorization. */
public interface SecurityAdministration {
    SecurityConfiguration configuration();

    void replaceRolePolicy(String roleCode, RolePolicyDraft draft);

    void replaceUserRoles(String userId, UserRolesDraft draft);

    SupervisorRelationship saveSupervisorRelationship(SupervisorRelationshipDraft draft);

    void setSupervisorRelationshipActive(String id, boolean active);

    RecordAssignment saveRecordAssignment(RecordAssignmentDraft draft);

    void setRecordAssignmentActive(String id, boolean active);

    record SecurityConfiguration(
            List<RolePermissionGrant> permissions,
            List<FieldPermissionGrant> fields,
            List<UserRoleAssignment> userRoles,
            List<SupervisorRelationship> supervisorRelationships,
            List<RecordAssignment> recordAssignments) {
        public SecurityConfiguration {
            permissions = immutable(permissions);
            fields = immutable(fields);
            userRoles = immutable(userRoles);
            supervisorRelationships = immutable(supervisorRelationships);
            recordAssignments = immutable(recordAssignments);
        }
    }

    record RolePermissionGrant(
            String id,
            String roleCode,
            String resource,
            PermissionAction action,
            RowScope rowScope,
            String policyKey,
            boolean enabled) {
    }

    record FieldPermissionGrant(
            String id,
            String roleCode,
            String resource,
            String fieldName,
            ColumnAccess accessMode) {
    }

    record UserRoleAssignment(String userId, String roleCode) {
    }

    record SupervisorRelationship(
            String id,
            String userId,
            String supervisorId,
            String relationType,
            boolean active,
            Instant effectiveFrom,
            Instant effectiveTo) {
    }

    record RecordAssignment(
            String id,
            String resource,
            String recordId,
            String userId,
            String assignmentType,
            boolean active,
            Instant effectiveFrom,
            Instant effectiveTo) {
    }

    record RolePolicyDraft(List<PermissionGrantDraft> permissions, List<FieldGrantDraft> fields) {
        public RolePolicyDraft {
            permissions = immutable(permissions);
            fields = immutable(fields);
            Set<String> resources = permissions.stream()
                    .map(PermissionGrantDraft::resource)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            for (FieldGrantDraft field : fields) {
                if (!resources.contains(field.resource())) {
                    throw new IllegalArgumentException("Field grant requires a permission for resource " + field.resource());
                }
            }
            rejectDuplicates(permissions.stream()
                    .map(permission -> permission.resource() + "\u0000" + permission.action() + "\u0000"
                            + permission.rowScope() + "\u0000" + nullToEmpty(permission.policyKey()))
                    .toList(), "Duplicate role permission");
            rejectDuplicates(fields.stream()
                    .map(field -> field.resource() + "\u0000" + field.fieldName())
                    .toList(), "Duplicate field permission");
        }
    }

    record PermissionGrantDraft(
            String resource,
            PermissionAction action,
            RowScope rowScope,
            String policyKey,
            boolean enabled) {
        public PermissionGrantDraft {
            requireText("resource", resource);
            if (action == null) throw new IllegalArgumentException("action must not be null");
            if (rowScope == null) throw new IllegalArgumentException("rowScope must not be null");
            policyKey = trimToNull(policyKey);
        }
    }

    record FieldGrantDraft(String resource, String fieldName, ColumnAccess accessMode) {
        public FieldGrantDraft {
            requireText("resource", resource);
            requireText("fieldName", fieldName);
            if (accessMode == null) throw new IllegalArgumentException("accessMode must not be null");
        }
    }

    record UserRolesDraft(Set<String> roleCodes) {
        public UserRolesDraft {
            roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
            roleCodes.forEach(role -> requireText("roleCode", role));
        }
    }

    record SupervisorRelationshipDraft(
            String userId,
            String supervisorId,
            String relationType,
            boolean active,
            Instant effectiveFrom,
            Instant effectiveTo) {
        public SupervisorRelationshipDraft {
            requireText("userId", userId);
            requireText("supervisorId", supervisorId);
            requireText("relationType", relationType);
            if (userId.equals(supervisorId)) throw new IllegalArgumentException("A user cannot supervise itself");
            requireValidWindow(effectiveFrom, effectiveTo);
        }
    }

    record RecordAssignmentDraft(
            String resource,
            String recordId,
            String userId,
            String assignmentType,
            boolean active,
            Instant effectiveFrom,
            Instant effectiveTo) {
        public RecordAssignmentDraft {
            requireText("resource", resource);
            requireText("recordId", recordId);
            requireText("userId", userId);
            requireText("assignmentType", assignmentType);
            requireValidWindow(effectiveFrom, effectiveTo);
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void requireValidWindow(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("effectiveFrom must be before effectiveTo");
        }
    }

    private static void rejectDuplicates(List<String> keys, String message) {
        if (Set.copyOf(keys).size() != keys.size()) throw new IllegalArgumentException(message);
    }
}
