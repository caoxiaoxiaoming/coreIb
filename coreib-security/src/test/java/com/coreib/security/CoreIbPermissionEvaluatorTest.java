package com.coreib.security;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoreIbPermissionEvaluatorTest {
    private final CoreIbPermissionEvaluator evaluator = new CoreIbPermissionEvaluator();

    @Test
    void nurseCanReadOnlyAssignedPatients() {
        PermissionSubject nurse = new PermissionSubject(
                "nurse-1", "ward-a", Set.of("ward-a"), Set.of(), Set.of());
        PermissionPolicy policy = new PermissionPolicy(
                "patient", Set.of(PermissionAction.READ), RowScope.RELATED_RECORDS,
                Map.of("name", ColumnAccess.READ, "idCard", ColumnAccess.MASKED));

        assertThat(evaluator.decide(nurse, policy, PermissionAction.READ,
                new PermissionRow("patient-1", null, "ward-a", Set.of("nurse-1"), Set.of())))
                .extracting(PermissionDecision::allowed).isEqualTo(true);
        assertThat(evaluator.decide(nurse, policy, PermissionAction.READ,
                new PermissionRow("patient-2", null, "ward-a", Set.of("nurse-2"), Set.of())))
                .extracting(PermissionDecision::allowed).isEqualTo(false);
    }

    @Test
    void headNurseCanReadManagedNursesAndTheirPatients() {
        PermissionSubject headNurse = new PermissionSubject(
                "head-1", "ward-a", Set.of("ward-a"), Set.of("nurse-1", "nurse-2"), Set.of());
        PermissionPolicy policy = new PermissionPolicy(
                "patient", Set.of(PermissionAction.READ), RowScope.MANAGED_USERS, Map.of());

        assertThat(evaluator.decide(headNurse, policy, PermissionAction.READ,
                new PermissionRow("patient-1", null, "ward-a", Set.of("nurse-1"), Set.of())).allowed())
                .isTrue();
        assertThat(evaluator.decide(headNurse, policy, PermissionAction.READ,
                new PermissionRow("patient-2", null, "ward-a", Set.of("nurse-9"), Set.of())).allowed())
                .isFalse();
    }

    @Test
    void informationClerkIsReadOnlyWhileLeaderCanUpdate() {
        PermissionSubject clerk = PermissionSubject.of("info-1", "information");
        PermissionRow businessRow = PermissionRow.ownedBy("business-1", "owner-1", "information");
        PermissionPolicy readOnly = new PermissionPolicy(
                "business-data", Set.of(PermissionAction.READ), RowScope.ORGANIZATION, Map.of());
        PermissionPolicy leader = new PermissionPolicy(
                "business-data", Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                RowScope.ORGANIZATION, Map.of());

        assertThat(evaluator.decide(clerk, readOnly, PermissionAction.READ, businessRow).allowed()).isTrue();
        assertThat(evaluator.decide(clerk, readOnly, PermissionAction.UPDATE, businessRow).allowed()).isFalse();
        assertThat(evaluator.decide(clerk, leader, PermissionAction.UPDATE, businessRow).allowed()).isTrue();
    }

    @Test
    void customScopeFailsClosedUntilBackendPolicyCompilesIt() {
        PermissionSubject subject = PermissionSubject.of("user-1", "org-1");
        PermissionPolicy policy = new PermissionPolicy(
                "business-data", Set.of(PermissionAction.READ), RowScope.CUSTOM, Map.of());

        assertThat(evaluator.decide(subject, policy, PermissionAction.READ,
                PermissionRow.ownedBy("row-1", "user-1", "org-1")).allowed()).isFalse();
    }

    @Test
    void relatedUsersAreComparedWithTheCurrentSubject() {
        PermissionSubject nurse = PermissionSubject.of("nurse-1", "ward-a");
        PermissionPolicy policy = new PermissionPolicy(
                "patient", Set.of(PermissionAction.READ), RowScope.RELATED_RECORDS, Map.of());

        assertThat(evaluator.decide(nurse, policy, PermissionAction.READ,
                new PermissionRow("patient-1", null, "ward-a", Set.of(), Set.of("nurse-1"))).allowed())
                .isTrue();
    }
}
