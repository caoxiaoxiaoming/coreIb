package com.coreib.kernel;

import java.util.Objects;

/** Stable metadata exposed by every product domain module. */
public record DomainModuleDescriptor(String code, String name, String version) {
    public DomainModuleDescriptor {
        code = requireText(code, "code");
        name = requireText(name, "name");
        version = requireText(version, "version");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
