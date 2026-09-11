package com.coreib.server.api;

import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.PlatformDirectory;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserSummary;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.ResourcePermission;
import com.coreib.platform.AuditEvent;
import com.coreib.platform.AuditSink;
import com.coreib.platform.DirectoryEntity;
import com.coreib.platform.OrganizationDraft;
import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.PlatformDirectoryCommands;
import com.coreib.platform.RoleDraft;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserDraft;
import com.coreib.platform.UserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Read-only platform directory API; mutations will use separate command endpoints. */
@RestController
@RequestMapping("/api/v1/platform/directory")
public class PlatformDirectoryController {
    public static final String RESOURCE = "platform-directory";
    private static final Logger log = LoggerFactory.getLogger(PlatformDirectoryController.class);

    private final CurrentPermissionProvider permissionProvider;
    private final PlatformDirectory directory;
    private final PlatformDirectoryCommands commands;
    private final AuditSink auditSink;

    @Autowired
    public PlatformDirectoryController(CurrentPermissionProvider permissionProvider, PlatformDirectory directory,
                                       PlatformDirectoryCommands commands, AuditSink auditSink) {
        this.permissionProvider = permissionProvider;
        this.directory = directory;
        this.commands = commands;
        this.auditSink = auditSink;
    }

    public PlatformDirectoryController(CurrentPermissionProvider permissionProvider, PlatformDirectory directory) {
        this(permissionProvider, directory, new UnavailablePlatformDirectoryCommands(), new NoopAuditSink());
    }

    @GetMapping("/organizations")
    public List<OrganizationSummary> organizations() {
        requireRead();
        return directory.organizations();
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        requireRead();
        return directory.users();
    }

    @GetMapping("/roles")
    public List<RoleSummary> roles() {
        requireRead();
        return directory.roles();
    }

    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationSummary createOrganization(@RequestBody OrganizationDraft draft) {
        requireUpdate();
        try {
            OrganizationSummary result = commands.createOrganization(draft);
            audit("CREATE", "organization", draft.id(), "SUCCESS", draft.displayName());
            return result;
        } catch (RuntimeException exception) {
            audit("CREATE", "organization", draft.id(), "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary createUser(@RequestBody UserDraft draft) {
        requireUpdate();
        try {
            UserSummary result = commands.createUser(draft);
            audit("CREATE", "user", draft.id(), "SUCCESS", draft.loginName());
            return result;
        } catch (RuntimeException exception) {
            audit("CREATE", "user", draft.id(), "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleSummary createRole(@RequestBody RoleDraft draft) {
        requireUpdate();
        try {
            RoleSummary result = commands.createRole(draft);
            audit("CREATE", "role", draft.code(), "SUCCESS", draft.displayName());
            return result;
        } catch (RuntimeException exception) {
            audit("CREATE", "role", draft.code(), "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PutMapping("/organizations/{id}")
    public OrganizationSummary updateOrganization(@org.springframework.web.bind.annotation.PathVariable String id,
                                                   @RequestBody OrganizationDraft draft) {
        requireUpdate();
        OrganizationSummary result = commands.updateOrganization(id, draft);
        audit("UPDATE", "organization", id, "SUCCESS", draft.displayName());
        return result;
    }

    @PutMapping("/users/{id}")
    public UserSummary updateUser(@org.springframework.web.bind.annotation.PathVariable String id,
                                  @RequestBody UserDraft draft) {
        requireUpdate();
        UserSummary result = commands.updateUser(id, draft);
        audit("UPDATE", "user", id, "SUCCESS", draft.loginName());
        return result;
    }

    @PutMapping("/roles/{id}")
    public RoleSummary updateRole(@org.springframework.web.bind.annotation.PathVariable String id,
                                  @RequestBody RoleDraft draft) {
        requireUpdate();
        RoleSummary result = commands.updateRole(id, draft);
        audit("UPDATE", "role", id, "SUCCESS", draft.displayName());
        return result;
    }

    @DeleteMapping("/{entity}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@org.springframework.web.bind.annotation.PathVariable String entity,
                       @org.springframework.web.bind.annotation.PathVariable String id) {
        requireDelete();
        DirectoryEntity directoryEntity = DirectoryEntity.valueOf(entity.toUpperCase(java.util.Locale.ROOT));
        commands.delete(directoryEntity, id);
        audit("DELETE", directoryEntity.name().toLowerCase(), id, "SUCCESS", null);
    }

    @PatchMapping("/{entity}/{id}/enabled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEnabled(@org.springframework.web.bind.annotation.PathVariable String entity,
                           @org.springframework.web.bind.annotation.PathVariable String id,
                           @RequestBody EnabledCommand command) {
        requireUpdate();
        DirectoryEntity directoryEntity = DirectoryEntity.valueOf(entity.toUpperCase(java.util.Locale.ROOT));
        try {
            commands.setEnabled(directoryEntity, id, command.enabled());
            audit("UPDATE", directoryEntity.name().toLowerCase(), id, "SUCCESS", "enabled=" + command.enabled());
        } catch (RuntimeException exception) {
            audit("UPDATE", directoryEntity.name().toLowerCase(), id, "FAILED", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    public record EnabledCommand(boolean enabled) { }

    private void requireRead() {
        ResourcePermission permission = permissionProvider.current().resources().get(RESOURCE);
        if (permission == null || !permission.actions().contains(PermissionAction.READ)) {
            throw new PermissionDeniedException(RESOURCE, PermissionAction.READ, "resource-not-granted");
        }
    }

    private void requireUpdate() {
        ResourcePermission permission = permissionProvider.current().resources().get(RESOURCE);
        if (permission == null || !permission.actions().contains(PermissionAction.UPDATE)) {
            throw new PermissionDeniedException(RESOURCE, PermissionAction.UPDATE, "resource-not-granted");
        }
    }

    private void requireDelete() {
        ResourcePermission permission = permissionProvider.current().resources().get(RESOURCE);
        if (permission == null || !permission.actions().contains(PermissionAction.DELETE)) {
            throw new PermissionDeniedException(RESOURCE, PermissionAction.DELETE, "resource-not-granted");
        }
    }

    private void audit(String action, String resource, String targetId, String outcome, String details) {
        try {
            auditSink.append(new AuditEvent(java.util.UUID.randomUUID().toString(),
                    permissionProvider.current().subjectId(), action, resource, targetId, outcome, details, java.time.Instant.now()));
        } catch (RuntimeException exception) {
            // Auditing is best effort on the command path; preserve the original business outcome.
            log.warn("Unable to append audit event action={} resource={} target={} outcome={}",
                    action, resource, targetId, outcome, exception);
        }
    }
}
