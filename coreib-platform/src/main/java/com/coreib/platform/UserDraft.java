package com.coreib.platform;

import java.util.Set;

public record UserDraft(String id, String loginName, String displayName, String organizationId, String externalIdentity, Set<String> roleCodes) {
    public UserDraft {
        requireText("id", id); requireText("loginName", loginName); requireText("displayName", displayName);
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
    private static void requireText(String name, String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank"); }
}
