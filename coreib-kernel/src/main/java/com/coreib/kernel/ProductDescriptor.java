package com.coreib.kernel;

import java.util.List;

/** Product identity and its installed module catalog. */
public record ProductDescriptor(
        String code,
        String name,
        String version,
        String description,
        List<DomainModuleDescriptor> modules) {

    public ProductDescriptor {
        code = requireText(code, "code");
        name = requireText(name, "name");
        version = requireText(version, "version");
        description = description == null ? "" : description.trim();
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
