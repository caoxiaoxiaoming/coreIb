package com.coreib.server;

import com.coreib.server.api.JdbcPlatformDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPlatformDirectoryTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE coreib_sys_organization (id VARCHAR(64) PRIMARY KEY, parent_id VARCHAR(64), org_code VARCHAR(64), display_name VARCHAR(128), org_type VARCHAR(32), sort_order INT, enabled BOOLEAN, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sys_user (id VARCHAR(64) PRIMARY KEY, login_name VARCHAR(128), display_name VARCHAR(128), organization_id VARCHAR(64), external_identity VARCHAR(256), enabled BOOLEAN, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_user_role (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), role_code VARCHAR(64), created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_role (role_code VARCHAR(64) PRIMARY KEY, display_name VARCHAR(128), description VARCHAR(512), enabled BOOLEAN, created_at TIMESTAMP)");
        jdbc.update("INSERT INTO coreib_sys_organization VALUES ('root', NULL, 'ROOT', '总部', 'ROOT', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO coreib_sys_user VALUES ('u1', 'admin', '管理员', 'root', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO coreib_sec_role VALUES ('platform-admin', '平台管理员', '管理平台目录', TRUE, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur1', 'u1', 'platform-admin', CURRENT_TIMESTAMP)");
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void mapsOrganizationsUsersAndRolesWithUserRoleAggregation() {
        var directory = new JdbcPlatformDirectory(jdbc);

        assertThat(directory.organizations()).extracting("code").containsExactly("ROOT");
        assertThat(directory.users()).singleElement().satisfies(user -> {
            assertThat(user.loginName()).isEqualTo("admin");
            assertThat(user.roleCodes()).containsExactly("platform-admin");
        });
        assertThat(directory.roles()).singleElement().extracting("code").isEqualTo("platform-admin");
    }
}
