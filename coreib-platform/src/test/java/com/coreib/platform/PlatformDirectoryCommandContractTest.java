package com.coreib.platform;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformDirectoryCommandContractTest {
    @Test void rejectsInvalidDrafts() {
        assertThatThrownBy(() -> new OrganizationDraft("id", null, "code", "name", "DEPARTMENT", -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new UserDraft("id", "login", "name", null, null, null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new RoleDraft("", "role", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
