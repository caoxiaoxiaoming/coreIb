package com.coreib.platform;

public record RoleDraft(String code, String displayName, String description) {
    public RoleDraft {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
    }
}
