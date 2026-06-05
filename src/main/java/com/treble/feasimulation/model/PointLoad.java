package com.treble.feasimulation.model;

/**
 * Concentrated point load applied at a node (2D): fx, fy, and optional moment.
 */
public class PointLoad {
    private final int id;
    private final int nodeId;
    private final double fx;
    private final double fy;
    private final double moment; // positive out of plane

    public PointLoad(int id, int nodeId, double fx, double fy) {
        this(id, nodeId, fx, fy, 0.0);
    }

    public PointLoad(int id, int nodeId, double fx, double fy, double moment) {
        this.id = id;
        this.nodeId = nodeId;
        this.fx = fx;
        this.fy = fy;
        this.moment = moment;
    }

    public int getId() { return id; }
    public int getNodeId() { return nodeId; }
    public double getFx() { return fx; }
    public double getFy() { return fy; }
    public double getMoment() { return moment; }

    @Override
    public String toString() {
        return "PointLoad{" + id + ": node=" + nodeId + ", (" + fx + "," + fy + "), M=" + moment + "}";
    }
}
