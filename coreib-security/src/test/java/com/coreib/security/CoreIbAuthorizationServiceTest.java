package com.coreib.security;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreIbAuthorizationServiceTest {
    private static final PermissionSubject SUBJECT = new PermissionSubject(
            "head-1",
            "ward-a",
            Set.of("ward-a", "room-a"),
            Set.of("nurse-1", "nurse-2"),
            Set.of("patient-assigned"));

    @Test
    void combinesIndependentRoleScopesWithOrSemantics() {
        CoreIbAuthorizationService service = service(true, List.of(
                policy(PermissionAction.READ, RowScope.ORGANIZATION, Map.of()),
                policy(PermissionAction.READ, RowScope.MANAGED_USERS, Map.of())));

        RowAccessCriteria criteria = service.rowCriteria("patient", PermissionAction.READ);

        assertThat(criteria.scopes())
                .containsExactlyInAnyOrder(RowScope.ORGANIZATION, RowScope.MANAGED_USERS);
        assertThat(criteria.matches(PermissionRow.ownedBy("p-org", "other", "ward-a"))).isTrue();
        assertThat(criteria.matches(PermissionRow.ownedBy("p-managed", "nurse-1", "ward-b"))).isTrue();
        assertThat(criteria.matches(PermissionRow.ownedBy("p-denied", "other", "ward-b"))).isFalse();
    }

    @Test
    void keepsRowScopesSpecificToEachAction() {
        CoreIbAuthorizationService service = service(true, List.of(
                policy(PermissionAction.READ, RowScope.ORGANIZATION, Map.of()),
                policy(PermissionAction.UPDATE, RowScope.SELF, Map.of())));
        PermissionRow colleague = PermissionRow.ownedBy("p-1", "nurse-1", "ward-a");
        PermissionRow own = PermissionRow.ownedBy("p-2", "head-1", "ward-b");

        assertThat(service.decide("patient", PermissionAction.READ, colleague).allowed()).isTrue();
        assertThat(service.decide("patient", PermissionAction.READ).allowed()).isTrue();
        assertThat(service.decide("patient", PermissionAction.DELETE).allowed()).isFalse();
        assertThat(service.decide("patient", PermissionAction.UPDATE, colleague).allowed()).isFalse();
        assertThat(service.decide("patient", PermissionAction.UPDATE, own).allowed()).isTrue();
        assertThat(service.rowCriteria("patient", PermissionAction.UPDATE).scopes())
                .containsExactly(RowScope.SELF);
    }

    @Test
    void mergesFieldsOnlyAcrossPoliciesGrantingTheAction() {
        CoreIbAuthorizationService service = service(true, List.of(
                policy(PermissionAction.READ, RowScope.ORGANIZATION,
                        Map.of("idCard", ColumnAccess.MASKED)),
                policy(PermissionAction.READ, RowScope.MANAGED_USERS,
                        Map.of("idCard", ColumnAccess.READ)),
                policy(PermissionAction.UPDATE, RowScope.SELF,
                        Map.of("idCard", ColumnAccess.WRITE))));

        assertThat(service.fieldAccess("patient", PermissionAction.READ, "idCard"))
                .isEqualTo(ColumnAccess.READ);
        assertThat(service.fieldAccess("patient", PermissionAction.UPDATE, "idCard"))
                .isEqualTo(ColumnAccess.WRITE);
        assertThat(service.fieldAccess("patient", "idCard")).isEqualTo(ColumnAccess.WRITE);
        assertThat(service.fieldAccess("patient", PermissionAction.READ, "diagnosis"))
                .isEqualTo(ColumnAccess.HIDDEN);
    }

    @Test
    void unresolvedCustomScopesFailClosedAndRemainExplainable() {
        CoreIbAuthorizationService service = service(true, List.of(new PermissionPolicy(
                "patient",
                Set.of(PermissionAction.READ),
                RowScope.CUSTOM,
                Map.of(),
                "ward-duty-policy")));

        RowAccessCriteria criteria = service.rowCriteria("patient", PermissionAction.READ);

        assertThat(criteria.denied()).isTrue();
        assertThat(criteria.hasUnresolvedCustomPolicies()).isTrue();
        assertThat(criteria.unresolvedCustomPolicyKeys()).containsExactly("ward-duty-policy");
        assertThat(service.decide("patient", PermissionAction.READ,
                PermissionRow.ownedBy("p-1", "head-1", "ward-a")).allowed()).isFalse();
    }

    @Test
    void anonymousSubjectsFailClosedEvenIfAProviderReturnsPolicies() {
        CoreIbAuthorizationService service = service(false, List.of(
                policy(PermissionAction.READ, RowScope.ALL, Map.of("name", ColumnAccess.READ))));

        assertThat(service.rowCriteria("patient", PermissionAction.READ).denied()).isTrue();
        assertThat(service.fieldAccess("patient", PermissionAction.READ, "name"))
                .isEqualTo(ColumnAccess.HIDDEN);
        assertThatThrownBy(() -> service.require(
                "patient",
                PermissionAction.READ,
                PermissionRow.ownedBy("p-1", "head-1", "ward-a")))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("anonymous-subject");
    }

    private static PermissionPolicy policy(
            PermissionAction action,
            RowScope scope,
            Map<String, ColumnAccess> fields) {
        return new PermissionPolicy("patient", Set.of(action), scope, fields);
    }

    private static CoreIbAuthorizationService service(
            boolean authenticated,
            Collection<PermissionPolicy> policies) {
        PermissionSnapshot snapshot = authenticated
                ? new PermissionSnapshot(true, SUBJECT.userId(), Set.of("test-role"), Map.of())
                : PermissionSnapshot.anonymous();
        return new CoreIbAuthorizationService(new CurrentAuthorizationProvider() {
            @Override
            public PermissionSnapshot current() {
                return snapshot;
            }

            @Override
            public PermissionSubject subject() {
                return authenticated ? SUBJECT : PermissionSubject.of("anonymous", null);
            }

            @Override
            public Collection<PermissionPolicy> policies(String resource) {
                return policies;
            }
        });
    }
}
