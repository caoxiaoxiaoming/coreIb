package com.coreib.platform;

import com.coreib.security.ColumnAccess;
import com.coreib.security.PermissionAction;
import com.coreib.security.RowScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAdministrationContractTest {
    @Test
    void rejectsFieldGrantWithoutMatchingResourcePermission() {
        assertThatThrownBy(() -> new SecurityAdministration.RolePolicyDraft(
                List.of(new SecurityAdministration.PermissionGrantDraft(
                        "patient", PermissionAction.READ, RowScope.SELF, null, true)),
                List.of(new SecurityAdministration.FieldGrantDraft(
                        "encounter", "diagnosis", ColumnAccess.READ))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encounter");
    }

    @Test
    void rejectsInvalidEffectiveWindowAndSelfSupervision() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new SecurityAdministration.SupervisorRelationshipDraft(
                "user-1", "user-1", "DIRECT", true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityAdministration.RecordAssignmentDraft(
                "patient", "p-1", "user-1", "PRIMARY", true, now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveFrom");
    }
}
