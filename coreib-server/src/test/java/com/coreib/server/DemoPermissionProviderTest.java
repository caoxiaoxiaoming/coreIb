package com.coreib.server;

import com.coreib.security.PermissionAction;
import com.coreib.server.api.DemoPermissionProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPermissionProviderTest {
    @Test
    void exposesNurseReadAndMaskedPatientFields() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DemoPermissionProvider.SUBJECT_HEADER, "nurse");

        var snapshot = new DemoPermissionProvider(request).current();

        assertThat(snapshot.authenticated()).isTrue();
        assertThat(snapshot.subjectId()).isEqualTo("nurse-001");
        assertThat(snapshot.roleCodes()).containsExactly("nurse");
        assertThat(snapshot.resources().get("patient").rowScope().name()).isEqualTo("RELATED_RECORDS");
        assertThat(snapshot.resources().get("patient").fields().get("idCard").name()).isEqualTo("MASKED");
        assertThat(new DemoPermissionProvider(request).subject().userId()).isEqualTo("nurse-001");
    }

    @Test
    void exposesReadOnlyClerkAndWritableLeader() {
        HttpServletRequest clerkRequest = requestFor("information-clerk");
        HttpServletRequest leaderRequest = requestFor("information-leader");

        var clerk = new DemoPermissionProvider(clerkRequest).current();
        var leader = new DemoPermissionProvider(leaderRequest).current();

        assertThat(clerk.resources().get("business-data").actions()).containsExactly(PermissionAction.READ);
        assertThat(leader.resources().get("business-data").actions())
                .containsExactlyInAnyOrder(PermissionAction.READ, PermissionAction.UPDATE);
    }

    private static MockHttpServletRequest requestFor(String subject) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DemoPermissionProvider.SUBJECT_HEADER, subject);
        return request;
    }
}
