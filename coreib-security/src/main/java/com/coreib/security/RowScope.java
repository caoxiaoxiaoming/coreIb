package com.coreib.security;

/** Portable row scopes. CUSTOM must be compiled by a trusted backend policy provider. */
public enum RowScope {
    ALL,
    ORGANIZATION,
    ORGANIZATION_TREE,
    SELF,
    MANAGED_USERS,
    RELATED_RECORDS,
    CUSTOM
}
