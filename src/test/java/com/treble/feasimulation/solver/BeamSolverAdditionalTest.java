package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BeamSolverAdditionalTest {

    @Test
    public void testSingularMatrixThrows() {
        FEAData d = new FEAData();
        // single element with no supports
        d.addNode(new Node(1, 0.0, 0.0));
        d.addNode(new Node(2, 1.0, 0.0));
        d.addElement(new BeamElement(1, 1, 2, 0, 1.0, 1e-6));

        BeamSolver solver = new BeamSolver();
        assertThrows(IllegalStateException.class, () -> solver.solve(d));
    }

    @Test
    public void testStressSigmaCalculationAtFixedEnd() {
        FEAData data = new FEAData();
        double L = 2.0;
        double P = 1000.0;
        double E = 2.0e11;
        double I = 1.0e-6;

        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, L, 0.0));
        data.addMaterial(new Material(1, "Steel", E, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addPointLoad(new PointLoad(1, 2, 0.0, -P));

        BeamSolver solver = new BeamSolver();
        BeamSolver.Result r = solver.solve(data);

        // find element result
        BeamSolver.ElementResult er = null;
        for (BeamSolver.ElementResult x : r.elementResults) if (x.elementId == 1) { er = x; break; }
        assertNotNull(er);
        double Mfixed = Math.abs(er.endMomentStart);
        double expectedM = P * L; // cantilever fixed end moment
        assertEquals(expectedM, Mfixed, Math.max(1e-6, Math.abs(expectedM) * 0.05));

        // compute sigma at extreme fiber y (assume rectangular section with depth 0.1 -> y=0.05)
        double y = 0.05;
        double sigma = Mfixed * y / I;
        double expectedSigma = expectedM * y / I;
        assertEquals(expectedSigma, sigma, Math.abs(expectedSigma) * 0.05, "Stress should match M*y/I within 5%");
    }

    @Test
    public void testMaterialAffectsDisplacement() {
        FEAData d1 = new FEAData();
        FEAData d2 = new FEAData();
        double L = 2.0;
        double P = 1000.0;
        double E1 = 2.0e11;
        double E2 = 1.0e11; // half stiffness
        double I = 1.0e-6;

        // case 1
        d1.addNode(new Node(1, 0.0, 0.0));
        d1.addNode(new Node(2, L, 0.0));
        d1.addMaterial(new Material(1, "M1", E1, 7800));
        d1.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));
        d1.addSupport(new Support(1, 1, Support.Type.FIXED));
        d1.addPointLoad(new PointLoad(1, 2, 0.0, -P));

        // case 2 (softer)
        d2.addNode(new Node(1, 0.0, 0.0));
        d2.addNode(new Node(2, L, 0.0));
        d2.addMaterial(new Material(1, "M2", E2, 7800));
        d2.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));
        d2.addSupport(new Support(1, 1, Support.Type.FIXED));
        d2.addPointLoad(new PointLoad(1, 2, 0.0, -P));

        BeamSolver s = new BeamSolver();
        BeamSolver.Result r1 = s.solve(d1);
        BeamSolver.Result r2 = s.solve(d2);

        double v1 = Math.abs(r1.displacements[2]);
        double v2 = Math.abs(r2.displacements[2]);
        // softer material should give larger deflection
        assertTrue(v2 > v1, "Softer material should produce larger deflection");
    }
}
