package com.coreib.server.api;

import com.coreib.platform.PlatformManagement;

import java.util.List;

final class DefaultPlatformMenus {
    private DefaultPlatformMenus() {
    }

    static List<PlatformManagement.MenuEntry> entries() {
        return List.of(
                menu("system-admin", null, "系统管理", "", "", "Setting", "", "DIRECTORY", 10),
                menu("users", "system-admin", "用户管理", "/system/users", "UserManagementView", "User", "platform-directory:READ", "MENU", 10),
                menu("roles", "system-admin", "角色管理", "/system/roles", "RoleManagementView", "Avatar", "platform-directory:READ", "MENU", 20),
                menu("menus", "system-admin", "菜单管理", "/system/menus", "MenuManagementView", "Menu", "platform-management:READ", "MENU", 30),
                menu("departments", "system-admin", "部门管理", "/system/departments", "DepartmentManagementView", "OfficeBuilding", "platform-directory:READ", "MENU", 40),
                menu("posts", "system-admin", "岗位管理", "/system/posts", "PostManagementView", "Postcard", "platform-management:READ", "MENU", 50),
                menu("dictionaries", "system-admin", "字典管理", "/system/dictionaries", "DictionaryManagementView", "Collection", "platform-management:READ", "MENU", 60),
                menu("configs", "system-admin", "参数配置", "/system/configs", "ConfigManagementView", "Tools", "platform-management:READ", "MENU", 70),
                menu("notices", "system-admin", "通知公告", "/system/notices", "NoticeManagementView", "Bell", "platform-management:READ", "MENU", 80),
                menu("infrastructure", null, "基础设施", "", "", "Cpu", "", "DIRECTORY", 20),
                menu("audit-logs", "infrastructure", "操作日志", "/infrastructure/audit-logs", "AuditLogView", "Document", "platform-management:READ", "MENU", 10),
                menu("login-logs", "infrastructure", "登录日志", "/infrastructure/login-logs", "LoginLogView", "Tickets", "platform-management:READ", "MENU", 20),
                menu("files", "infrastructure", "文件管理", "/infrastructure/files", "FileManagementView", "Folder", "file-management:READ", "MENU", 30),
                menu("codegen", "infrastructure", "代码生成", "/infrastructure/codegen", "CodeGenerationView", "MagicStick", "code-generation:READ", "MENU", 40)
        );
    }

    private static PlatformManagement.MenuEntry menu(
            String id,
            String parentId,
            String name,
            String path,
            String component,
            String icon,
            String permission,
            String type,
            int sortOrder) {
        return new PlatformManagement.MenuEntry(
                id, parentId, name, path, component, icon, permission, type, sortOrder, true, true);
    }
}
