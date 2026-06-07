package com.treble.feasimulation.model;

import java.util.List;
import java.util.Optional;

/**
 * Built-in material presets for the beam simulator.
 */
public final class MaterialLibrary {
    private static final Material STEEL = new Material(1, "Steel", 2.0e11, 7850.0, 2.5e8, 0.3, 0.01);
    private static final Material ALUMINIUM = new Material(2, "Aluminium", 6.9e10, 2700.0, 1.5e8, 0.33, 0.01);
    private static final Material WOOD = new Material(3, "Wood", 1.0e10, 600.0, 4.0e7, 0.3, 0.01);

    private static final List<Material> PRESETS = List.of(STEEL, ALUMINIUM, WOOD);

    private MaterialLibrary() {
    }

    public static List<Material> getPresets() {
        return PRESETS;
    }

    public static Material getDefaultMaterial() {
        return STEEL;
    }

    public static Optional<Material> findById(int id) {
        return PRESETS.stream().filter(material -> material.getId() == id).findFirst();
    }

    public static Optional<Material> findByName(String name) {
        return PRESETS.stream().filter(material -> material.getName().equalsIgnoreCase(name)).findFirst();
    }
}
