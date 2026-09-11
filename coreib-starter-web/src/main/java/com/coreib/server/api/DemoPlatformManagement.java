package com.coreib.server.api;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.PlatformManagement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mutable, fully usable platform fixture for local development without a database. */
final class DemoPlatformManagement implements PlatformManagement {
    private final Map<String, MenuEntry> menus = new LinkedHashMap<>();
    private final Map<String, PostEntry> posts = new LinkedHashMap<>();
    private final Map<String, DictionaryTypeEntry> dictionaryTypes = new LinkedHashMap<>();
    private final Map<String, DictionaryDataEntry> dictionaryData = new LinkedHashMap<>();
    private final Map<String, ConfigEntry> configs = new LinkedHashMap<>();
    private final Map<String, NoticeEntry> notices = new LinkedHashMap<>();
    private final List<AuditEvent> auditEvents = new ArrayList<>();
    private final List<LoginEvent> loginEvents = new ArrayList<>();

    DemoPlatformManagement() {
        DefaultPlatformMenus.entries().forEach(entry -> menus.put(entry.id(), entry));
        posts.put("nurse", new PostEntry("nurse", "NURSE", "护士", 10, true));
        posts.put("head-nurse", new PostEntry("head-nurse", "HEAD_NURSE", "护士长", 20, true));
        posts.put("it-staff", new PostEntry("it-staff", "IT_STAFF", "信息科科员", 30, true));
        dictionaryTypes.put("common-status", new DictionaryTypeEntry(
                "common-status", "common_status", "通用状态", "平台通用启停状态", true));
        dictionaryData.put("status-enabled", new DictionaryDataEntry(
                "status-enabled", "common_status", "启用", "1", 10, true));
        dictionaryData.put("status-disabled", new DictionaryDataEntry(
                "status-disabled", "common_status", "停用", "0", 20, true));
        configs.put("site-name", new ConfigEntry(
                "site-name", "coreib.site.name", "系统名称", "coreIb", true, "管理端显示名称"));
        configs.put("session-timeout", new ConfigEntry(
                "session-timeout", "coreib.session.timeout-minutes", "会话超时", "30", false, "单位：分钟"));
        notices.put("welcome", new NoticeEntry(
                "welcome", "coreIb 平台基座已就绪", "NOTICE", "PUBLISHED",
                "系统管理、权限、基础设施和代码生成模块已经启用。", Instant.now()));
        auditEvents.add(new AuditEvent(id(), "platform-admin-001", "START", "platform",
                "coreib", "SUCCESS", "demo platform initialized", Instant.now()));
        loginEvents.add(new LoginEvent(id(), "platform-admin-001", "platform.admin", "127.0.0.1",
                "SUCCESS", "演示管理员登录", Instant.now()));
    }

    @Override public synchronized List<MenuEntry> menus() { return List.copyOf(menus.values()); }
    @Override public synchronized List<PostEntry> posts() { return List.copyOf(posts.values()); }
    @Override public synchronized List<DictionaryTypeEntry> dictionaryTypes() { return List.copyOf(dictionaryTypes.values()); }
    @Override public synchronized List<DictionaryDataEntry> dictionaryData(String typeCode) {
        return dictionaryData.values().stream().filter(value -> typeCode == null || typeCode.isBlank() || value.typeCode().equals(typeCode)).toList();
    }
    @Override public synchronized List<ConfigEntry> configs() { return List.copyOf(configs.values()); }
    @Override public synchronized List<NoticeEntry> notices() { return List.copyOf(notices.values()); }
    @Override public synchronized List<AuditEvent> auditEvents() {
        List<AuditEvent> result = new ArrayList<>(auditEvents);
        java.util.Collections.reverse(result);
        return List.copyOf(result);
    }
    @Override public synchronized List<LoginEvent> loginEvents() {
        List<LoginEvent> result = new ArrayList<>(loginEvents);
        java.util.Collections.reverse(result);
        return List.copyOf(result);
    }

    @Override public synchronized MenuEntry saveMenu(MenuEntry entry) { menus.put(entry.id(), entry); audit("SAVE", "menu", entry.id()); return entry; }
    @Override public synchronized PostEntry savePost(PostEntry entry) { posts.put(entry.id(), entry); audit("SAVE", "post", entry.id()); return entry; }
    @Override public synchronized DictionaryTypeEntry saveDictionaryType(DictionaryTypeEntry entry) { dictionaryTypes.put(entry.id(), entry); audit("SAVE", "dictionary-type", entry.id()); return entry; }
    @Override public synchronized DictionaryDataEntry saveDictionaryData(DictionaryDataEntry entry) { dictionaryData.put(entry.id(), entry); audit("SAVE", "dictionary-data", entry.id()); return entry; }
    @Override public synchronized ConfigEntry saveConfig(ConfigEntry entry) { configs.put(entry.id(), entry); audit("SAVE", "config", entry.id()); return entry; }
    @Override public synchronized NoticeEntry saveNotice(NoticeEntry entry) { notices.put(entry.id(), entry); audit("SAVE", "notice", entry.id()); return entry; }
    @Override public synchronized void appendLoginEvent(LoginEvent event) { loginEvents.add(event); }

    @Override
    public synchronized void delete(ManagedEntity entity, String id) {
        switch (entity) {
            case MENU -> menus.remove(id);
            case POST -> posts.remove(id);
            case DICTIONARY_TYPE -> {
                DictionaryTypeEntry type = dictionaryTypes.remove(id);
                if (type != null) dictionaryData.values().removeIf(value -> value.typeCode().equals(type.code()));
            }
            case DICTIONARY_DATA -> dictionaryData.remove(id);
            case CONFIG -> configs.remove(id);
            case NOTICE -> notices.remove(id);
        }
        audit("DELETE", entity.name().toLowerCase(), id);
    }

    private void audit(String action, String resource, String target) {
        auditEvents.add(new AuditEvent(id(), "platform-admin-001", action, resource, target,
                "SUCCESS", "demo operation", Instant.now()));
    }

    private static String id() { return UUID.randomUUID().toString(); }
}
