package com.treble.feasimulation.model;

public class Load {
    private final int id;
    private final int nodeId;
    private final double fx;
    private final double fy;

    public Load(int id, int nodeId, double fx, double fy) {
        this.id = id;
        this.nodeId = nodeId;
        this.fx = fx;
        this.fy = fy;
    }

    public int getId() {
        return id;
    }

    public int getNodeId() {
        return nodeId;
    }

    public double getFx() {
        return fx;
    }

    public double getFy() {
        return fy;
    }

    @Override
    public String toString() {
        return "Load{" + id + ": node=" + nodeId + ", (" + fx + "," + fy + ")}";
    }
}
