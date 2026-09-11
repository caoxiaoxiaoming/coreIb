package com.coreib.server;

import com.coreib.security.CoreIbAuthorizationService;
import com.coreib.security.CurrentAuthorizationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
class DemoProfileApplicationTest {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private WebApplicationContext webContext;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void registersOneDemoAuthorizationProviderAndTheBackendFacade() {
        assertThat(context.getBean(CurrentAuthorizationProvider.class)).isNotNull();
        assertThat(context.containsBean("anonymousPermissionProvider")).isFalse();
        assertThat(context.getBean(CoreIbAuthorizationService.class)).isNotNull();
    }

    @Test
    void exposesSecurityConfigurationOnlyToThePlatformAdministrator() throws Exception {
        mvc.perform(get("/api/v1/platform/security")
                        .header("X-CoreIb-Demo-Subject", "platform-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.userRoles").isArray());

        mvc.perform(get("/api/v1/platform/security")
                        .header("X-CoreIb-Demo-Subject", "nurse"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void exposesCompletePlatformManagementAndCodeGenerationToAdministrator() throws Exception {
        mvc.perform(get("/api/v1/platform/management/menus")
                        .header("X-CoreIb-Demo-Subject", "platform-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'users')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'codegen')]").exists());

        mvc.perform(get("/api/v1/code-generation/tables")
                        .header("X-CoreIb-Demo-Subject", "platform-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tableName").value("coreib_patient"));

        mvc.perform(get("/api/v1/code-generation/preview")
                        .param("tableName", "coreib_patient")
                        .param("moduleName", "patient")
                        .header("X-CoreIb-Demo-Subject", "platform-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(6))
                .andExpect(jsonPath("$.columns[0].primaryKey").value(true));

        mvc.perform(get("/api/v1/platform/management/menus")
                        .header("X-CoreIb-Demo-Subject", "nurse"))
                .andExpect(status().isForbidden());
    }

    @Test
    void performsDirectoryAndParameterCrudInDemoProfile() throws Exception {
        mvc.perform(post("/api/v1/platform/directory/roles")
                        .header("X-CoreIb-Demo-Subject", "platform-admin")
                        .contentType("application/json")
                        .content("""
                                {"code":"test-role","displayName":"测试角色","description":"integration"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("test-role"));

        mvc.perform(put("/api/v1/platform/management/configs/test-config")
                        .header("X-CoreIb-Demo-Subject", "platform-admin")
                        .contentType("application/json")
                        .content("""
                                {"id":"test-config","configKey":"test.key","name":"测试参数","value":"ok","publicVisible":false,"remark":"integration"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("ok"));

        mvc.perform(get("/api/v1/platform/management/configs")
                        .header("X-CoreIb-Demo-Subject", "platform-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'test-config')]").exists());
    }
}
