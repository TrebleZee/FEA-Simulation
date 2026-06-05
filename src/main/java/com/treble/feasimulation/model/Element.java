package com.treble.feasimulation.model;

import java.util.Arrays;

public class Element {
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

    public int getMaterialId() {
        return materialId;
    }

    @Override
    public String toString() {
        return "Element{" + id + ": nodes=" + Arrays.toString(nodeIds) + ", mat=" + materialId + "}";
    }
}
