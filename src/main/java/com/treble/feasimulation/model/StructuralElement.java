package com.treble.feasimulation.model;

/**
 * Base class for all structural elements that possess physical properties like area.
 */
public abstract class StructuralElement extends Element {
    public StructuralElement(int id, int[] nodeIds, int materialId, double area) {
        super(id, nodeIds, materialId, area);
    }
}
