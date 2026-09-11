package com.coreib.server.api;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformBootstrapConfigurationTest {
    @Test
    void seedsMissingPlatformMenusWithoutOverwritingExistingConfiguration() {
        var database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .build();
        try {
            var jdbc = new JdbcTemplate(database);
            jdbc.execute("""
                    CREATE TABLE coreib_sys_menu (
                      id VARCHAR(64) PRIMARY KEY,
                      parent_id VARCHAR(64),
                      menu_name VARCHAR(128) NOT NULL,
                      path VARCHAR(256),
                      component VARCHAR(256),
                      icon VARCHAR(64),
                      permission_code VARCHAR(256),
                      menu_type VARCHAR(32) NOT NULL,
                      sort_order INT NOT NULL,
                      visible BOOLEAN NOT NULL,
                      enabled BOOLEAN NOT NULL,
                      created_at TIMESTAMP NOT NULL,
                      updated_at TIMESTAMP NOT NULL
                    )
                    """);
            jdbc.update("INSERT INTO coreib_sys_menu VALUES (?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    "users", "system-admin", "自定义用户入口", "/custom-users", "CustomUsers", "User",
                    "platform-directory:READ", "MENU", 5, true, true);

            PlatformBootstrapConfiguration.ensureDefaultMenus(jdbc);
            PlatformBootstrapConfiguration.ensureDefaultMenus(jdbc);

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM coreib_sys_menu", Integer.class)).isEqualTo(14);
            assertThat(jdbc.queryForObject("SELECT menu_name FROM coreib_sys_menu WHERE id='users'", String.class))
                    .isEqualTo("自定义用户入口");
            assertThat(jdbc.queryForObject("SELECT parent_id FROM coreib_sys_menu WHERE id='codegen'", String.class))
                    .isEqualTo("infrastructure");
        } finally {
            database.shutdown();
        }
    }
}
