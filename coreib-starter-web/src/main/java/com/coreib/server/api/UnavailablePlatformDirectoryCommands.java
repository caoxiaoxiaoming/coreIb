package com.coreib.server.api;

import com.coreib.platform.*;

final class UnavailablePlatformDirectoryCommands implements PlatformDirectoryCommands {
    @Override public OrganizationSummary createOrganization(OrganizationDraft draft) { throw unavailable(); }
    @Override public UserSummary createUser(UserDraft draft) { throw unavailable(); }
    @Override public RoleSummary createRole(RoleDraft draft) { throw unavailable(); }
    @Override public void setEnabled(DirectoryEntity entity, String id, boolean enabled) { throw unavailable(); }
    private PlatformCommandUnavailableException unavailable() { return new PlatformCommandUnavailableException(); }
}
