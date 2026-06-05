package com.treble.feasimulation.model;

public class Material {
    private final int id;
    private final String name;
    private final double youngsModulus;
    private final double density;
    private final double yieldStress;

    public Material(int id, String name, double youngsModulus, double density) {
        this(id, name, youngsModulus, density, Double.NaN);
    }

    public Material(int id, String name, double youngsModulus, double density, double yieldStress) {
        this.id = id;
        this.name = name;
        this.youngsModulus = youngsModulus;
        this.density = density;
        this.yieldStress = yieldStress;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getYoungsModulus() {
        return youngsModulus;
    }

    public double getDensity() {
        return density;
    }

    public double getYieldStress() {
        return yieldStress;
    }

    @Override
    public String toString() {
        if (Double.isFinite(yieldStress) && yieldStress > 0.0) {
            return String.format("Material{%d: %s, E=%.3e, density=%.3f, yield=%.3e}",
                    id, name, youngsModulus, density, yieldStress);
        }
        return String.format("Material{%d: %s, E=%.3e, density=%.3f}",
                id, name, youngsModulus, density);
    }
}
