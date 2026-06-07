package com.treble.feasimulation.model;

public class Material {
    private final int id;
    private final String name;
    private final double youngsModulus;
    private final double density;
    private final double yieldStress;
    private final double poissonRatio;
    private final double thickness;

    public Material(int id, String name, double youngsModulus, double density) {
        this(id, name, youngsModulus, density, Double.NaN, Double.NaN, Double.NaN);
    }

    public Material(int id, String name, double youngsModulus, double density, double yieldStress) {
        this(id, name, youngsModulus, density, yieldStress, Double.NaN, Double.NaN);
    }

    public Material(int id, String name, double youngsModulus, double density, double yieldStress, double poissonRatio, double thickness) {
        this.id = id;
        this.name = name;
        this.youngsModulus = youngsModulus;
        this.density = density;
        this.yieldStress = yieldStress;
        this.poissonRatio = poissonRatio;
        this.thickness = thickness;
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

    public double getPoissonRatio() {
        return poissonRatio;
    }

    public double getThickness() {
        return thickness;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Material{%d: %s, E=%.3e, density=%.3f", id, name, youngsModulus, density));
        if (Double.isFinite(yieldStress) && yieldStress > 0.0) {
            sb.append(String.format(", yield=%.3e", yieldStress));
        }
        if (Double.isFinite(poissonRatio)) {
            sb.append(String.format(", v=%.2f", poissonRatio));
        }
        if (Double.isFinite(thickness)) {
            sb.append(String.format(", t=%.2f", thickness));
        }
        sb.append("}");
        return sb.toString();
    }
}
