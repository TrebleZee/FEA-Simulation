package com.treble.feasimulation.model;

/**
 * Simple 2D beam element representation connecting two node IDs with a material reference
 * and basic cross-section properties.
 */
public class BeamElement extends Element {
    private final double area;       // cross-sectional area
    private final double inertia;    // second moment of area (I)

    public BeamElement(int id, int nodeStartId, int nodeEndId, int materialId, double area, double inertia) {
        super(id, new int[]{nodeStartId, nodeEndId}, materialId);
        this.area = area;
        this.inertia = inertia;
    }

    public int getNodeStartId() {
        return getNodeIds()[0];
    }

    public int getNodeEndId() {
        return getNodeIds()[1];
    }

    public double getArea() {
        return area;
    }

    public double getInertia() {
        return inertia;
    }

    @Override
    public Node.DOF[] getActiveDOFs() {
        return new Node.DOF[]{Node.DOF.UX, Node.DOF.UY, Node.DOF.ROTATION};
    }

    @Override
    public String toString() {
        return "BeamElement{" + getId() + ": " + getNodeStartId() + "->" + getNodeEndId() + ", mat=" + getMaterialId() + ", A=" + area + ", I=" + inertia + "}";
    }
}
