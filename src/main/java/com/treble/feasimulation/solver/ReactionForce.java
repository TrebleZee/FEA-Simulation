package com.treble.feasimulation.solver;

/**
 * Support reaction force at a constrained node.
 * Units follow the model: Fx, Fy in N; moment in N·m.
 */
public class ReactionForce {
    private final int supportId;
    private final int nodeId;
    private final double fx;
    private final double fy;
    private final double moment;

    public ReactionForce(int supportId, int nodeId, double fx, double fy, double moment) {
        this.supportId = supportId;
        this.nodeId = nodeId;
        this.fx = fx;
        this.fy = fy;
        this.moment = moment;
    }

    public int getSupportId() { return supportId; }
    public int getNodeId() { return nodeId; }
    public double getFx() { return fx; }
    public double getFy() { return fy; }
    public double getMoment() { return moment; }
}
