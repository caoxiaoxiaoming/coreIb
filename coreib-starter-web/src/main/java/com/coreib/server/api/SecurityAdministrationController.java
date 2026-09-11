package com.coreib.server.api;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.AuditSink;
import com.coreib.platform.SecurityAdministration;
import com.coreib.platform.SecurityAdministration.RecordAssignment;
import com.coreib.platform.SecurityAdministration.RecordAssignmentDraft;
import com.coreib.platform.SecurityAdministration.RolePolicyDraft;
import com.coreib.platform.SecurityAdministration.SecurityConfiguration;
import com.coreib.platform.SecurityAdministration.SupervisorRelationship;
import com.coreib.platform.SecurityAdministration.SupervisorRelationshipDraft;
import com.coreib.platform.SecurityAdministration.UserRolesDraft;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.ResourcePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** Permission configuration API guarded by a separate administration resource. */
@RestController
@RequestMapping("/api/v1/platform/security")
public class SecurityAdministrationController {
    public static final String RESOURCE = "security-administration";
    private static final Logger log = LoggerFactory.getLogger(SecurityAdministrationController.class);

    private final CurrentPermissionProvider permissionProvider;
    private final SecurityAdministration administration;
    private final AuditSink auditSink;

    public SecurityAdministrationController(
            CurrentPermissionProvider permissionProvider,
            SecurityAdministration administration,
            AuditSink auditSink) {
        this.permissionProvider = permissionProvider;
        this.administration = administration;
        this.auditSink = auditSink;
    }

    @GetMapping
    public SecurityConfiguration configuration() {
        require(PermissionAction.READ);
        return administration.configuration();
    }

    @PutMapping("/roles/{roleCode}/policy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replaceRolePolicy(@PathVariable String roleCode, @RequestBody RolePolicyDraft draft) {
        require(PermissionAction.UPDATE);
        execute("REPLACE", "role-policy", roleCode,
                () -> administration.replaceRolePolicy(roleCode, draft),
                "permissions=" + draft.permissions().size() + ", fields=" + draft.fields().size());
    }

    @PutMapping("/users/{userId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replaceUserRoles(@PathVariable String userId, @RequestBody UserRolesDraft draft) {
        require(PermissionAction.UPDATE);
        execute("REPLACE", "user-role", userId,
                () -> administration.replaceUserRoles(userId, draft),
                "roles=" + draft.roleCodes().size());
    }

    @PostMapping("/supervisor-relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public SupervisorRelationship saveSupervisorRelationship(@RequestBody SupervisorRelationshipDraft draft) {
        require(PermissionAction.UPDATE);
        try {
            SupervisorRelationship result = administration.saveSupervisorRelationship(draft);
            audit("UPSERT", "supervisor-relationship", result.id(), "SUCCESS", draft.relationType());
            return result;
        } catch (RuntimeException exception) {
            audit("UPSERT", "supervisor-relationship", draft.userId(), "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PatchMapping("/supervisor-relationships/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setSupervisorRelationshipActive(@PathVariable String id, @RequestBody ActiveCommand command) {
        require(PermissionAction.UPDATE);
        execute("UPDATE", "supervisor-relationship", id,
                () -> administration.setSupervisorRelationshipActive(id, command.active()),
                "active=" + command.active());
    }

    @PostMapping("/record-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public RecordAssignment saveRecordAssignment(@RequestBody RecordAssignmentDraft draft) {
        require(PermissionAction.UPDATE);
        try {
            RecordAssignment result = administration.saveRecordAssignment(draft);
            audit("UPSERT", "record-assignment", result.id(), "SUCCESS", draft.resource());
            return result;
        } catch (RuntimeException exception) {
            audit("UPSERT", "record-assignment", draft.recordId(), "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PatchMapping("/record-assignments/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setRecordAssignmentActive(@PathVariable String id, @RequestBody ActiveCommand command) {
        require(PermissionAction.UPDATE);
        execute("UPDATE", "record-assignment", id,
                () -> administration.setRecordAssignmentActive(id, command.active()),
                "active=" + command.active());
    }

    public record ActiveCommand(boolean active) {
    }

    private void require(PermissionAction action) {
        ResourcePermission permission = permissionProvider.current().resources().get(RESOURCE);
        if (permission == null || !permission.actions().contains(action)) {
            throw new PermissionDeniedException(RESOURCE, action, "resource-not-granted");
        }
    }

    private void execute(String action, String resource, String target, Runnable command, String details) {
        try {
            command.run();
            audit(action, resource, target, "SUCCESS", details);
        } catch (RuntimeException exception) {
            audit(action, resource, target, "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private void audit(String action, String resource, String targetId, String outcome, String details) {
        try {
            auditSink.append(new AuditEvent(UUID.randomUUID().toString(), permissionProvider.current().subjectId(),
                    action, resource, targetId, outcome, details, Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("Unable to append security administration audit event action={} resource={} target={} outcome={}",
                    action, resource, targetId, outcome, exception);
        }
    }
}
