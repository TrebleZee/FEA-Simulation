package com.treble.feasimulation.mesh;

import com.treble.feasimulation.model.PolygonRegion;
import com.treble.feasimulation.model.TriangularElement;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MeshValidityTest {

    @Test
    public void testTriangleValidity() {
        double[][] coords = {{0, 0}, {100, 0}, {50, 50}};
        PolygonRegion triangle = PolygonRegion.fromCoordinates(1, coords, 1);
        TriangularMeshGenerator generator = new TriangularMeshGenerator();
        MeshGenerator.MeshResult result = generator.generateMesh(triangle, 1.0);
        
        assertEquals(1, result.getElements().size());
        TriangularElement elem = result.getElements().get(0);
        assertTrue(elem.getArea() > 0);
        
        // Check for inverted triangles (Area should be correct regardless of vertex order due to Math.abs)
        // However, CCW order is preferred for some operations.
        assertEquals(0.5 * 100 * 50, elem.getArea(), 1e-9);
    }

    @Test
    public void testMeshDensityPresence() {
        double[][] coords = {{0, 0}, {100, 0}, {100, 100}, {0, 100}};
        PolygonRegion square = PolygonRegion.fromCoordinates(1, coords, 1);
        TriangularMeshGenerator generator = new TriangularMeshGenerator();
        
        // Current implementation IGNORES density. 
        // This test documents current behavior and will fail if we improve it.
        MeshGenerator.MeshResult result1 = generator.generateMesh(square, 1.0);
        MeshGenerator.MeshResult result2 = generator.generateMesh(square, 10.0);
        
        assertEquals(result1.getElements().size(), result2.getElements().size(), 
            "Mesh density currently has no effect on ear-clipping generator");
    }

    @Test
    public void testNoSelfIntersectionHandling() {
        // Self-intersecting polygon (bowtie)
        // Ear-clipping might fail or produce overlapping triangles.
        double[][] coords = {{0, 0}, {100, 100}, {100, 0}, {0, 100}};
        PolygonRegion bowtie = PolygonRegion.fromCoordinates(1, coords, 1);
        TriangularMeshGenerator generator = new TriangularMeshGenerator();
        
        MeshGenerator.MeshResult result = generator.generateMesh(bowtie, 1.0);
        // We just want to see if it crashes or produces something weird.
        assertNotNull(result);
    }
}
