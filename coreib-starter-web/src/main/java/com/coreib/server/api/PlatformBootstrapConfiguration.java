package com.coreib.server.api;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

/** Explicit, one-time administrator bootstrap for a new production database. */
@Configuration(proxyBeanMethods = false)
@Profile("!demo")
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "coreib.bootstrap.admin", name = "enabled", havingValue = "true")
public class PlatformBootstrapConfiguration {
    @Bean
    ApplicationRunner coreIbAdminBootstrap(
            DataSource dataSource,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            @Value("${coreib.bootstrap.admin.user-id:platform-admin-001}") String userId,
            @Value("${coreib.bootstrap.admin.login-name:platform.admin}") String loginName,
            @Value("${coreib.bootstrap.admin.display-name:平台管理员}") String displayName,
            @Value("${coreib.bootstrap.admin.external-identity}") String externalIdentity) {
        return arguments -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (externalIdentity == null || externalIdentity.isBlank()) {
                throw new IllegalStateException("coreib.bootstrap.admin.external-identity must not be blank");
            }
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            ensureOrganization(jdbc);
            ensureRole(jdbc);
            ensureUser(jdbc, userId, loginName, displayName, externalIdentity);
            ensureUserRole(jdbc, userId);
            ensurePermissions(jdbc);
            ensureDefaultMenus(jdbc);
        });
    }

    private static void ensureOrganization(JdbcTemplate jdbc) {
        if (count(jdbc, "coreib_sys_organization", "id", "coreib-root") == 0) {
            jdbc.update("INSERT INTO coreib_sys_organization (id,parent_id,org_code,display_name,org_type,sort_order,enabled,created_at,updated_at) VALUES (?,NULL,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    "coreib-root", "ROOT", "默认机构", "ROOT", 0, true);
        }
    }
    private static void ensureRole(JdbcTemplate jdbc) {
        if (count(jdbc, "coreib_sec_role", "role_code", "platform-admin") == 0) {
            jdbc.update("INSERT INTO coreib_sec_role (role_code,display_name,description,enabled,created_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",
                    "platform-admin", "平台管理员", "coreIb platform administrator", true);
        }
    }
    private static void ensureUser(JdbcTemplate jdbc, String id, String login, String display, String external) {
        if (count(jdbc, "coreib_sys_user", "id", id) == 0) {
            jdbc.update("INSERT INTO coreib_sys_user (id,login_name,display_name,organization_id,external_identity,enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    id, login, display, "coreib-root", external, true);
        } else {
            jdbc.update("UPDATE coreib_sys_user SET login_name=?,display_name=?,external_identity=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    login, display, external, true, id);
        }
    }
    private static void ensureUserRole(JdbcTemplate jdbc, String userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM coreib_sec_user_role WHERE user_id=? AND role_code=?", Integer.class, userId, "platform-admin");
        if (count == null || count == 0) jdbc.update("INSERT INTO coreib_sec_user_role (id,user_id,role_code,created_at) VALUES (?,?,?,CURRENT_TIMESTAMP)", UUID.randomUUID().toString(), userId, "platform-admin");
    }
    private static void ensurePermissions(JdbcTemplate jdbc) {
        List<Grant> grants = List.of(
                new Grant("platform-directory", "READ"), new Grant("platform-directory", "UPDATE"), new Grant("platform-directory", "DELETE"),
                new Grant("security-administration", "READ"), new Grant("security-administration", "UPDATE"),
                new Grant("platform-management", "READ"), new Grant("platform-management", "UPDATE"), new Grant("platform-management", "DELETE"),
                new Grant("file-management", "READ"), new Grant("file-management", "CREATE"), new Grant("file-management", "DELETE"),
                new Grant("code-generation", "READ"), new Grant("code-generation", "EXPORT"));
        for (Grant grant : grants) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM coreib_sec_permission WHERE role_code=? AND resource=? AND action=?", Integer.class, "platform-admin", grant.resource(), grant.action());
            if (count == null || count == 0) jdbc.update("INSERT INTO coreib_sec_permission (id,role_code,resource,action,row_scope,policy_key,enabled,created_at) VALUES (?,?,?,?,?,NULL,?,CURRENT_TIMESTAMP)", UUID.randomUUID().toString(), "platform-admin", grant.resource(), grant.action(), "ALL", true);
        }
    }
    static void ensureDefaultMenus(JdbcTemplate jdbc) {
        for (com.coreib.platform.PlatformManagement.MenuEntry menu : DefaultPlatformMenus.entries()) {
            if (count(jdbc, "coreib_sys_menu", "id", menu.id()) == 0) {
                jdbc.update("INSERT INTO coreib_sys_menu (id,parent_id,menu_name,path,component,icon,permission_code,menu_type,sort_order,visible,enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                        menu.id(), menu.parentId(), menu.name(), menu.path(), menu.component(), menu.icon(),
                        menu.permission(), menu.menuType(), menu.sortOrder(), menu.visible(), menu.enabled());
            }
        }
    }
    private static int count(JdbcTemplate jdbc, String table, String column, String value) {
        Integer result = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + "=?", Integer.class, value);
        return result == null ? 0 : result;
    }
    private record Grant(String resource, String action) { }
}
