package com.treble.feasimulation.model;

public class Node {
    public enum DOF { UX, UY, ROTATION }

    private final int id;
    private final double x;
    private final double y;

    public Node(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * @return the types of degrees of freedom supported by this node in 2D space.
     * By default, 2D nodes support UX, UY, and ROTATION.
     */
    public DOF[] getSupportedDOFs() {
        return DOF.values();
    }

    @Override
    public String toString() {
        return "Node{" + id + ": (" + x + "," + y + ")}";
    }
}
