package com.treble.feasimulation.model;

/**
 * Represents a node in a truss structure.
 * Truss nodes primarily support translational degrees of freedom (UX, UY).
 */
public class TrussNode extends Node {

    public TrussNode(int id, double x, double y) {
        super(id, x, y);
    }

    @Override
    public DOF[] getSupportedDOFs() {
        return new DOF[]{DOF.UX, DOF.UY};
    }

    @Override
    public Node copyAt(double x, double y) {
        return new TrussNode(getId(), x, y);
    }

    @Override
    public String toString() {
        return "TrussNode{" + getId() + ": (" + getX() + "," + getY() + ")}";
    }
}
