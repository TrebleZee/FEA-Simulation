package com.treble.feasimulation.model;

import java.util.Arrays;

public abstract class Element {
    private final int id;
    private final int[] nodeIds;
    private final int materialId;

    public Element(int id, int[] nodeIds, int materialId) {
        this.id = id;
        this.nodeIds = nodeIds.clone();
        this.materialId = materialId;
    }

    public int getId() {
        return id;
    }

    public int[] getNodeIds() {
        return nodeIds.clone();
    }

    public int getNodeStartId() {
        return nodeIds[0];
    }

    public int getNodeEndId() {
        return nodeIds[nodeIds.length - 1];
    }

    public int getMaterialId() {
        return materialId;
    }

    /**
     * @return the DOFs active for each node of this element.
     */
    public abstract Node.DOF[] getActiveDOFs();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + id + ": nodes=" + Arrays.toString(nodeIds) + ", mat=" + materialId + "}";
    }
}
