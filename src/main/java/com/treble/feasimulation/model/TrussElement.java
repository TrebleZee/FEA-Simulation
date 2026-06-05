package com.treble.feasimulation.model;

import java.util.Arrays;

/**
 * 2D Truss element representation connecting two node IDs.
 * Trusses only support axial forces (UX, UY) and do not support rotation.
 */
public class TrussElement extends Element {

    public TrussElement(int id, int nodeStartId, int nodeEndId, int materialId, double area) {
        super(id, new int[]{nodeStartId, nodeEndId}, materialId, area);
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

    @Override
    public Node.DOF[] getActiveDOFs() {
        return new Node.DOF[]{Node.DOF.UX, Node.DOF.UY};
    }

    @Override
    public String toString() {
        return "TrussElement{" + getId() + ": " + getNodeStartId() + "->" + getNodeEndId() + ", mat=" + getMaterialId() + ", A=" + getArea() + "}";
    }
}
