package com.coreib.platform;

public record OrganizationDraft(String id, String parentId, String code, String displayName, String type, int sortOrder) {
    public OrganizationDraft {
        requireText("id", id); requireText("code", code); requireText("displayName", displayName); requireText("type", type);
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must not be negative");
    }
    private static void requireText(String name, String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank"); }
}
