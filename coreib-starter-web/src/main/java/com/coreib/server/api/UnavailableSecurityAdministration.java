package com.coreib.server.api;

import com.coreib.platform.SecurityAdministration;

import java.util.List;

final class UnavailableSecurityAdministration implements SecurityAdministration {
    @Override
    public SecurityConfiguration configuration() {
        return new SecurityConfiguration(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Override
    public void replaceRolePolicy(String roleCode, RolePolicyDraft draft) {
        throw unavailable();
    }

    @Override
    public void replaceUserRoles(String userId, UserRolesDraft draft) {
        throw unavailable();
    }

    @Override
    public SupervisorRelationship saveSupervisorRelationship(SupervisorRelationshipDraft draft) {
        throw unavailable();
    }

    @Override
    public void setSupervisorRelationshipActive(String id, boolean active) {
        throw unavailable();
    }

    @Override
    public RecordAssignment saveRecordAssignment(RecordAssignmentDraft draft) {
        throw unavailable();
    }

    @Override
    public void setRecordAssignmentActive(String id, boolean active) {
        throw unavailable();
    }

    private PlatformCommandUnavailableException unavailable() {
        return new PlatformCommandUnavailableException("Security administration requires an enabled coreIb datasource");
    }
}
