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
        
        MeshGenerator.MeshResult result = generator.generateMesh(square, 1.0);
        
        assertNotNull(result);
        assertTrue(result.getNodes().size() >= 4, "Should have at least original polygon vertices as nodes");
        assertTrue(result.getElements().size() >= 2, "Should have at least n-2 triangles");
        
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
        
        MeshGenerator.MeshResult result = generator.generateMesh(lPoly, 1.0);
        
        assertNotNull(result);
        assertTrue(result.getNodes().size() >= 6, "Should have at least original vertices as nodes");
        assertTrue(result.getElements().size() >= 4, "Should have at least n-2 triangles for a simple polygon");
        
        for (TriangularElement element : result.getElements()) {
            assertTrue(element.getArea() > 0, "Triangle area should be positive");
        }
    }
}
