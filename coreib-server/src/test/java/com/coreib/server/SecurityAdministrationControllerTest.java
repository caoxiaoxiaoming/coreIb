package com.coreib.server;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.SecurityAdministration;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.PermissionSnapshot;
import com.coreib.security.ResourcePermission;
import com.coreib.security.RowScope;
import com.coreib.server.api.SecurityAdministrationController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAdministrationControllerTest {
    @Test
    void requiresDedicatedReadPermission() {
        SecurityAdministrationController controller = new SecurityAdministrationController(
                PermissionSnapshot::anonymous, new StubAdministration(), event -> { });

        assertThatThrownBy(controller::configuration).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void replacesPolicyAndAuditsSuccessfulCommand() {
        StubAdministration administration = new StubAdministration();
        List<AuditEvent> events = new ArrayList<>();
        SecurityAdministrationController controller = new SecurityAdministrationController(
                updateProvider(), administration, events::add);
        SecurityAdministration.RolePolicyDraft draft = new SecurityAdministration.RolePolicyDraft(
                List.of(new SecurityAdministration.PermissionGrantDraft(
                        "patient", PermissionAction.READ, RowScope.SELF, null, true)), List.of());

        controller.replaceRolePolicy("nurse", draft);

        assertThat(administration.replacedRole).isEqualTo("nurse");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actorId()).isEqualTo("admin");
            assertThat(event.resource()).isEqualTo("role-policy");
            assertThat(event.outcome()).isEqualTo("SUCCESS");
        });
    }

    private CurrentPermissionProvider updateProvider() {
        return () -> new PermissionSnapshot(true, "admin", Set.of("platform-admin"),
                Map.of(SecurityAdministrationController.RESOURCE,
                        new ResourcePermission(Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                                RowScope.ALL, Map.of())));
    }

    private static final class StubAdministration implements SecurityAdministration {
        private String replacedRole;

        @Override
        public SecurityConfiguration configuration() {
            return new SecurityConfiguration(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        @Override
        public void replaceRolePolicy(String roleCode, RolePolicyDraft draft) {
            replacedRole = roleCode;
        }

        @Override
        public void replaceUserRoles(String userId, UserRolesDraft draft) {
        }

        @Override
        public SupervisorRelationship saveSupervisorRelationship(SupervisorRelationshipDraft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSupervisorRelationshipActive(String id, boolean active) {
        }

        @Override
        public RecordAssignment saveRecordAssignment(RecordAssignmentDraft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRecordAssignmentActive(String id, boolean active) {
        }
    }
}
