package com.coreib.platform;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformDirectoryContractTest {
    @Test
    void copiesRoleCodesAndRejectsInvalidOrganizations() {
        Set<String> roles = new java.util.HashSet<>(Set.of("platform-admin"));
        UserSummary user = new UserSummary("user-1", "admin", "平台管理员", "root", "总部", true, roles);
        roles.add("changed-later");

        assertThat(user.roleCodes()).containsExactly("platform-admin");
        assertThatThrownBy(() -> new OrganizationSummary("", null, "ROOT", "总部", "ROOT", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
