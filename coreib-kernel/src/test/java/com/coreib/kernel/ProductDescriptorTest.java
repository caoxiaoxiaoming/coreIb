package com.coreib.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductDescriptorTest {
    @Test
    void copiesModuleCatalogAndNormalizesIdentity() {
        List<DomainModuleDescriptor> modules = new ArrayList<>();
        modules.add(new DomainModuleDescriptor("core", "Core", "1.0.0"));

        ProductDescriptor descriptor = new ProductDescriptor(
                " datlas ", " Datlas ", "1.0.0", null, modules);
        modules.clear();

        assertEquals("datlas", descriptor.code());
        assertEquals(1, descriptor.modules().size());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.modules().clear());
    }

    @Test
    void rejectsBlankModuleCodes() {
        assertThrows(IllegalArgumentException.class,
                () -> new DomainModuleDescriptor(" ", "Core", "1.0.0"));
    }
}
