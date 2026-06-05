package com.treble.feasimulation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MaterialLibraryTest {

    @Test
    public void presetsExposeExpectedMaterials() {
        assertEquals(3, MaterialLibrary.getPresets().size());

        Material steel = MaterialLibrary.findByName("Steel").orElseThrow();
        Material aluminium = MaterialLibrary.findByName("Aluminium").orElseThrow();
        Material wood = MaterialLibrary.findByName("Wood").orElseThrow();

        assertAll(
                () -> assertEquals(1, steel.getId()),
                () -> assertEquals(2.0e11, steel.getYoungsModulus(), 1e6),
                () -> assertEquals(7850.0, steel.getDensity(), 1e-9),
                () -> assertEquals(2.5e8, steel.getYieldStress(), 1e5),
                () -> assertEquals(2, aluminium.getId()),
                () -> assertEquals(6.9e10, aluminium.getYoungsModulus(), 1e6),
                () -> assertEquals(2700.0, aluminium.getDensity(), 1e-9),
                () -> assertEquals(1.5e8, aluminium.getYieldStress(), 1e5),
                () -> assertEquals(3, wood.getId()),
                () -> assertEquals(1.0e10, wood.getYoungsModulus(), 1e6),
                () -> assertEquals(600.0, wood.getDensity(), 1e-9),
                () -> assertEquals(4.0e7, wood.getYieldStress(), 1e4),
                () -> assertSame(steel, MaterialLibrary.getDefaultMaterial())
        );
    }
}
