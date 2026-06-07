package com.treble.feasimulation.model;

/**
 * Simple 2D beam element representation connecting two node IDs with a material reference
 * and basic cross-section properties.
 */
public class BeamElement extends StructuralElement {
    private final double inertia;    // second moment of area (I)

    public BeamElement(int id, int nodeStartId, int nodeEndId, int materialId, double area, double inertia) {
        super(id, new int[]{nodeStartId, nodeEndId}, materialId, area);
        this.inertia = inertia;
    }

    public int getNodeStartId() {
        return getNodeIds()[0];
    }

    public int getNodeEndId() {
        return getNodeIds()[1];
    }

    @Override
    public double getArea() {
        return super.getArea();
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
        return "BeamElement{" + getId() + ": " + getNodeStartId() + "->" + getNodeEndId() + ", mat=" + getMaterialId() + ", A=" + getArea() + ", I=" + inertia + "}";
    }
}
