package com.treble.feasimulation.mesh;

import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PolygonRegion;
import com.treble.feasimulation.model.TriangularElement;

import java.util.List;

/**
 * Result of a mesh generation process.
 */
public interface MeshGenerator {
    /**
     * Data class to hold the generated mesh.
     */
    class MeshResult {
        private final List<Node> nodes;
        private final List<TriangularElement> elements;

        public MeshResult(List<Node> nodes, List<TriangularElement> elements) {
            this.nodes = nodes;
            this.elements = elements;
        }

        public List<Node> getNodes() { return nodes; }
        public List<TriangularElement> getElements() { return elements; }
    }

    /**
     * Generates a mesh for the given polygon region with globally unique IDs.
     *
     * @param region       The polygon region to mesh.
     * @param density      Mesh density (target edge length; smaller = finer mesh).
     * @param startNodeId  First node ID to assign; caller increments this by the node count
     *                     before calling again for the next region.
     * @param startElementId First element ID to assign; same contract as startNodeId.
     * @return A MeshResult whose node and element IDs begin at the supplied offsets.
     */
    MeshResult generateMesh(PolygonRegion region, double density, int startNodeId, int startElementId);

    /**
     * Convenience overload that starts IDs at 1 — safe for single-region models and tests.
     */
    default MeshResult generateMesh(PolygonRegion region, double density) {
        return generateMesh(region, density, 1, 1);
    }
}
