package com.coreib.platform;

public interface PlatformDirectoryCommands {
    OrganizationSummary createOrganization(OrganizationDraft draft);
    UserSummary createUser(UserDraft draft);
    RoleSummary createRole(RoleDraft draft);
    default OrganizationSummary updateOrganization(String id, OrganizationDraft draft) {
        throw new UnsupportedOperationException("Organization update is not configured");
    }
    default UserSummary updateUser(String id, UserDraft draft) {
        throw new UnsupportedOperationException("User update is not configured");
    }
    default RoleSummary updateRole(String code, RoleDraft draft) {
        throw new UnsupportedOperationException("Role update is not configured");
    }
    default void delete(DirectoryEntity entity, String id) {
        throw new UnsupportedOperationException("Directory deletion is not configured");
    }
    void setEnabled(DirectoryEntity entity, String id, boolean enabled);
}
