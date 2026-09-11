package com.coreib.security;

import java.util.Collection;

/**
 * Backend authorization source for the current request.
 *
 * <p>The UI snapshot is only a presentation projection. Business services use the original
 * policies so independent role grants and action-specific row scopes are not collapsed.</p>
 */
public interface CurrentAuthorizationProvider extends CurrentPermissionProvider {
    Collection<PermissionPolicy> policies(String resource);

    /** Returns subject relationships scoped to a resource, such as record assignments. */
    default PermissionSubject subject(String resource) {
        return subject();
    }
}
