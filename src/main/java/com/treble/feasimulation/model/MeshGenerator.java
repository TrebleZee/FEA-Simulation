package com.treble.feasimulation.model;

public interface MeshGenerator {
    /**
     * Generates a mesh for a given polygon region.
     * 
     * @param region The polygon region to mesh.
     * @return A Mesh object containing nodes and elements.
     */
    Mesh generateMesh(PolygonRegion region);
}
