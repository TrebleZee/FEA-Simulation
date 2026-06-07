package com.treble.feasimulation.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TriangularMeshGeneratorTest {

    @Test
    public void testGenerateMeshStructure() {
        List<PolygonRegion.Vertex> vertices = Arrays.asList(
            new PolygonRegion.Vertex(0, 0),
            new PolygonRegion.Vertex(1, 0),
            new PolygonRegion.Vertex(0, 1)
        );
        PolygonRegion region = new PolygonRegion(1, vertices, 1);
        
        MeshGenerator generator = new TriangularMeshGenerator();
        Mesh mesh = generator.generateMesh(region);
        
        assertNotNull(mesh);
        assertNotNull(mesh.getNodes());
        assertNotNull(mesh.getElements());
    }
}
