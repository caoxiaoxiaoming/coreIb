package com.coreib.server;

import com.coreib.security.CoreIbAuthorizationService;
import com.coreib.security.CoreIbPermissionEvaluator;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionPolicy;
import com.coreib.security.PermissionRow;
import com.coreib.security.PermissionSnapshot;
import com.coreib.security.PermissionSubject;
import com.coreib.security.RowScope;
import com.coreib.server.api.JdbcPermissionSnapshotProvider;
import com.coreib.server.api.JdbcPrincipalLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPermissionSnapshotProviderTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE coreib_sys_organization (id VARCHAR(64) PRIMARY KEY, parent_id VARCHAR(64), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE coreib_sys_user (id VARCHAR(64) PRIMARY KEY, login_name VARCHAR(128), external_identity VARCHAR(256), organization_id VARCHAR(64), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE coreib_sec_role (role_code VARCHAR(64) PRIMARY KEY, enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE coreib_sec_user_role (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), role_code VARCHAR(64))");
        jdbc.execute("CREATE TABLE coreib_sec_permission (id VARCHAR(64) PRIMARY KEY, role_code VARCHAR(64), resource VARCHAR(128), action VARCHAR(32), row_scope VARCHAR(32), policy_key VARCHAR(128), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE coreib_sec_field_permission (id VARCHAR(64) PRIMARY KEY, role_code VARCHAR(64), resource VARCHAR(128), field_name VARCHAR(128), access_mode VARCHAR(32))");
        jdbc.execute("CREATE TABLE coreib_sec_user_supervisor (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), supervisor_id VARCHAR(64), active BOOLEAN, effective_from TIMESTAMP, effective_to TIMESTAMP)");
        jdbc.execute("CREATE TABLE coreib_sec_record_assignment (id VARCHAR(64) PRIMARY KEY, resource VARCHAR(128), record_id VARCHAR(128), user_id VARCHAR(64), active BOOLEAN, effective_from TIMESTAMP, effective_to TIMESTAMP)");

        jdbc.update("INSERT INTO coreib_sys_organization VALUES ('hospital', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sys_organization VALUES ('ward-a', 'hospital', TRUE)");
        jdbc.update("INSERT INTO coreib_sys_organization VALUES ('information', 'hospital', TRUE)");
        for (String user : new String[]{"nurse-001", "nurse-002", "head-nurse-001"}) {
            jdbc.update("INSERT INTO coreib_sys_user VALUES (?, ?, ?, 'ward-a', TRUE)",
                    user, user + "@local", "oidc:" + user);
        }
        for (String user : new String[]{"info-clerk-001", "info-leader-001"}) {
            jdbc.update("INSERT INTO coreib_sys_user VALUES (?, ?, ?, 'information', TRUE)",
                    user, user + "@local", "oidc:" + user);
        }
        jdbc.update("INSERT INTO coreib_sys_user VALUES ('disabled-001', 'disabled@local', 'oidc:disabled', 'ward-a', FALSE)");

        for (String role : new String[]{"nurse", "head-nurse", "information-clerk", "information-leader", "ward-reader", "patient-editor"}) {
            jdbc.update("INSERT INTO coreib_sec_role VALUES (?, TRUE)", role);
        }
        jdbc.update("INSERT INTO coreib_sec_role VALUES ('disabled-admin', FALSE)");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-nurse', 'nurse-001', 'nurse')");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-head', 'head-nurse-001', 'head-nurse')");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-clerk', 'info-clerk-001', 'information-clerk')");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-leader', 'info-leader-001', 'information-leader')");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-head-ward', 'head-nurse-001', 'ward-reader')");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-head-editor', 'head-nurse-001', 'patient-editor')");
        jdbc.update("INSERT INTO coreib_sec_user_role VALUES ('ur-head-disabled', 'head-nurse-001', 'disabled-admin')");

        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-nurse', 'nurse', 'patient', 'READ', 'RELATED_RECORDS', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-head', 'head-nurse', 'patient', 'READ', 'MANAGED_USERS', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-clerk', 'information-clerk', 'business-data', 'READ', 'ORGANIZATION', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-leader-read', 'information-leader', 'business-data', 'READ', 'ORGANIZATION', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-leader-update', 'information-leader', 'business-data', 'UPDATE', 'ORGANIZATION', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-head-ward', 'ward-reader', 'patient', 'READ', 'ORGANIZATION', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-head-update', 'patient-editor', 'patient', 'UPDATE', 'SELF', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-disabled', 'patient-editor', 'patient', 'DELETE', 'ALL', NULL, FALSE)");
        jdbc.update("INSERT INTO coreib_sec_permission VALUES ('p-disabled-role', 'disabled-admin', 'patient', 'DELETE', 'ALL', NULL, TRUE)");
        jdbc.update("INSERT INTO coreib_sec_field_permission VALUES ('f-nurse-name', 'nurse', 'patient', 'name', 'READ')");
        jdbc.update("INSERT INTO coreib_sec_field_permission VALUES ('f-nurse-card', 'nurse', 'patient', 'idCard', 'MASKED')");
        jdbc.update("INSERT INTO coreib_sec_field_permission VALUES ('f-leader-amount', 'information-leader', 'business-data', 'amount', 'WRITE')");

        jdbc.update("INSERT INTO coreib_sec_user_supervisor VALUES ('s1', 'nurse-001', 'head-nurse-001', TRUE, NULL, NULL)");
        jdbc.update("INSERT INTO coreib_sec_user_supervisor VALUES ('s2', 'nurse-002', 'head-nurse-001', TRUE, NULL, NULL)");
        jdbc.update("INSERT INTO coreib_sec_record_assignment VALUES ('a1', 'patient', 'patient-001', 'nurse-001', TRUE, NULL, NULL)");
        Timestamp expired = Timestamp.from(Instant.now().minusSeconds(3600));
        jdbc.update("INSERT INTO coreib_sec_user_supervisor VALUES ('s-expired', 'nurse-expired', 'head-nurse-001', TRUE, NULL, ?)", expired);
        jdbc.update("INSERT INTO coreib_sec_record_assignment VALUES ('a-expired', 'patient', 'patient-expired', 'nurse-001', TRUE, NULL, ?)", expired);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void loadsNurseAndHeadNurseManagementScopes() {
        JdbcPermissionSnapshotProvider nurseProvider = provider("nurse-001");
        JdbcPermissionSnapshotProvider headProvider = provider("head-nurse-001");
        PermissionSnapshot nurse = nurseProvider.current();
        PermissionSnapshot head = headProvider.current();

        assertThat(nurse.resources().get("patient").rowScope()).isEqualTo(RowScope.RELATED_RECORDS);
        assertThat(nurse.resources().get("patient").fields()).containsEntry("idCard", com.coreib.security.ColumnAccess.MASKED);
        PermissionSubject nurseSubject = nurseProvider.subject();
        assertThat(nurseSubject.relatedRecordIds()).containsExactly("patient-001");
        assertThat(new CoreIbPermissionEvaluator().decide(nurseSubject,
                new PermissionPolicy("patient", Set.of(PermissionAction.READ), RowScope.RELATED_RECORDS, Map.of()),
                PermissionAction.READ, PermissionRow.ownedBy("patient-001", "nurse-002", "ward-a")).allowed()).isTrue();
        assertThat(new CoreIbPermissionEvaluator().decide(nurseSubject,
                new PermissionPolicy("patient", Set.of(PermissionAction.READ), RowScope.RELATED_RECORDS, Map.of()),
                PermissionAction.READ, PermissionRow.ownedBy("patient-002", "nurse-002", "ward-a")).allowed()).isFalse();

        PermissionSubject headSubject = headProvider.subject();
        assertThat(head.resources().get("patient").rowScope()).isEqualTo(RowScope.CUSTOM);
        assertThat(headSubject.managedUserIds()).containsExactlyInAnyOrder("nurse-001", "nurse-002");
        assertThat(headSubject.visibleOrganizationIds()).containsExactly("ward-a");
        assertThat(new CoreIbPermissionEvaluator().decide(headSubject,
                new PermissionPolicy("patient", Set.of(PermissionAction.READ), RowScope.MANAGED_USERS, Map.of()),
                PermissionAction.READ, PermissionRow.ownedBy("patient-002", "nurse-002", "ward-a")).allowed()).isTrue();
    }

    @Test
    void preservesMultiRoleAndActionSpecificPoliciesForBackendAuthorization() {
        JdbcPermissionSnapshotProvider provider = provider("head-nurse-001");
        CoreIbAuthorizationService authorization = new CoreIbAuthorizationService(provider);

        assertThat(provider.policies("patient"))
                .extracting(PermissionPolicy::rowScope)
                .containsExactlyInAnyOrder(RowScope.MANAGED_USERS, RowScope.ORGANIZATION, RowScope.SELF);
        assertThat(authorization.rowCriteria("patient", PermissionAction.READ).scopes())
                .containsExactlyInAnyOrder(RowScope.MANAGED_USERS, RowScope.ORGANIZATION);
        assertThat(authorization.rowCriteria("patient", PermissionAction.UPDATE).scopes())
                .containsExactly(RowScope.SELF);
        assertThat(authorization.decide("patient", PermissionAction.READ,
                PermissionRow.ownedBy("p-org", "other", "ward-a")).allowed()).isTrue();
        assertThat(authorization.decide("patient", PermissionAction.READ,
                PermissionRow.ownedBy("p-managed", "nurse-001", "other-ward")).allowed()).isTrue();
        assertThat(authorization.decide("patient", PermissionAction.UPDATE,
                PermissionRow.ownedBy("p-other", "nurse-001", "ward-a")).allowed()).isFalse();
        assertThat(authorization.decide("patient", PermissionAction.UPDATE,
                PermissionRow.ownedBy("p-own", "head-nurse-001", "other-ward")).allowed()).isTrue();
        assertThat(provider.current().resources().get("patient").actions())
                .doesNotContain(PermissionAction.DELETE);
    }

    @Test
    void scopesRecordAssignmentsByResourceAndFiltersExpiredRelationships() {
        JdbcPermissionSnapshotProvider nurse = provider("nurse-001");
        JdbcPermissionSnapshotProvider head = provider("head-nurse-001");

        assertThat(nurse.subject("patient").relatedRecordIds()).containsExactly("patient-001");
        assertThat(nurse.subject("encounter").relatedRecordIds()).isEmpty();
        assertThat(head.subject("patient").managedUserIds())
                .containsExactlyInAnyOrder("nurse-001", "nurse-002")
                .doesNotContain("nurse-expired");
    }

    @Test
    void loadsReadOnlyClerkAndWritableLeader() {
        PermissionSnapshot clerk = snapshot("info-clerk-001");
        PermissionSnapshot leader = snapshot("info-leader-001");

        assertThat(clerk.resources().get("business-data").actions()).containsExactly(PermissionAction.READ);
        assertThat(leader.resources().get("business-data").actions())
                .containsExactlyInAnyOrder(PermissionAction.READ, PermissionAction.UPDATE);
        assertThat(leader.resources().get("business-data").fields())
                .containsEntry("amount", com.coreib.security.ColumnAccess.WRITE);
        PermissionSubject clerkSubject = provider("info-clerk-001").subject();
        PermissionPolicy readPolicy = new PermissionPolicy("business-data", Set.of(PermissionAction.READ), RowScope.ORGANIZATION, Map.of());
        assertThat(new CoreIbPermissionEvaluator().decide(clerkSubject, readPolicy, PermissionAction.READ,
                PermissionRow.ownedBy("b1", "nurse-001", "information")).allowed()).isTrue();
        assertThat(new CoreIbPermissionEvaluator().decide(clerkSubject, readPolicy, PermissionAction.READ,
                PermissionRow.ownedBy("b2", "nurse-001", "ward-a")).allowed()).isFalse();
    }

    @Test
    void anonymousOrUnknownPrincipalFailsClosed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(new JdbcPermissionSnapshotProvider(request, jdbc).current().authenticated()).isFalse();
        request.setUserPrincipal(() -> "missing");
        assertThat(new JdbcPermissionSnapshotProvider(request, jdbc).current().authenticated()).isFalse();
    }

    @Test
    void resolvesLoginNameToTheInternalPlatformSubject() {
        JdbcPermissionSnapshotProvider provider = provider(
                "nurse-001@local", JdbcPrincipalLookup.LOGIN_NAME);

        assertThat(provider.current().authenticated()).isTrue();
        assertThat(provider.current().subjectId()).isEqualTo("nurse-001");
        assertThat(provider.current().roleCodes()).containsExactly("nurse");
        assertThat(provider.subject("patient").relatedRecordIds()).containsExactly("patient-001");
    }

    @Test
    void resolvesExternalIdentityAndFailsClosedForUnknownOrDisabledIdentity() {
        JdbcPermissionSnapshotProvider known = provider(
                "oidc:info-leader-001", JdbcPrincipalLookup.EXTERNAL_IDENTITY);
        JdbcPermissionSnapshotProvider unknown = provider(
                "oidc:missing", JdbcPrincipalLookup.EXTERNAL_IDENTITY);
        JdbcPermissionSnapshotProvider disabled = provider(
                "oidc:disabled", JdbcPrincipalLookup.EXTERNAL_IDENTITY);

        assertThat(known.current().subjectId()).isEqualTo("info-leader-001");
        assertThat(known.current().roleCodes()).containsExactly("information-leader");
        assertThat(unknown.current().authenticated()).isFalse();
        assertThat(disabled.current().authenticated()).isFalse();
    }

    private PermissionSnapshot snapshot(String userId) {
        return provider(userId).current();
    }

    private JdbcPermissionSnapshotProvider provider(String userId) {
        return provider(userId, JdbcPrincipalLookup.ID);
    }

    private JdbcPermissionSnapshotProvider provider(String userId, JdbcPrincipalLookup lookup) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(() -> userId);
        return new JdbcPermissionSnapshotProvider(request, jdbc, lookup);
    }
}
