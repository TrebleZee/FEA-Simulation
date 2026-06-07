package com.treble.feasimulation.mesh;

import com.treble.feasimulation.model.PolygonRegion;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.TriangularElement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class TriangularMeshGeneratorTest {

    @Test
    public void testSquareTriangulation() {
        // Create a unit square
        double[][] coords = {
            {0, 0},
            {1, 0},
            {1, 1},
            {0, 1}
        };
        PolygonRegion square = PolygonRegion.fromCoordinates(1, coords, 1);
        TriangularMeshGenerator generator = new TriangularMeshGenerator();
        
        MeshGenerator.MeshResult result = generator.generateMesh(square);
        
        assertNotNull(result);
        assertEquals(4, result.getNodes().size(), "Should have 4 nodes for a square");
        assertEquals(2, result.getElements().size(), "Should have 2 triangles for a square");
        
        // Check that all triangles have area > 0
        for (TriangularElement element : result.getElements()) {
            assertTrue(element.getArea() > 0, "Triangle area should be positive");
        }
    }

    @Test
    public void testLPolygonTriangulation() {
        // L-shaped polygon (concave)
        double[][] coords = {
            {0, 0},
            {2, 0},
            {2, 1},
            {1, 1},
            {1, 2},
            {0, 2}
        };
        PolygonRegion lPoly = PolygonRegion.fromCoordinates(1, coords, 1);
        TriangularMeshGenerator generator = new TriangularMeshGenerator();
        
        MeshGenerator.MeshResult result = generator.generateMesh(lPoly);
        
        assertNotNull(result);
        assertEquals(6, result.getNodes().size(), "Should have 6 nodes");
        assertEquals(4, result.getElements().size(), "Should have 4 triangles for a 6-vertex polygon (n-2)");
        
        for (TriangularElement element : result.getElements()) {
            assertTrue(element.getArea() > 0, "Triangle area should be positive");
        }
    }
}
