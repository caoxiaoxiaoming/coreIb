package com.coreib.platform;

import java.util.List;

/** Port implemented by JDBC, remote IAM or another platform directory adapter. */
public interface PlatformDirectory {
    List<OrganizationSummary> organizations();

    List<UserSummary> users();

    List<RoleSummary> roles();
}
