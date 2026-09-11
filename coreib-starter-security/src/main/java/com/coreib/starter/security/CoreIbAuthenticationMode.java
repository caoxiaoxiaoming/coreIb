package com.coreib.starter.security;

/** Supported authentication integrations for the servlet application. */
public enum CoreIbAuthenticationMode {
    /** Local development mode. Authorization providers still fail closed. */
    DISABLED,
    /** Stateless OAuth 2.0 resource server using bearer JWTs. */
    JWT,
    /** Browser login using an OpenID Connect authorization-code session. */
    OIDC
}
