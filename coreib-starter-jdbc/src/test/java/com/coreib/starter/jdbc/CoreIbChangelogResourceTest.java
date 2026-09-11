package com.coreib.starter.jdbc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CoreIbChangelogResourceTest {
    @Test
    void containsPortableBaselineAndRollback() throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("db/changelog/db.changelog-master.yaml")) {
            assertThat(input).as("Liquibase master changelog").isNotNull();
            String changelog = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(changelog).contains("tableName: coreib_platform_metadata");
            assertThat(changelog).contains("tableName: coreib_sec_user_supervisor");
            assertThat(changelog).contains("tableName: coreib_sec_record_assignment");
            assertThat(changelog).contains("tableName: coreib_sec_field_permission");
            assertThat(changelog).contains("tableName: coreib_sys_organization");
            assertThat(changelog).contains("tableName: coreib_sys_user");
            assertThat(changelog).contains("tableName: coreib_audit_event");
            assertThat(changelog).contains("id: coreib-005-security-integrity");
            assertThat(changelog).contains("id: coreib-006-platform-management");
            assertThat(changelog).contains("tableName: coreib_sys_menu");
            assertThat(changelog).contains("tableName: coreib_sys_dict_type");
            assertThat(changelog).contains("tableName: coreib_sys_config");
            assertThat(changelog).contains("tableName: coreib_sys_notice");
            assertThat(changelog).contains("tableName: coreib_login_event");
            assertThat(changelog).contains("tableName: coreib_file_object");
            assertThat(changelog).contains("addForeignKeyConstraint:");
            assertThat(changelog).contains("constraintName: uk_cib_user_role");
            assertThat(changelog).contains("indexName: idx_cib_user_external");
            assertThat(changelog).contains("type: datetime");
            assertThat(changelog).contains("dropTable:");
        }
    }
}
