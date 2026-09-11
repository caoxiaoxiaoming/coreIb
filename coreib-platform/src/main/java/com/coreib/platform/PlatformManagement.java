package com.coreib.platform;

import java.time.Instant;
import java.util.List;

/** Administrative metadata required by a reusable business application foundation. */
public interface PlatformManagement {
    List<MenuEntry> menus();
    List<PostEntry> posts();
    List<DictionaryTypeEntry> dictionaryTypes();
    List<DictionaryDataEntry> dictionaryData(String typeCode);
    List<ConfigEntry> configs();
    List<NoticeEntry> notices();
    List<AuditEvent> auditEvents();
    List<LoginEvent> loginEvents();

    MenuEntry saveMenu(MenuEntry entry);
    PostEntry savePost(PostEntry entry);
    DictionaryTypeEntry saveDictionaryType(DictionaryTypeEntry entry);
    DictionaryDataEntry saveDictionaryData(DictionaryDataEntry entry);
    ConfigEntry saveConfig(ConfigEntry entry);
    NoticeEntry saveNotice(NoticeEntry entry);
    void appendLoginEvent(LoginEvent event);
    void delete(ManagedEntity entity, String id);

    enum ManagedEntity {
        MENU, POST, DICTIONARY_TYPE, DICTIONARY_DATA, CONFIG, NOTICE
    }

    record MenuEntry(String id, String parentId, String name, String path, String component,
                     String icon, String permission, String menuType, int sortOrder,
                     boolean visible, boolean enabled) {
        public MenuEntry {
            requireText("id", id);
            requireText("name", name);
            requireText("menuType", menuType);
            if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    record PostEntry(String id, String code, String name, int sortOrder, boolean enabled) {
        public PostEntry {
            requireText("id", id); requireText("code", code); requireText("name", name);
            if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    record DictionaryTypeEntry(String id, String code, String name, String remark, boolean enabled) {
        public DictionaryTypeEntry {
            requireText("id", id); requireText("code", code); requireText("name", name);
        }
    }

    record DictionaryDataEntry(String id, String typeCode, String label, String value,
                               int sortOrder, boolean enabled) {
        public DictionaryDataEntry {
            requireText("id", id); requireText("typeCode", typeCode);
            requireText("label", label); requireText("value", value);
            if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    record ConfigEntry(String id, String configKey, String name, String value,
                       boolean publicVisible, String remark) {
        public ConfigEntry {
            requireText("id", id); requireText("configKey", configKey);
            requireText("name", name); requireText("value", value);
        }
    }

    record NoticeEntry(String id, String title, String noticeType, String status,
                       String content, Instant publishedAt) {
        public NoticeEntry {
            requireText("id", id); requireText("title", title);
            requireText("noticeType", noticeType); requireText("status", status);
            content = content == null ? "" : content;
        }
    }

    record LoginEvent(String id, String userId, String identityName, String sourceIp,
                      String outcome, String message, Instant occurredAt) {
        public LoginEvent {
            requireText("id", id); requireText("userId", userId);
            requireText("identityName", identityName); requireText("outcome", outcome);
            occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        }
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
