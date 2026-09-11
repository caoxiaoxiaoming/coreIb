package com.coreib.server;

import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.OrganizationDraft;
import com.coreib.platform.AuditEvent;
import com.coreib.platform.DirectoryEntity;
import com.coreib.platform.PlatformDirectory;
import com.coreib.platform.PlatformDirectoryCommands;
import com.coreib.platform.RoleDraft;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserDraft;
import com.coreib.platform.UserSummary;
import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionAction;
import com.coreib.security.PermissionDeniedException;
import com.coreib.security.PermissionSnapshot;
import com.coreib.security.ResourcePermission;
import com.coreib.security.RowScope;
import com.coreib.server.api.PlatformDirectoryController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformDirectoryControllerTest {
    private final PlatformDirectory directory = new PlatformDirectory() {
        @Override
        public List<OrganizationSummary> organizations() {
            return List.of(new OrganizationSummary("root", null, "ROOT", "总部", "ROOT", true));
        }

        @Override
        public List<UserSummary> users() {
            return List.of();
        }

        @Override
        public List<RoleSummary> roles() {
            return List.of();
        }
    };

    @Test
    void returnsDirectoryWhenReadIsGranted() {
        CurrentPermissionProvider provider = () -> new PermissionSnapshot(
                true, "admin", Set.of("platform-admin"),
                Map.of(PlatformDirectoryController.RESOURCE,
                        new ResourcePermission(Set.of(PermissionAction.READ), RowScope.ALL, Map.of())));

        assertThat(new PlatformDirectoryController(provider, directory).organizations())
                .extracting(OrganizationSummary::code).containsExactly("ROOT");
    }

    @Test
    void deniesDirectoryWithoutExplicitGrant() {
        PlatformDirectoryController controller = new PlatformDirectoryController(
                PermissionSnapshot::anonymous, directory);

        assertThatThrownBy(controller::users).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void auditsSuccessfulPlatformCommand() {
        List<AuditEvent> events = new ArrayList<>();
        PlatformDirectoryController controller = new PlatformDirectoryController(
                updateProvider(), directory, new StubCommands(), events::add);

        OrganizationSummary organization = controller.createOrganization(
                new OrganizationDraft("ward-a", "root", "WARD-A", "Ward A", "WARD", 10));

        assertThat(organization.id()).isEqualTo("ward-a");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actorId()).isEqualTo("admin");
            assertThat(event.action()).isEqualTo("CREATE");
            assertThat(event.outcome()).isEqualTo("SUCCESS");
        });
    }

    @Test
    void auditsFailedCommandAndPreservesOriginalExceptionWhenAuditAlsoFails() {
        IllegalStateException commandFailure = new IllegalStateException("command failed");
        List<AuditEvent> events = new ArrayList<>();
        PlatformDirectoryCommands commands = new StubCommands() {
            @Override
            public OrganizationSummary createOrganization(OrganizationDraft draft) {
                throw commandFailure;
            }
        };
        PlatformDirectoryController auditedController = new PlatformDirectoryController(
                updateProvider(), directory, commands, events::add);

        assertThatThrownBy(() -> auditedController.createOrganization(
                new OrganizationDraft("ward-a", "root", "WARD-A", "Ward A", "WARD", 10)))
                .isSameAs(commandFailure);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.outcome()).isEqualTo("FAILED");
            assertThat(event.details()).isEqualTo("IllegalStateException");
        });

        PlatformDirectoryController auditFailingController = new PlatformDirectoryController(
                updateProvider(), directory, commands, event -> {
                    throw new IllegalArgumentException("audit failed");
                });
        assertThatThrownBy(() -> auditFailingController.createOrganization(
                new OrganizationDraft("ward-b", "root", "WARD-B", "Ward B", "WARD", 20)))
                .isSameAs(commandFailure);
    }

    private CurrentPermissionProvider updateProvider() {
        return () -> new PermissionSnapshot(
                true, "admin", Set.of("platform-admin"),
                Map.of(PlatformDirectoryController.RESOURCE,
                        new ResourcePermission(Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                                RowScope.ALL, Map.of())));
    }

    private static class StubCommands implements PlatformDirectoryCommands {
        @Override
        public OrganizationSummary createOrganization(OrganizationDraft draft) {
            return new OrganizationSummary(draft.id(), draft.parentId(), draft.code(),
                    draft.displayName(), draft.type(), true);
        }

        @Override
        public UserSummary createUser(UserDraft draft) {
            return new UserSummary(draft.id(), draft.loginName(), draft.displayName(),
                    draft.organizationId(), null, true, draft.roleCodes());
        }

        @Override
        public RoleSummary createRole(RoleDraft draft) {
            return new RoleSummary(draft.code(), draft.displayName(), draft.description(), true);
        }

        @Override
        public void setEnabled(DirectoryEntity entity, String id, boolean enabled) {
        }
    }
}
