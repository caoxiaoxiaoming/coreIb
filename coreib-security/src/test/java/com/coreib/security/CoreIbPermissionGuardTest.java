package com.coreib.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class CoreIbPermissionGuardTest {
    @Test
    void rejectsAnUpdateWhenOnlyReadIsGranted() {
        CoreIbPermissionGuard guard = new CoreIbPermissionGuard();
        PermissionSubject subject = PermissionSubject.of("info-1", "information");
        PermissionPolicy readOnly = PermissionPolicyPresets.informationClerkBusinessData();

        assertThatThrownBy(() -> guard.require(
                subject,
                readOnly,
                PermissionAction.UPDATE,
                PermissionRow.ownedBy("business-1", "owner-1", "information")))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("business-data / UPDATE");
    }

    @Test
    void combinesRoleGrantsForDifferentManagedResources() {
        CoreIbPermissionPolicies policies = new CoreIbPermissionPolicies();
        PermissionSubject headNurse = new PermissionSubject(
                "head-1", "ward-a", java.util.Set.of("ward-a"), java.util.Set.of("nurse-1"), java.util.Set.of());

        assertThat(policies.decide(
                headNurse,
                List.of(PermissionPolicyPresets.headNursePatients()),
                PermissionAction.READ,
                new PermissionRow("patient-1", null, "ward-a", java.util.Set.of("nurse-1"), java.util.Set.of()))
                .allowed()).isTrue();
    }
}
