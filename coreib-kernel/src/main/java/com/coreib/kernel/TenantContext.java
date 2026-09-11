package com.coreib.kernel;

import java.util.Optional;

/** Request-scoped tenant lookup; products may use a fixed tenant for single-tenant deployments. */
@FunctionalInterface
public interface TenantContext {
    Optional<String> currentTenantId();

    static TenantContext singleTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        String normalized = tenantId.trim();
        return () -> Optional.of(normalized);
    }
}
