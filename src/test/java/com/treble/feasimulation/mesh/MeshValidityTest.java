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

        // With density-driven subdivision, a triangle may be split into many small triangles.
        assertTrue(result.getElements().size() >= 1, "Should create at least one triangle");

        // Sum of areas should equal the polygon area (within tolerance)
        double expectedArea = 0.5 * 100 * 50; // 2500
        double areaSum = 0.0;
        for (TriangularElement elem : result.getElements()) {
            assertTrue(elem.getArea() > 0, "Triangle area should be positive");
            areaSum += elem.getArea();
        }
        assertEquals(expectedArea, areaSum, 1e-6, "Total meshed area should match polygon area");
    }

    @Test
    public void testMeshDensityPresence() {
        double[][] coords = {{0, 0}, {100, 0}, {100, 100}, {0, 100}};
        PolygonRegion square = PolygonRegion.fromCoordinates(1, coords, 1);
        TriangularMeshGenerator generator = new TriangularMeshGenerator();
        
        // Density affects subdivision: smaller target length -> more triangles
        MeshGenerator.MeshResult dense = generator.generateMesh(square, 1.0);   // smaller target, denser
        MeshGenerator.MeshResult coarse = generator.generateMesh(square, 10.0); // larger target, coarser

        assertTrue(dense.getElements().size() >= coarse.getElements().size(),
            "Denser mesh should have at least as many elements as coarse mesh");
        assertNotEquals(dense.getElements().size(), coarse.getElements().size(),
            "Density parameter should affect triangle count");
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
