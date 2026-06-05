package com.treble.feasimulation.model;

import java.util.Arrays;

public abstract class Element {
    private final int id;
    private final int[] nodeIds;
    private final int materialId;
    private final double area;

    public Element(int id, int[] nodeIds, int materialId, double area) {
        this.id = id;
        this.nodeIds = nodeIds.clone();
        this.materialId = materialId;
        this.area = area;
    }

    public int getId() {
        return id;
    }

    public int[] getNodeIds() {
        return nodeIds.clone();
    }

    public int getNodeStartId() {
        return nodeIds[0];
    }

    public int getNodeEndId() {
        return nodeIds[nodeIds.length - 1];
    }

    public int getMaterialId() {
        return materialId;
    }

    public double getArea() {
        return area;
    }

    /**
     * Computes the axial stiffness (EA/L) of the element.
     * Phase 2: This will be used to build the axial component of the stiffness matrix.
     * 
     * @param youngsModulus E
     * @param length L
     * @return EA/L
     */
    public double computeAxialStiffness(double youngsModulus, double length) {
        if (length <= 0) return 0;
        return (youngsModulus * area) / length;
    }

    /**
     * @return the DOFs active for each node of this element.
     */
    public abstract Node.DOF[] getActiveDOFs();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + id + ": nodes=" + Arrays.toString(nodeIds) + ", mat=" + materialId + "}";
    }
}
