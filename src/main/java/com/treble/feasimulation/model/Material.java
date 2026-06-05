package com.treble.feasimulation.model;

public class Material {
    private final int id;
    private final String name;
    private final double youngsModulus;
    private final double density;

    public Material(int id, String name, double youngsModulus, double density) {
        this.id = id;
        this.name = name;
        this.youngsModulus = youngsModulus;
        this.density = density;
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

    @Override
    public String toString() {
        return "Material{" + id + ": " + name + "}";
    }
}
