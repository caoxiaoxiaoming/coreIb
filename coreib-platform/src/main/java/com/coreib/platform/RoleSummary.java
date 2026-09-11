package com.coreib.platform;

/** Role metadata exposed by the platform directory API. */
public record RoleSummary(String code, String displayName, String description, boolean enabled) {
    public RoleSummary {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
