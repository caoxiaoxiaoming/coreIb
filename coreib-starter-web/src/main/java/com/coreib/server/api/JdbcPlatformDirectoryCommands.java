package com.coreib.server.api;

import com.coreib.platform.DirectoryEntity;
import com.coreib.platform.OrganizationDraft;
import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.PlatformDirectoryCommands;
import com.coreib.platform.RoleDraft;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserDraft;
import com.coreib.platform.UserSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Standard-SQL command adapter for the platform directory tables. */
public final class JdbcPlatformDirectoryCommands implements PlatformDirectoryCommands {
    private final JdbcTemplate jdbc;

    public JdbcPlatformDirectoryCommands(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public OrganizationSummary createOrganization(OrganizationDraft draft) {
        jdbc.update("INSERT INTO coreib_sys_organization (id, parent_id, org_code, display_name, org_type, sort_order, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                draft.id(), draft.parentId(), draft.code(), draft.displayName(), draft.type(), draft.sortOrder(), true);
        return new OrganizationSummary(draft.id(), draft.parentId(), draft.code(), draft.displayName(), draft.type(), true);
    }

    @Override
    @Transactional
    public UserSummary createUser(UserDraft draft) {
        jdbc.update("INSERT INTO coreib_sys_user (id, login_name, display_name, organization_id, external_identity, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                draft.id(), draft.loginName(), draft.displayName(), draft.organizationId(), draft.externalIdentity(), true);
        for (String roleCode : draft.roleCodes()) {
            jdbc.update("INSERT INTO coreib_sec_user_role (id, user_id, role_code, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    draft.id() + ":" + roleCode, draft.id(), roleCode);
        }
        return new UserSummary(draft.id(), draft.loginName(), draft.displayName(), draft.organizationId(), null, true, draft.roleCodes());
    }

    @Override
    @Transactional
    public RoleSummary createRole(RoleDraft draft) {
        jdbc.update("INSERT INTO coreib_sec_role (role_code, display_name, description, enabled, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                draft.code(), draft.displayName(), draft.description(), true);
        return new RoleSummary(draft.code(), draft.displayName(), draft.description(), true);
    }

    @Override
    @Transactional
    public OrganizationSummary updateOrganization(String id, OrganizationDraft draft) {
        requireSame(id, draft.id());
        int updated = jdbc.update("UPDATE coreib_sys_organization SET parent_id=?, org_code=?, display_name=?, org_type=?, sort_order=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                draft.parentId(), draft.code(), draft.displayName(), draft.type(), draft.sortOrder(), id);
        requireUpdated(updated, "organization", id);
        Boolean enabled = jdbc.queryForObject("SELECT enabled FROM coreib_sys_organization WHERE id=?", Boolean.class, id);
        return new OrganizationSummary(id, draft.parentId(), draft.code(), draft.displayName(), draft.type(), Boolean.TRUE.equals(enabled));
    }

    @Override
    @Transactional
    public UserSummary updateUser(String id, UserDraft draft) {
        requireSame(id, draft.id());
        int updated = jdbc.update("UPDATE coreib_sys_user SET login_name=?, display_name=?, organization_id=?, external_identity=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                draft.loginName(), draft.displayName(), draft.organizationId(), draft.externalIdentity(), id);
        requireUpdated(updated, "user", id);
        jdbc.update("DELETE FROM coreib_sec_user_role WHERE user_id=?", id);
        for (String roleCode : draft.roleCodes()) {
            jdbc.update("INSERT INTO coreib_sec_user_role (id, user_id, role_code, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    java.util.UUID.randomUUID().toString(), id, roleCode);
        }
        String organizationName = draft.organizationId() == null ? null : jdbc.query(
                "SELECT display_name FROM coreib_sys_organization WHERE id=?", result -> result.next() ? result.getString(1) : null, draft.organizationId());
        Boolean enabled = jdbc.queryForObject("SELECT enabled FROM coreib_sys_user WHERE id=?", Boolean.class, id);
        return new UserSummary(id, draft.loginName(), draft.displayName(), draft.organizationId(), organizationName, Boolean.TRUE.equals(enabled), draft.roleCodes());
    }

    @Override
    @Transactional
    public RoleSummary updateRole(String code, RoleDraft draft) {
        requireSame(code, draft.code());
        int updated = jdbc.update("UPDATE coreib_sec_role SET display_name=?, description=? WHERE role_code=?",
                draft.displayName(), draft.description(), code);
        requireUpdated(updated, "role", code);
        Boolean enabled = jdbc.queryForObject("SELECT enabled FROM coreib_sec_role WHERE role_code=?", Boolean.class, code);
        return new RoleSummary(code, draft.displayName(), draft.description(), Boolean.TRUE.equals(enabled));
    }

    @Override
    @Transactional
    public void delete(DirectoryEntity entity, String id) {
        if (entity == DirectoryEntity.USER) {
            jdbc.update("DELETE FROM coreib_sec_record_assignment WHERE user_id=?", id);
            jdbc.update("DELETE FROM coreib_sec_user_supervisor WHERE user_id=? OR supervisor_id=?", id, id);
            jdbc.update("DELETE FROM coreib_sec_user_role WHERE user_id=?", id);
        } else if (entity == DirectoryEntity.ROLE) {
            jdbc.update("DELETE FROM coreib_sec_field_permission WHERE role_code=?", id);
            jdbc.update("DELETE FROM coreib_sec_permission WHERE role_code=?", id);
            jdbc.update("DELETE FROM coreib_sec_user_role WHERE role_code=?", id);
        }
        String table = switch (entity) {
            case ORGANIZATION -> "coreib_sys_organization";
            case USER -> "coreib_sys_user";
            case ROLE -> "coreib_sec_role";
        };
        String key = entity == DirectoryEntity.ROLE ? "role_code" : "id";
        int updated = jdbc.update("DELETE FROM " + table + " WHERE " + key + "=?", id);
        requireUpdated(updated, entity.name().toLowerCase(), id);
    }

    @Override
    @Transactional
    public void setEnabled(DirectoryEntity entity, String id, boolean enabled) {
        String table = switch (entity) {
            case ORGANIZATION -> "coreib_sys_organization";
            case USER -> "coreib_sys_user";
            case ROLE -> "coreib_sec_role";
        };
        String key = entity == DirectoryEntity.ROLE ? "role_code" : "id";
        int updated = jdbc.update("UPDATE " + table + " SET enabled = ? WHERE " + key + " = ?", enabled, id);
        if (updated == 0) throw new IllegalArgumentException("Directory entity not found: " + entity + "/" + id);
    }

    private static void requireSame(String pathId, String bodyId) {
        if (!pathId.equals(bodyId)) throw new IllegalArgumentException("Path id does not match body id");
    }

    private static void requireUpdated(int count, String type, String id) {
        if (count == 0) throw new IllegalArgumentException("Unknown " + type + ": " + id);
    }
}
