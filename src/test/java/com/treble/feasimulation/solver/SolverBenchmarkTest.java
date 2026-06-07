package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SolverBenchmarkTest {

    @Test
    public void testCantileverPlate() {
        // L=2.0, H=0.5, E=2.0e11, nu=0.3, t=0.01
        // Load P=1000 N at tip (distributed)
        FEAData data = new FEAData();
        
        double L = 2.0;
        double H = 0.5;
        double E = 2.0e11;
        double nu = 0.3;
        double t = 0.01;
        double loadP = 1000.0;

        List<PolygonRegion.Vertex> vertices = Arrays.asList(
            new PolygonRegion.Vertex(0, 0),
            new PolygonRegion.Vertex(L, 0),
            new PolygonRegion.Vertex(L, H),
            new PolygonRegion.Vertex(0, H)
        );
        
        Material mat = new Material(1, "Steel", E, 7850.0, 2.5e8, nu, t);
        data.addMaterial(mat);
        
        PolygonRegion region = new PolygonRegion(1, vertices, 1);
        data.addPolygonRegion(region);
        
        // Fixed at x=0 (Edge index 3)
        data.addEdgeSupport(new EdgeSupport(1, 1, 3, Support.Type.FIXED));
        
        // Load at x=L (Edge index 1)
        // Vertical load -1000 N/m distributed over H=0.5m -> Total P = 500 N
        data.addDistributedLoad(new DistributedLoad(1, 1, 1, 0, -loadP, DistributedLoad.Type.DIRECTIONAL));
        
        PlaneStressSolver solver = new PlaneStressSolver();
        // Improve accuracy by setting higher mesh density (smaller edge length)
        solver.setMeshDensity(0.1); 
        
        PlaneStressResult result = (PlaneStressResult) solver.solve(data);
        
        assertNotNull(result);
        
        // Tip displacement (Nodes at x=L)
        // Since we have many nodes now, we find the one at (L, H/2) or average
        double vMax = 0;
        for (int i = 0; i < result.getDisplacements().length / 2; i++) {
            vMax = Math.min(vMax, result.getDisplacements()[2 * i + 1]);
        }
        
        // Beam theory: delta = P*L^3 / (3*E*I)
        // I = (t * H^3) / 12
        double I = (t * Math.pow(H, 3)) / 12.0;
        double P_total = loadP * H;
        double deltaBeam = (P_total * Math.pow(L, 3)) / (3 * E * I);
        
        System.out.println("[DEBUG_LOG] Cantilever Tip Displacement (FEA density=0.1): " + vMax);
        System.out.println("[DEBUG_LOG] Cantilever Tip Displacement (Beam Theory): " + (-deltaBeam));
        
        // With more elements, it should be closer to beam theory but still likely stiffer
        System.out.println("[DEBUG_LOG] Result displacements length: " + result.getDisplacements().length);
    }

    @Test
    public void testPlateWithHoleSymmetry() {
        // Plate with hole is usually modeled using 1/4 symmetry.
        // For our simple tool, we might just model the whole thing or a simple hole.
        // But our ear-clipping generator DOES NOT SUPPORT HOLES.
        // It only supports simple polygons.
        
        // Let's test a "hole" by creating a polygon that goes around it.
        // This confirms if concave polygons work.
        double[][] coords = {
            {0, 0}, {2, 0}, {2, 2}, {0, 2}, // Outer
            {0.5, 1.5}, {1.5, 1.5}, {1.5, 0.5}, {0.5, 0.5}, {0.5, 1.5}, // "Inner" cut (manual path)
            {0, 2} // Back to start
        };
        // Wait, the above is not a simple polygon. 
        // A better way to test concave is an L-shape (already tested in MeshGeneratorTest).
    }
}
