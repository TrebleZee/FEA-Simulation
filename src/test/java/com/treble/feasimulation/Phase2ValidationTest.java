package com.treble.feasimulation;

import com.treble.feasimulation.model.*;
import com.treble.feasimulation.solver.TrussSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class Phase2ValidationTest {

    @Test
    public void testLargeTrussPerformance() {
        FEAData data = new FEAData();
        int nBays = 100;
        double L = 1.0;
        // Create a long truss bridge
        for (int i = 0; i <= nBays; i++) {
            data.addNode(new TrussNode(i * 2, i * L, 0)); // Bottom
            data.addNode(new TrussNode(i * 2 + 1, i * L + L/2, L)); // Top
        }

        for (int i = 0; i < nBays; i++) {
            data.addElement(new TrussMember(i * 4, i * 2, (i + 1) * 2, 0, 0.01)); // Bottom chord
            data.addElement(new TrussMember(i * 4 + 1, i * 2 + 1, (i + 1) * 2 + 1, 0, 0.01)); // Top chord
            data.addElement(new TrussMember(i * 4 + 2, i * 2, i * 2 + 1, 0, 0.01)); // Vertical/Diagonal
            data.addElement(new TrussMember(i * 4 + 3, i * 2 + 1, (i + 1) * 2, 0, 0.01)); // Diagonal
        }
        // Last vertical
        data.addElement(new TrussMember(nBays * 4, nBays * 2, nBays * 2 + 1, 0, 0.01));

        data.addSupport(new Support(1, 0, Support.Type.FIXED)); // node 0
        data.addSupport(new Support(2, nBays * 2, Support.Type.ROLLER)); // last bottom node

        data.addPointLoad(new PointLoad(1, nBays * 2 + 1, 0, -10000)); // Load at the end top node

        TrussSolver solver = new TrussSolver();
        long start = System.currentTimeMillis();
        TrussSolver.Result result = solver.solve(data);
        long end = System.currentTimeMillis();

        System.out.println("[DEBUG_LOG] Large Truss (400 members) solve time: " + (end - start) + "ms");
        assertNotNull(result);
        assertTrue((end - start) < 500, "Should solve within 500ms");
    }

    @Test
    public void testStabilityDetection() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, 0.01));
        
        // Pinned at one end, free at other -> Mechanism (rotation around pin)
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        
        TrussSolver solver = new TrussSolver();
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            solver.solve(data);
        });
        
        System.out.println("[DEBUG_LOG] Stability exception message: " + exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("unstable"), "Message was: " + exception.getMessage());
    }

    @Test
    public void testWarrenTrussAccuracy() {
        FEAData data = new FEAData();
        // 2-bay Warren Truss
        // Nodes
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 2, 0));
        data.addNode(new TrussNode(3, 4, 0));
        data.addNode(new TrussNode(4, 1, 1.732)); // ~60 deg
        data.addNode(new TrussNode(5, 3, 1.732));

        // Members
        data.addElement(new TrussMember(1, 1, 2, 0, 0.01));
        data.addElement(new TrussMember(2, 2, 3, 0, 0.01));
        data.addElement(new TrussMember(3, 4, 5, 0, 0.01));
        data.addElement(new TrussMember(4, 1, 4, 0, 0.01));
        data.addElement(new TrussMember(5, 4, 2, 0, 0.01));
        data.addElement(new TrussMember(6, 2, 5, 0, 0.01));
        data.addElement(new TrussMember(7, 5, 3, 0, 0.01));

        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 3, Support.Type.ROLLER));
        
        // Load at center bottom node
        data.addPointLoad(new PointLoad(1, 2, 0, -10000));

        TrussSolver solver = new TrussSolver();
        TrussSolver.Result result = solver.solve(data);

        assertNotNull(result);
        // Symmetry check: Elements 4 and 7 should have same force (absolute)
        double force4 = 0, force7 = 0;
        for (var er : result.elementResults) {
            if (er.elementId == 4) force4 = er.axialForce;
            if (er.elementId == 7) force7 = er.axialForce;
        }
        assertEquals(force4, force7, 1e-3, "Symmetry check failed");
        assertTrue(force4 < 0, "Member 4 should be in compression");
    }

    @Test
    public void testArchitectureConsistency() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, 0.01));

        assertTrue(data.getNodes().get(0) instanceof TrussNode);
        assertTrue(data.getElements().get(0) instanceof TrussMember);
    }
}
