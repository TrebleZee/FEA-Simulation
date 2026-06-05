package com.treble.feasimulation.model;

/**
 * Boundary condition / support at a node.
 */
public class Support {
    public enum Type { FIXED, PINNED, ROLLER }

    private final int id;
    private final int nodeId;
    private final Type type;
    private final boolean restrainX;
    private final boolean restrainY;
    private final boolean restrainRotation;

    public Support(int id, int nodeId, Type type) {
        this.id = id;
        this.nodeId = nodeId;
        this.type = type;
        switch (type) {
            case FIXED:
                this.restrainX = true; this.restrainY = true; this.restrainRotation = true; break;
            case PINNED:
                this.restrainX = true; this.restrainY = true; this.restrainRotation = false; break;
            case ROLLER:
                // Typical roller: restrains vertical (y) but allows horizontal movement and rotation
                this.restrainX = false; this.restrainY = true; this.restrainRotation = false; break;
            default:
                this.restrainX = false; this.restrainY = false; this.restrainRotation = false; break;
        }
    }

    /**
     * Full constructor allowing custom restraint flags
     */
    public Support(int id, int nodeId, boolean restrainX, boolean restrainY, boolean restrainRotation) {
        this.id = id;
        this.nodeId = nodeId;
        this.type = null;
        this.restrainX = restrainX;
        this.restrainY = restrainY;
        this.restrainRotation = restrainRotation;
    }

    public int getId() { return id; }
    public int getNodeId() { return nodeId; }
    public Type getType() { return type; }
    public boolean isRestrainX() { return restrainX; }
    public boolean isRestrainY() { return restrainY; }
    public boolean isRestrainRotation() { return restrainRotation; }

    @Override
    public String toString() {
        return "Support{" + id + ": node=" + nodeId + ", type=" + type + ", rx=" + restrainX + ", ry=" + restrainY + ", rtheta=" + restrainRotation + "}";
    }
}
