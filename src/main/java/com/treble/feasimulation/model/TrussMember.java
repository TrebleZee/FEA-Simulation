package com.treble.feasimulation.model;

/**
 * Represents a member in a truss structure.
 * Truss members only support axial loads and connect two nodes.
 * Reuses the DOF system by specifying UX and UY as active degrees of freedom.
 */
public class TrussMember extends TrussElement {

    public TrussMember(int id, int nodeStartId, int nodeEndId, int materialId, double area) {
        super(id, nodeStartId, nodeEndId, materialId, area);
    }

    @Override
    public Node.DOF[] getActiveDOFs() {
        // Reuse the DOF system: Truss members only have translational DOFs
        return new Node.DOF[]{Node.DOF.UX, Node.DOF.UY};
    }

    @Override
    public String toString() {
        return "TrussMember{" + getId() + ": " + getNodeStartId() + "->" + getNodeEndId() + ", mat=" + getMaterialId() + ", A=" + getArea() + "}";
    }
}
