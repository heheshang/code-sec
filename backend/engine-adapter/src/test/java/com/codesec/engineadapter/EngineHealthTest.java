package com.codesec.engineadapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EngineHealth}.
 */
class EngineHealthTest {

    @Test
    void defaultConstructor() {
        EngineHealth h = new EngineHealth();
        assertFalse(h.isOk());
        assertNull(h.getEngineVersion());
        assertEquals(0L, h.getScanCount());
    }

    @Test
    void parameterizedConstructor() {
        EngineHealth h = new EngineHealth(true, "2.0.0", 5);
        assertTrue(h.isOk());
        assertEquals("2.0.0", h.getEngineVersion());
        assertEquals(5L, h.getScanCount());
    }

    @Test
    void settersAndGetters() {
        EngineHealth h = new EngineHealth();
        h.setOk(true);
        h.setEngineVersion("1.5.0");
        h.setScanCount(3);
        assertTrue(h.isOk());
        assertEquals("1.5.0", h.getEngineVersion());
        assertEquals(3L, h.getScanCount());
    }
}
