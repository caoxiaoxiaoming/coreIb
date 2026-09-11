package com.coreib.server.api;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.PlatformManagement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Portable CRUD adapter using SQL shared by SQL Server, Oracle and PostgreSQL. */
final class JdbcPlatformManagement implements PlatformManagement {
    private final JdbcTemplate jdbc;

    JdbcPlatformManagement(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<MenuEntry> menus() {
        List<MenuEntry> result = jdbc.query("SELECT id,parent_id,menu_name,path,component,icon,permission_code,menu_type,sort_order,visible,enabled FROM coreib_sys_menu ORDER BY sort_order,menu_name",
                (r, n) -> new MenuEntry(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getString(7), r.getString(8), r.getInt(9), r.getBoolean(10), r.getBoolean(11)));
        return result.isEmpty() ? new DemoPlatformManagement().menus() : result;
    }
    @Override public List<PostEntry> posts() {
        return jdbc.query("SELECT id,post_code,post_name,sort_order,enabled FROM coreib_sys_post ORDER BY sort_order,post_name",
                (r, n) -> new PostEntry(r.getString(1), r.getString(2), r.getString(3), r.getInt(4), r.getBoolean(5)));
    }
    @Override public List<DictionaryTypeEntry> dictionaryTypes() {
        return jdbc.query("SELECT id,type_code,type_name,remark,enabled FROM coreib_sys_dict_type ORDER BY type_name",
                (r, n) -> new DictionaryTypeEntry(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getBoolean(5)));
    }
    @Override public List<DictionaryDataEntry> dictionaryData(String typeCode) {
        String base = "SELECT id,type_code,data_label,data_value,sort_order,enabled FROM coreib_sys_dict_data";
        return typeCode == null || typeCode.isBlank()
                ? jdbc.query(base + " ORDER BY type_code,sort_order", (r, n) -> mapDictionaryData(r))
                : jdbc.query(base + " WHERE type_code=? ORDER BY sort_order", (r, n) -> mapDictionaryData(r), typeCode);
    }
    @Override public List<ConfigEntry> configs() {
        return jdbc.query("SELECT id,config_key,config_name,config_value,public_visible,remark FROM coreib_sys_config ORDER BY config_name",
                (r, n) -> new ConfigEntry(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getBoolean(5), r.getString(6)));
    }
    @Override public List<NoticeEntry> notices() {
        return jdbc.query("SELECT id,title,notice_type,status,content,published_at FROM coreib_sys_notice ORDER BY created_at DESC",
                (r, n) -> new NoticeEntry(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), instant(r.getTimestamp(6))));
    }
    @Override public List<AuditEvent> auditEvents() {
        return jdbc.query("SELECT event_id,actor_id,action,resource,target_id,outcome,details,occurred_at FROM coreib_audit_event ORDER BY occurred_at DESC",
                (r, n) -> new AuditEvent(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getString(7), instant(r.getTimestamp(8))));
    }
    @Override public List<LoginEvent> loginEvents() {
        return jdbc.query("SELECT event_id,user_id,identity_name,source_ip,outcome,message,occurred_at FROM coreib_login_event ORDER BY occurred_at DESC",
                (r,n) -> new LoginEvent(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),instant(r.getTimestamp(7))));
    }
    @Override public void appendLoginEvent(LoginEvent event) {
        jdbc.update("INSERT INTO coreib_login_event (event_id,user_id,identity_name,source_ip,outcome,message,occurred_at) VALUES (?,?,?,?,?,?,?)",
                event.id(),event.userId(),event.identityName(),event.sourceIp(),event.outcome(),event.message(),Timestamp.from(event.occurredAt()));
    }

    @Override @Transactional public MenuEntry saveMenu(MenuEntry e) {
        upsert("coreib_sys_menu", e.id(),
                "UPDATE coreib_sys_menu SET parent_id=?,menu_name=?,path=?,component=?,icon=?,permission_code=?,menu_type=?,sort_order=?,visible=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                new Object[]{e.parentId(),e.name(),e.path(),e.component(),e.icon(),e.permission(),e.menuType(),e.sortOrder(),e.visible(),e.enabled(),e.id()},
                "INSERT INTO coreib_sys_menu (id,parent_id,menu_name,path,component,icon,permission_code,menu_type,sort_order,visible,enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                new Object[]{e.id(),e.parentId(),e.name(),e.path(),e.component(),e.icon(),e.permission(),e.menuType(),e.sortOrder(),e.visible(),e.enabled()});
        return e;
    }
    @Override @Transactional public PostEntry savePost(PostEntry e) {
        upsert("coreib_sys_post",e.id(),"UPDATE coreib_sys_post SET post_code=?,post_name=?,sort_order=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",new Object[]{e.code(),e.name(),e.sortOrder(),e.enabled(),e.id()},"INSERT INTO coreib_sys_post (id,post_code,post_name,sort_order,enabled,created_at,updated_at) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",new Object[]{e.id(),e.code(),e.name(),e.sortOrder(),e.enabled()}); return e;
    }
    @Override @Transactional public DictionaryTypeEntry saveDictionaryType(DictionaryTypeEntry e) {
        upsert("coreib_sys_dict_type",e.id(),"UPDATE coreib_sys_dict_type SET type_code=?,type_name=?,remark=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",new Object[]{e.code(),e.name(),e.remark(),e.enabled(),e.id()},"INSERT INTO coreib_sys_dict_type (id,type_code,type_name,remark,enabled,created_at,updated_at) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",new Object[]{e.id(),e.code(),e.name(),e.remark(),e.enabled()}); return e;
    }
    @Override @Transactional public DictionaryDataEntry saveDictionaryData(DictionaryDataEntry e) {
        upsert("coreib_sys_dict_data",e.id(),"UPDATE coreib_sys_dict_data SET type_code=?,data_label=?,data_value=?,sort_order=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",new Object[]{e.typeCode(),e.label(),e.value(),e.sortOrder(),e.enabled(),e.id()},"INSERT INTO coreib_sys_dict_data (id,type_code,data_label,data_value,sort_order,enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",new Object[]{e.id(),e.typeCode(),e.label(),e.value(),e.sortOrder(),e.enabled()}); return e;
    }
    @Override @Transactional public ConfigEntry saveConfig(ConfigEntry e) {
        upsert("coreib_sys_config",e.id(),"UPDATE coreib_sys_config SET config_key=?,config_name=?,config_value=?,public_visible=?,remark=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",new Object[]{e.configKey(),e.name(),e.value(),e.publicVisible(),e.remark(),e.id()},"INSERT INTO coreib_sys_config (id,config_key,config_name,config_value,public_visible,remark,created_at,updated_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",new Object[]{e.id(),e.configKey(),e.name(),e.value(),e.publicVisible(),e.remark()}); return e;
    }
    @Override @Transactional public NoticeEntry saveNotice(NoticeEntry e) {
        Timestamp published = e.publishedAt() == null ? null : Timestamp.from(e.publishedAt());
        upsert("coreib_sys_notice",e.id(),"UPDATE coreib_sys_notice SET title=?,notice_type=?,status=?,content=?,published_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",new Object[]{e.title(),e.noticeType(),e.status(),e.content(),published,e.id()},"INSERT INTO coreib_sys_notice (id,title,notice_type,status,content,published_at,created_at,updated_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",new Object[]{e.id(),e.title(),e.noticeType(),e.status(),e.content(),published}); return e;
    }

    @Override
    @Transactional
    public void delete(ManagedEntity entity, String id) {
        if (entity == ManagedEntity.DICTIONARY_TYPE) {
            List<String> codes = jdbc.query("SELECT type_code FROM coreib_sys_dict_type WHERE id=?", (r,n) -> r.getString(1), id);
            if (!codes.isEmpty()) jdbc.update("DELETE FROM coreib_sys_dict_data WHERE type_code=?", codes.get(0));
        }
        String table = switch (entity) {
            case MENU -> "coreib_sys_menu"; case POST -> "coreib_sys_post";
            case DICTIONARY_TYPE -> "coreib_sys_dict_type"; case DICTIONARY_DATA -> "coreib_sys_dict_data";
            case CONFIG -> "coreib_sys_config"; case NOTICE -> "coreib_sys_notice";
        };
        jdbc.update("DELETE FROM " + table + " WHERE id=?", id);
    }

    private void upsert(String table, String id, String updateSql, Object[] updateArgs, String insertSql, Object[] insertArgs) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=?", Integer.class, id);
        if (count != null && count > 0) jdbc.update(updateSql, updateArgs); else jdbc.update(insertSql, insertArgs);
    }
    private static DictionaryDataEntry mapDictionaryData(java.sql.ResultSet r) throws java.sql.SQLException {
        return new DictionaryDataEntry(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5),r.getBoolean(6));
    }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
