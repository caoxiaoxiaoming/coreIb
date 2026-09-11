package com.github.caoxiaoxiaoming.coreib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreIbTest {
    @Test
    void exposesReleaseVersion() {
        assertEquals("1.0.1", CoreIb.version());
    }
}
