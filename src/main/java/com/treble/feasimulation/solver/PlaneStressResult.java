package com.treble.feasimulation.solver;

import java.util.List;
import java.util.Map;

/**
 * Result of a plane-stress analysis.
 */
public class PlaneStressResult implements SolverResult {
    private final double[] displacements;
    private final List<ElementStress> elementStresses;
    private final Map<Integer, Integer> nodeIdToIndex;
    private final List<com.treble.feasimulation.model.TriangularElement> elements;

    public PlaneStressResult(double[] displacements, List<ElementStress> elementStresses, Map<Integer, Integer> nodeIdToIndex, List<com.treble.feasimulation.model.TriangularElement> elements) {
        this.displacements = displacements;
        this.elementStresses = elementStresses;
        this.nodeIdToIndex = nodeIdToIndex;
        this.elements = elements;
    }

    @Override
    public double[] getDisplacements() {
        return displacements;
    }

    public List<ElementStress> getElementStresses() {
        return elementStresses;
    }

    public Map<Integer, Integer> getNodeIdToIndex() {
        return nodeIdToIndex;
    }

    public List<com.treble.feasimulation.model.TriangularElement> getElements() {
        return elements;
    }

    public static class ElementStress {
        public final int elementId;
        public final double sigmaX;
        public final double sigmaY;
        public final double tauXY;
        public final double vonMises;

        public ElementStress(int elementId, double sigmaX, double sigmaY, double tauXY) {
            this.elementId = elementId;
            this.sigmaX = sigmaX;
            this.sigmaY = sigmaY;
            this.tauXY = tauXY;
            // Von Mises stress for plane stress: sqrt(sigmaX^2 - sigmaX*sigmaY + sigmaY^2 + 3*tauXY^2)
            this.vonMises = Math.sqrt(sigmaX * sigmaX - sigmaX * sigmaY + sigmaY * sigmaY + 3 * tauXY * tauXY);
        }
    }
}
