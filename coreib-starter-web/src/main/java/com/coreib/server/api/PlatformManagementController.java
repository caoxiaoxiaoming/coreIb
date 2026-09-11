package com.coreib.server.api;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.AuditSink;
import com.coreib.platform.PlatformManagement;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.ResourcePermission;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Complete system-management API consumed by the Vue administration modules. */
@RestController
@RequestMapping("/api/v1/platform/management")
public class PlatformManagementController {
    public static final String RESOURCE = "platform-management";
    private final CurrentPermissionProvider permissions;
    private final PlatformManagement management;
    private final AuditSink auditSink;

    public PlatformManagementController(CurrentPermissionProvider permissions, PlatformManagement management,
                                        AuditSink auditSink) {
        this.permissions = permissions;
        this.management = management;
        this.auditSink = auditSink;
    }

    @GetMapping("/menus") public List<PlatformManagement.MenuEntry> menus() { require(PermissionAction.READ); return management.menus(); }
    @GetMapping("/posts") public List<PlatformManagement.PostEntry> posts() { require(PermissionAction.READ); return management.posts(); }
    @GetMapping("/dictionary-types") public List<PlatformManagement.DictionaryTypeEntry> dictionaryTypes() { require(PermissionAction.READ); return management.dictionaryTypes(); }
    @GetMapping("/dictionary-data") public List<PlatformManagement.DictionaryDataEntry> dictionaryData(@RequestParam(required = false) String typeCode) { require(PermissionAction.READ); return management.dictionaryData(typeCode); }
    @GetMapping("/configs") public List<PlatformManagement.ConfigEntry> configs() { require(PermissionAction.READ); return management.configs(); }
    @GetMapping("/notices") public List<PlatformManagement.NoticeEntry> notices() { require(PermissionAction.READ); return management.notices(); }
    @GetMapping("/audit-events") public List<AuditEvent> auditEvents() { require(PermissionAction.READ); return management.auditEvents(); }
    @GetMapping("/login-events") public List<PlatformManagement.LoginEvent> loginEvents() { require(PermissionAction.READ); return management.loginEvents(); }

    @PutMapping("/menus/{id}") public PlatformManagement.MenuEntry saveMenu(@PathVariable String id, @RequestBody PlatformManagement.MenuEntry entry) { verifyId(id, entry.id()); return save("menu", id, () -> management.saveMenu(entry)); }
    @PutMapping("/posts/{id}") public PlatformManagement.PostEntry savePost(@PathVariable String id, @RequestBody PlatformManagement.PostEntry entry) { verifyId(id, entry.id()); return save("post", id, () -> management.savePost(entry)); }
    @PutMapping("/dictionary-types/{id}") public PlatformManagement.DictionaryTypeEntry saveDictionaryType(@PathVariable String id, @RequestBody PlatformManagement.DictionaryTypeEntry entry) { verifyId(id, entry.id()); return save("dictionary-type", id, () -> management.saveDictionaryType(entry)); }
    @PutMapping("/dictionary-data/{id}") public PlatformManagement.DictionaryDataEntry saveDictionaryData(@PathVariable String id, @RequestBody PlatformManagement.DictionaryDataEntry entry) { verifyId(id, entry.id()); return save("dictionary-data", id, () -> management.saveDictionaryData(entry)); }
    @PutMapping("/configs/{id}") public PlatformManagement.ConfigEntry saveConfig(@PathVariable String id, @RequestBody PlatformManagement.ConfigEntry entry) { verifyId(id, entry.id()); return save("config", id, () -> management.saveConfig(entry)); }
    @PutMapping("/notices/{id}") public PlatformManagement.NoticeEntry saveNotice(@PathVariable String id, @RequestBody PlatformManagement.NoticeEntry entry) { verifyId(id, entry.id()); return save("notice", id, () -> management.saveNotice(entry)); }

    @DeleteMapping("/{entity}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable PlatformManagement.ManagedEntity entity, @PathVariable String id) {
        require(PermissionAction.DELETE);
        management.delete(entity, id);
        audit("DELETE", entity.name().toLowerCase(), id);
    }

    private <T> T save(String resource, String id, java.util.function.Supplier<T> operation) {
        require(PermissionAction.UPDATE);
        T result = operation.get();
        audit("SAVE", resource, id);
        return result;
    }

    private void require(PermissionAction action) {
        ResourcePermission permission = permissions.current().resources().get(RESOURCE);
        if (permission == null || !permission.actions().contains(action)) {
            throw new PermissionDeniedException(RESOURCE, action, "resource-not-granted");
        }
    }

    private void audit(String action, String resource, String target) {
        auditSink.append(new AuditEvent(UUID.randomUUID().toString(), permissions.current().subjectId(),
                action, resource, target, "SUCCESS", "platform management", Instant.now()));
    }

    private static void verifyId(String pathId, String bodyId) {
        if (!pathId.equals(bodyId)) throw new IllegalArgumentException("Path id does not match body id");
    }
}
