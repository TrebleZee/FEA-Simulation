package com.treble.feasimulation.model;

/**
 * Distributed load applied to a polygon edge.
 */
public class DistributedLoad {
    public enum Type { UNIFORM, DIRECTIONAL }

    private final int id;
    private final int polygonId;
    private final int edgeIndex;
    private final double wx; // Distributed load in x-direction (N/m) or normal magnitude
    private final double wy; // Distributed load in y-direction (N/m) or tangential magnitude
    private final Type type;

    public DistributedLoad(int id, int polygonId, int edgeIndex, double wx, double wy) {
        this(id, polygonId, edgeIndex, wx, wy, Type.DIRECTIONAL);
    }

    public DistributedLoad(int id, int polygonId, int edgeIndex, double wx, double wy, Type type) {
        this.id = id;
        this.polygonId = polygonId;
        this.edgeIndex = edgeIndex;
        this.wx = wx;
        this.wy = wy;
        this.type = type;
    }

    public int getId() { return id; }
    public int getPolygonId() { return polygonId; }
    public int getEdgeIndex() { return edgeIndex; }
    public double getWx() { return wx; }
    public double getWy() { return wy; }
    public Type getType() { return type; }

    @Override
    public String toString() {
        return "DistributedLoad{" + id + ": polygon=" + polygonId + ", edge=" + edgeIndex + ", (" + wx + "," + wy + "), type=" + type + "}";
    }
}
