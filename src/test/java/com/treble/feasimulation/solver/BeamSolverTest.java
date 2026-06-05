package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BeamSolverTest {

    @Test
    public void cantileverEndLoad() {
        FEAData data = new FEAData();
        double L = 2.0;
        double P = 1000.0;
        double E = 2.0e11;
        double I = 1.0e-6;

        // Nodes: 1 at x=0, 2 at x=L
        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, L, 0.0));

        data.addMaterial(new Material(1, "Steel", E, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));

        // fixed support at node 1
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        // apply downward point load at node 2
        data.addPointLoad(new PointLoad(1, 2, 0.0, -P));

        BeamSolver solver = new BeamSolver();
        BeamSolver.Result r = solver.solve(data);

        // displacement at node 2 (index 1): vertical DOF is at 3*1 + 1
        double v2 = Math.abs(r.displacements[3 * 1 + 1]);
        double expectedV = P * Math.pow(L, 3) / (3.0 * E * I);
        assertEquals(expectedV, v2, expectedV * 0.05, "Cantilever end deflection should match analytical within 5%");

        // rotation at node 2 (index 1): rotation DOF is at 3*1 + 2
        double theta2 = Math.abs(r.displacements[3 * 1 + 2]);
        double expectedTheta = P * L * L / (2.0 * E * I);
        assertEquals(expectedTheta, theta2, expectedTheta * 0.05, "Cantilever end rotation should match analytical within 5%");

        // element end moments: fixed end moment magnitude should be P*L
        boolean found = false;
        for (BeamSolver.ElementResult er : r.elementResults) {
            if (er.elementId == 1) {
                double Mstart = Math.abs(er.endMomentStart);
                double Mend = Math.abs(er.endMomentEnd);
                assertEquals(P * L, Mstart, Math.max(1e-6, Math.abs(P * L) * 0.05));
                // end moment at free end should be near zero
                assertEquals(0.0, Mend, 1e-3);
                assertNotNull(er.bendingStress);
                double expectedY = Math.sqrt(3.0 * I / 1.0);
                double expectedStress = P * L * expectedY / I;
                assertEquals(expectedY, er.bendingStress.extremeFiberDistance, expectedY * 1e-9);
                assertEquals(P * L, er.bendingStress.maxBendingMoment, Math.max(1e-6, Math.abs(P * L) * 0.05));
                assertEquals(expectedStress, er.bendingStress.maxTensileStress, expectedStress * 0.05);
                assertEquals(-expectedStress, er.bendingStress.maxCompressiveStress, expectedStress * 0.05);
                found = true;
            }
        }
        assertTrue(found, "Element results should contain element 1");
    }

    @Test
    public void simplySupportedMidLoad() {
        FEAData data = new FEAData();
        double L = 2.0;
        double P = 1000.0;
        double E = 2.0e11;
        double I = 1.0e-6;

        // Nodes: 1 at 0, 2 at L/2, 3 at L
        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, L / 2.0, 0.0));
        data.addNode(new Node(3, L, 0.0));

        data.addMaterial(new Material(1, "Steel", E, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));
        data.addElement(new BeamElement(2, 2, 3, 1, 1.0, I));

        // supports: pinned at node1, roller at node3 (both restrain vertical)
        data.addSupport(new Support(1, 1, Support.Type.PINNED));
        data.addSupport(new Support(2, 3, Support.Type.ROLLER));

        // mid-point downward load at node 2
        data.addPointLoad(new PointLoad(1, 2, 0.0, -P));

        BeamSolver solver = new BeamSolver();
        BeamSolver.Result r = solver.solve(data);

        // analytical max moment at mid span = P*L/4
        double expectedM = P * L / 4.0;

        double maxM = 0.0;
        for (BeamSolver.ElementResult er : r.elementResults) {
            maxM = Math.max(maxM, Math.abs(er.endMomentStart));
            maxM = Math.max(maxM, Math.abs(er.endMomentEnd));
            assertNotNull(er.bendingStress);
        }
        assertEquals(expectedM, maxM, expectedM * 0.10, "Max moment should match analytical within 10%");
    }

    @Test
    public void simplySupportedLoadPlacedOnSplitBeam() {
        FEAData data = new FEAData();
        double L = 2.0;
        double P = 1000.0;
        double E = 2.0e11;
        double I = 1.0e-6;

        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, L, 0.0));
        data.addMaterial(new Material(1, "Steel", E, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));
        data.addSupport(new Support(1, 1, Support.Type.PINNED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));

        int midNodeId = data.splitElementAtPoint(1, L / 2.0, 0.0);
        data.addPointLoad(new PointLoad(1, midNodeId, 0.0, -P));

        BeamSolver solver = new BeamSolver();
        BeamSolver.Result r = solver.solve(data);

        int midNodeIndex = -1;
        for (int i = 0; i < data.getNodes().size(); i++) {
            if (data.getNodes().get(i).getId() == midNodeId) {
                midNodeIndex = i;
                break;
            }
        }
        assertTrue(midNodeIndex >= 0);

        double vMid = Math.abs(r.displacements[3 * midNodeIndex + 1]);
        double expectedV = P * Math.pow(L, 3) / (48.0 * E * I);
        assertEquals(expectedV, vMid, expectedV * 0.10, "Midspan deflection should match analytical within 10%");

        double expectedM = P * L / 4.0;
        double maxM = 0.0;
        for (BeamSolver.ElementResult er : r.elementResults) {
            maxM = Math.max(maxM, Math.abs(er.endMomentStart));
            maxM = Math.max(maxM, Math.abs(er.endMomentEnd));
            assertNotNull(er.bendingStress);
        }
        assertEquals(expectedM, maxM, expectedM * 0.10, "Max moment should match analytical within 10%");
    }
}
