package com.coreib.server.api;

/** Platform user column matched against Servlet Principal.getName(). */
public enum JdbcPrincipalLookup {
    ID,
    LOGIN_NAME,
    EXTERNAL_IDENTITY
}
