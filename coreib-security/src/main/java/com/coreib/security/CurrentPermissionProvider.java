package com.coreib.security;

/** Authentication integrations provide the current subject and effective permissions here. */
@FunctionalInterface
public interface CurrentPermissionProvider {
    PermissionSnapshot current();

    default PermissionSubject subject() {
        return PermissionSubject.of(current().subjectId(), null);
    }
}
