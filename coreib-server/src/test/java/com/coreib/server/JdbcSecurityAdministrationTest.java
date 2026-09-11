package com.coreib.server;

import com.coreib.platform.SecurityAdministration;
import com.coreib.security.ColumnAccess;
import com.coreib.security.PermissionAction;
import com.coreib.security.RowScope;
import com.coreib.server.api.JdbcSecurityAdministration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcSecurityAdministrationTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private JdbcSecurityAdministration administration;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE coreib_sec_role (role_code VARCHAR(64) PRIMARY KEY)");
        jdbc.execute("CREATE TABLE coreib_sys_user (id VARCHAR(64) PRIMARY KEY)");
        jdbc.execute("CREATE TABLE coreib_sec_user_role (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), role_code VARCHAR(64), created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_permission (id VARCHAR(64) PRIMARY KEY, role_code VARCHAR(64), resource VARCHAR(128), action VARCHAR(32), row_scope VARCHAR(32), policy_key VARCHAR(128), enabled BOOLEAN, created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_field_permission (id VARCHAR(64) PRIMARY KEY, role_code VARCHAR(64), resource VARCHAR(128), field_name VARCHAR(128), access_mode VARCHAR(32), created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_user_supervisor (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), supervisor_id VARCHAR(64), relation_type VARCHAR(64), active BOOLEAN, effective_from TIMESTAMP, effective_to TIMESTAMP, created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_record_assignment (id VARCHAR(64) PRIMARY KEY, resource VARCHAR(128), record_id VARCHAR(128), user_id VARCHAR(64), assignment_type VARCHAR(64), active BOOLEAN, effective_from TIMESTAMP, effective_to TIMESTAMP, created_at TIMESTAMP)");
        jdbc.update("INSERT INTO coreib_sec_role VALUES ('nurse')");
        jdbc.update("INSERT INTO coreib_sec_role VALUES ('head-nurse')");
        jdbc.update("INSERT INTO coreib_sys_user VALUES ('nurse-1')");
        jdbc.update("INSERT INTO coreib_sys_user VALUES ('head-1')");
        administration = new JdbcSecurityAdministration(jdbc);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void replacesRolePoliciesAndUserRoles() {
        administration.replaceRolePolicy("nurse", new SecurityAdministration.RolePolicyDraft(
                List.of(new SecurityAdministration.PermissionGrantDraft(
                        "patient", PermissionAction.READ, RowScope.RELATED_RECORDS, "assigned", true)),
                List.of(new SecurityAdministration.FieldGrantDraft(
                        "patient", "idCard", ColumnAccess.MASKED))));
        administration.replaceUserRoles("nurse-1", new SecurityAdministration.UserRolesDraft(Set.of("nurse")));

        SecurityAdministration.SecurityConfiguration configuration = administration.configuration();
        assertThat(configuration.permissions()).singleElement().satisfies(permission -> {
            assertThat(permission.roleCode()).isEqualTo("nurse");
            assertThat(permission.rowScope()).isEqualTo(RowScope.RELATED_RECORDS);
            assertThat(permission.policyKey()).isEqualTo("assigned");
        });
        assertThat(configuration.fields()).singleElement()
                .extracting(SecurityAdministration.FieldPermissionGrant::accessMode)
                .isEqualTo(ColumnAccess.MASKED);
        assertThat(configuration.userRoles()).containsExactly(
                new SecurityAdministration.UserRoleAssignment("nurse-1", "nurse"));
    }

    @Test
    void upsertsRelationshipsAndAssignmentsAndCanDeactivateThem() {
        SecurityAdministration.SupervisorRelationship first = administration.saveSupervisorRelationship(
                new SecurityAdministration.SupervisorRelationshipDraft(
                        "nurse-1", "head-1", "DIRECT", true, null, null));
        SecurityAdministration.SupervisorRelationship second = administration.saveSupervisorRelationship(
                new SecurityAdministration.SupervisorRelationshipDraft(
                        "nurse-1", "head-1", "DIRECT", false, null, null));
        SecurityAdministration.RecordAssignment assignment = administration.saveRecordAssignment(
                new SecurityAdministration.RecordAssignmentDraft(
                        "patient", "patient-1", "nurse-1", "PRIMARY", true, null, null));
        administration.setRecordAssignmentActive(assignment.id(), false);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(administration.configuration().supervisorRelationships()).singleElement()
                .extracting(SecurityAdministration.SupervisorRelationship::active).isEqualTo(false);
        assertThat(administration.configuration().recordAssignments()).singleElement()
                .extracting(SecurityAdministration.RecordAssignment::active).isEqualTo(false);
    }

    @Test
    void rejectsUnknownRoleOrUser() {
        assertThatThrownBy(() -> administration.replaceUserRoles(
                "missing", new SecurityAdministration.UserRolesDraft(Set.of("nurse"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown user");
        assertThatThrownBy(() -> administration.replaceRolePolicy(
                "missing", new SecurityAdministration.RolePolicyDraft(List.of(), List.of())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown role");
    }
}
