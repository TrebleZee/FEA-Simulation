package com.treble.feasimulation.model;

/**
 * Simple 2D beam element representation connecting two node IDs with a material reference
 * and basic cross-section properties.
 */
public class BeamElement {
    private final int id;
    private final int nodeStartId;
    private final int nodeEndId;
    private final int materialId;
    private final double area;       // cross-sectional area
    private final double inertia;    // second moment of area (I)

    public BeamElement(int id, int nodeStartId, int nodeEndId, int materialId, double area, double inertia) {
        this.id = id;
        this.nodeStartId = nodeStartId;
        this.nodeEndId = nodeEndId;
        this.materialId = materialId;
        this.area = area;
        this.inertia = inertia;
    }

    public int getId() {
        return id;
    }

    public int getNodeStartId() {
        return nodeStartId;
    }

    public int getNodeEndId() {
        return nodeEndId;
    }

    public int getMaterialId() {
        return materialId;
    }

    public double getArea() {
        return area;
    }

    public double getInertia() {
        return inertia;
    }

    @Override
    public String toString() {
        return "BeamElement{" + id + ": " + nodeStartId + "->" + nodeEndId + ", mat=" + materialId + ", A=" + area + ", I=" + inertia + "}";
    }
}
