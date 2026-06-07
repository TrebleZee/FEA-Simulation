package com.treble.feasimulation.model;

/**
 * Unit/scale settings for converting user/model geometry to SI units.
 *
 * Design:
 * - metersPerUnit: how many meters correspond to one model unit (drawing unit).
 *   Example: if you draw in millimeters, metersPerUnit = 0.001.
 * - newtonsPerUnit: reserved for future use if we allow non-SI force entry. For now defaults to 1.0 (N).
 */
public class UnitSettings {
    private double metersPerUnit;
    private double newtonsPerUnit;

    public UnitSettings() {
        this(1.0, 1.0);
    }

    public UnitSettings(double metersPerUnit, double newtonsPerUnit) {
        if (!(metersPerUnit > 0)) throw new IllegalArgumentException("metersPerUnit must be > 0");
        if (!(newtonsPerUnit > 0)) throw new IllegalArgumentException("newtonsPerUnit must be > 0");
        this.metersPerUnit = metersPerUnit;
        this.newtonsPerUnit = newtonsPerUnit;
    }

    public double getMetersPerUnit() {
        return metersPerUnit;
    }

    public void setMetersPerUnit(double metersPerUnit) {
        if (!(metersPerUnit > 0)) throw new IllegalArgumentException("metersPerUnit must be > 0");
        this.metersPerUnit = metersPerUnit;
    }

    public double getNewtonsPerUnit() {
        return newtonsPerUnit;
    }

    public void setNewtonsPerUnit(double newtonsPerUnit) {
        if (!(newtonsPerUnit > 0)) throw new IllegalArgumentException("newtonsPerUnit must be > 0");
        this.newtonsPerUnit = newtonsPerUnit;
    }
}
