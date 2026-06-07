package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PlaneStressSolverTest {

    @Test
    public void testSimpleSquarePlaneStress() {
        FEAData data = new FEAData();
        
        // Square: (0,0), (1,0), (1,1), (0,1)
        List<PolygonRegion.Vertex> vertices = Arrays.asList(
            new PolygonRegion.Vertex(0, 0),
            new PolygonRegion.Vertex(1, 0),
            new PolygonRegion.Vertex(1, 1),
            new PolygonRegion.Vertex(0, 1)
        );
        
        Material mat = new Material(1, "Steel", 2.0e11, 7850.0, 2.5e8, 0.3, 0.01);
        data.addMaterial(mat);
        
        PolygonRegion region = new PolygonRegion(1, vertices, 1);
        data.addPolygonRegion(region);
        
        // Fix the left edge (Edge from (0,1) to (0,0) is index 3)
        data.addEdgeSupport(new EdgeSupport(1, 1, 3, Support.Type.FIXED));
        
        // Apply distributed load on right edge (Edge from (1,0) to (1,1) is index 1)
        // Load in X direction: 1000 N/m
        data.addDistributedLoad(new DistributedLoad(1, 1, 1, 1000, 0));
        
        PlaneStressSolver solver = new PlaneStressSolver();
        PlaneStressResult result = (PlaneStressResult) solver.solve(data);
        
        assertNotNull(result);
        assertTrue(result.getDisplacements().length > 0);
        assertFalse(result.getElementStresses().isEmpty());
        
        // Check if displacements at fixed edge are zero
        // In the simple mesh, vertex 0 and 3 are nodes on the fixed edge.
        // Node mapping in TriangularMeshGenerator: nodes are in order of vertices.
        int idx0 = result.getNodeIdToIndex().get(1); // Vertex 0
        int idx3 = result.getNodeIdToIndex().get(4); // Vertex 3
        
        assertEquals(0, result.getDisplacements()[2 * idx0], 1e-12);
        assertEquals(0, result.getDisplacements()[2 * idx0 + 1], 1e-12);
        assertEquals(0, result.getDisplacements()[2 * idx3], 1e-12);
        assertEquals(0, result.getDisplacements()[2 * idx3 + 1], 1e-12);
        
        // Right edge nodes should have moved in positive X
        int idx1 = result.getNodeIdToIndex().get(2); // Vertex 1
        int idx2 = result.getNodeIdToIndex().get(3); // Vertex 2
        assertTrue(result.getDisplacements()[2 * idx1] > 0);
        assertTrue(result.getDisplacements()[2 * idx2] > 0);
        
        // Check stresses
        for (PlaneStressResult.ElementStress stress : result.getElementStresses()) {
            assertTrue(stress.sigmaX > 0); // Tension
        }
    }
}
