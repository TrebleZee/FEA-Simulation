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
     * Generates a mesh for the given polygon region.
     *
     * @param region The polygon region to mesh.
     * @param density Mesh density (e.g., maximum edge length or target number of elements).
     * @return A MeshResult containing nodes and elements.
     */
    MeshResult generateMesh(PolygonRegion region, double density);
}
