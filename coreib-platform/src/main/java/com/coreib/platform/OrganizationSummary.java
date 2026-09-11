package com.coreib.platform;

/** Database-neutral organization projection used by platform administration. */
public record OrganizationSummary(
        String id,
        String parentId,
        String code,
        String displayName,
        String type,
        boolean enabled) {
    public OrganizationSummary {
        requireText("id", id);
        requireText("code", code);
        requireText("displayName", displayName);
        requireText("type", type);
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
