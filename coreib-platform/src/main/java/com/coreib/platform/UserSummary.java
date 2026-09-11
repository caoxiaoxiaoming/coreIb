package com.coreib.platform;

import java.util.Set;

/** Frontend-safe user projection; credentials and identity tokens never leave the auth adapter. */
public record UserSummary(
        String id,
        String loginName,
        String displayName,
        String organizationId,
        String organizationName,
        boolean enabled,
        Set<String> roleCodes) {
    public UserSummary {
        requireText("id", id);
        requireText("loginName", loginName);
        requireText("displayName", displayName);
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
