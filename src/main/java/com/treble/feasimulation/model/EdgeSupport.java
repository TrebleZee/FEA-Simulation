package com.treble.feasimulation.model;

/**
 * Boundary condition / support applied to a polygon edge.
 */
public class EdgeSupport {
    private final int id;
    private final int polygonId;
    private final int edgeIndex;
    private final Support.Type type;

    public EdgeSupport(int id, int polygonId, int edgeIndex, Support.Type type) {
        this.id = id;
        this.polygonId = polygonId;
        this.edgeIndex = edgeIndex;
        this.type = type;
    }

    public int getId() { return id; }
    public int getPolygonId() { return polygonId; }
    public int getEdgeIndex() { return edgeIndex; }
    public Support.Type getType() { return type; }

    @Override
    public String toString() {
        return "EdgeSupport{" + id + ": polygon=" + polygonId + ", edge=" + edgeIndex + ", type=" + type + "}";
    }
}
