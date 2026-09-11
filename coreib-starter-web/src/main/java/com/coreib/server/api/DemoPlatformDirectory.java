package com.coreib.server.api;

import com.coreib.platform.DirectoryEntity;
import com.coreib.platform.OrganizationDraft;
import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.PlatformDirectory;
import com.coreib.platform.PlatformDirectoryCommands;
import com.coreib.platform.RoleDraft;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserDraft;
import com.coreib.platform.UserSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared mutable directory used by the no-database demo profile. */
final class DemoPlatformDirectory implements PlatformDirectory, PlatformDirectoryCommands {
    private final Map<String, OrganizationSummary> organizations = new LinkedHashMap<>();
    private final Map<String, UserSummary> users = new LinkedHashMap<>();
    private final Map<String, RoleSummary> roles = new LinkedHashMap<>();

    DemoPlatformDirectory() {
        organizations.put("hospital", new OrganizationSummary("hospital", null, "ROOT", "示例机构", "ROOT", true));
        organizations.put("ward-a", new OrganizationSummary("ward-a", "hospital", "WARD-A", "一病区", "WARD", true));
        organizations.put("information", new OrganizationSummary("information", "hospital", "IT", "信息科", "DEPARTMENT", true));
        roles.put("platform-admin", new RoleSummary("platform-admin", "平台管理员", "管理平台全部模块", true));
        roles.put("nurse", new RoleSummary("nurse", "护士", "查看本人负责的数据", true));
        roles.put("head-nurse", new RoleSummary("head-nurse", "护士长", "查看管理人员及其负责的数据", true));
        roles.put("information-clerk", new RoleSummary("information-clerk", "信息科普通科员", "只读业务数据", true));
        roles.put("information-leader", new RoleSummary("information-leader", "信息科领导", "读取和修改业务数据", true));
        users.put("platform-admin-001", user("platform-admin-001", "platform.admin", "平台管理员", "information", Set.of("platform-admin")));
        users.put("nurse-001", user("nurse-001", "nurse.001", "示例护士", "ward-a", Set.of("nurse")));
        users.put("head-nurse-001", user("head-nurse-001", "head.nurse.001", "示例护士长", "ward-a", Set.of("head-nurse")));
        users.put("info-clerk-001", user("info-clerk-001", "info.clerk.001", "信息科科员", "information", Set.of("information-clerk")));
        users.put("info-leader-001", user("info-leader-001", "info.leader.001", "信息科负责人", "information", Set.of("information-leader")));
    }

    @Override public synchronized List<OrganizationSummary> organizations() { return List.copyOf(organizations.values()); }
    @Override public synchronized List<UserSummary> users() { return List.copyOf(users.values()); }
    @Override public synchronized List<RoleSummary> roles() { return List.copyOf(roles.values()); }

    @Override public synchronized OrganizationSummary createOrganization(OrganizationDraft draft) {
        requireAbsent(organizations, "organization", draft.id());
        return saveOrganization(draft.id(), draft);
    }
    @Override public synchronized UserSummary createUser(UserDraft draft) {
        requireAbsent(users, "user", draft.id());
        return saveUser(draft.id(), draft);
    }
    @Override public synchronized RoleSummary createRole(RoleDraft draft) {
        requireAbsent(roles, "role", draft.code());
        return saveRole(draft.code(), draft);
    }
    @Override public synchronized OrganizationSummary updateOrganization(String id, OrganizationDraft draft) { requirePresent(organizations, "organization", id); return saveOrganization(id, draft); }
    @Override public synchronized UserSummary updateUser(String id, UserDraft draft) { requirePresent(users, "user", id); return saveUser(id, draft); }
    @Override public synchronized RoleSummary updateRole(String code, RoleDraft draft) { requirePresent(roles, "role", code); return saveRole(code, draft); }

    @Override public synchronized void setEnabled(DirectoryEntity entity, String id, boolean enabled) {
        switch (entity) {
            case ORGANIZATION -> {
                OrganizationSummary current = required(organizations, "organization", id);
                organizations.put(id, new OrganizationSummary(current.id(), current.parentId(), current.code(), current.displayName(), current.type(), enabled));
            }
            case USER -> {
                UserSummary current = required(users, "user", id);
                users.put(id, new UserSummary(current.id(), current.loginName(), current.displayName(), current.organizationId(), current.organizationName(), enabled, current.roleCodes()));
            }
            case ROLE -> {
                RoleSummary current = required(roles, "role", id);
                roles.put(id, new RoleSummary(current.code(), current.displayName(), current.description(), enabled));
            }
        }
    }

    @Override public synchronized void delete(DirectoryEntity entity, String id) {
        switch (entity) {
            case ORGANIZATION -> {
                if (organizations.values().stream().anyMatch(value -> id.equals(value.parentId()))) throw new IllegalArgumentException("Organization has children");
                if (users.values().stream().anyMatch(value -> id.equals(value.organizationId()))) throw new IllegalArgumentException("Organization has users");
                requireRemoved(organizations, "organization", id);
            }
            case USER -> requireRemoved(users, "user", id);
            case ROLE -> {
                if (users.values().stream().anyMatch(value -> value.roleCodes().contains(id))) throw new IllegalArgumentException("Role is assigned to users");
                requireRemoved(roles, "role", id);
            }
        }
    }

    private OrganizationSummary saveOrganization(String id, OrganizationDraft d) {
        if (d.parentId() != null && !d.parentId().isBlank()) requirePresent(organizations, "parent organization", d.parentId());
        OrganizationSummary value = new OrganizationSummary(id, blankToNull(d.parentId()), d.code(), d.displayName(), d.type(), organizations.getOrDefault(id, new OrganizationSummary(id, null, d.code(), d.displayName(), d.type(), true)).enabled());
        organizations.put(id, value); return value;
    }
    private UserSummary saveUser(String id, UserDraft d) {
        if (d.organizationId() != null && !d.organizationId().isBlank()) requirePresent(organizations, "organization", d.organizationId());
        d.roleCodes().forEach(role -> requirePresent(roles, "role", role));
        UserSummary previous = users.get(id);
        UserSummary value = new UserSummary(id, d.loginName(), d.displayName(), blankToNull(d.organizationId()), organizationName(d.organizationId()), previous == null || previous.enabled(), d.roleCodes());
        users.put(id, value); return value;
    }
    private RoleSummary saveRole(String code, RoleDraft d) {
        RoleSummary previous = roles.get(code);
        RoleSummary value = new RoleSummary(code, d.displayName(), d.description(), previous == null || previous.enabled());
        roles.put(code, value); return value;
    }
    private String organizationName(String id) { OrganizationSummary value = id == null ? null : organizations.get(id); return value == null ? null : value.displayName(); }
    private UserSummary user(String id, String login, String name, String org, Set<String> roleCodes) { return new UserSummary(id, login, name, org, organizationName(org), true, roleCodes); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static <T> void requireAbsent(Map<String,T> map, String type, String id) { if (map.containsKey(id)) throw new IllegalArgumentException(type + " already exists: " + id); }
    private static <T> void requirePresent(Map<String,T> map, String type, String id) { if (!map.containsKey(id)) throw new IllegalArgumentException("Unknown " + type + ": " + id); }
    private static <T> T required(Map<String,T> map, String type, String id) { requirePresent(map,type,id); return map.get(id); }
    private static <T> void requireRemoved(Map<String,T> map, String type, String id) { if (map.remove(id) == null) throw new IllegalArgumentException("Unknown " + type + ": " + id); }
}
