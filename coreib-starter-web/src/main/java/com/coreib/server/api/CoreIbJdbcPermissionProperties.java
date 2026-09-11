package com.coreib.server.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("coreib.security.jdbc")
public class CoreIbJdbcPermissionProperties {
    private boolean enabled;
    private JdbcPrincipalLookup principalLookup = JdbcPrincipalLookup.ID;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JdbcPrincipalLookup getPrincipalLookup() {
        return principalLookup;
    }

    public void setPrincipalLookup(JdbcPrincipalLookup principalLookup) {
        this.principalLookup = principalLookup;
    }
}
