package com.treble.feasimulation;

import com.treble.feasimulation.model.*;
import com.treble.feasimulation.presenter.ModelValidator;
import com.treble.feasimulation.solver.MatrixTransformUtils;
import com.treble.feasimulation.solver.TrussSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended Phase 2 coverage: solver correctness, stability, validation, and known truss examples.
 */
public class Phase2ComprehensiveTest {

    private static final double E = 210e9;
    private static final double A = 0.01;

    @Test
    public void axialStiffnessMatchesAnalyticalDisplacement() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 2, 0));
        data.addMaterial(new Material(1, "Steel", E, 0.3));
        TrussMember member = new TrussMember(1, 1, 2, 1, A);
        data.addElement(member);

        double L = 2.0;
        double k = member.computeAxialStiffness(E, L);
        assertEquals(E * A / L, k, 1e-6);

        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));
        data.addPointLoad(new PointLoad(1, 2, 2000, 0));

        TrussSolver.Result result = new TrussSolver().solve(data);
        double expectedUx = 2000.0 * L / (E * A);
        assertEquals(expectedUx, result.displacements[2], 1e-9);
        assertEquals(0, result.displacements[3], 1e-9);
    }

    @Test
    public void globalStiffnessAssemblyForHorizontalBar() {
        // Single horizontal bar: K should reduce to EA/L for the free UX DOF at node 2
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addMaterial(new Material(1, "Steel", E, 0.3));
        data.addElement(new TrussMember(1, 1, 2, 1, A));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));
        data.addPointLoad(new PointLoad(1, 2, 500, 0));

        TrussSolver.Result result = new TrussSolver().solve(data);
        double expected = 500.0 / (E * A / 1.0);
        assertEquals(expected, result.displacements[2], 1e-12);
    }

    @Test
    public void localToGlobalTransformAt45Degrees() {
        double c = Math.sqrt(2) / 2;
        double[][] T = MatrixTransformUtils.computeTrussRotationMatrix(c, c);
        double k = E * A / Math.sqrt(2);
        double[][] keLocal = new double[4][4];
        keLocal[0][0] = k; keLocal[0][2] = -k;
        keLocal[2][0] = -k; keLocal[2][2] = k;

        double[][] keGlobal = MatrixTransformUtils.transformToGlobal(keLocal, T);
        // Symmetric bar at 45°: global K should be symmetric
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(keGlobal[i][j], keGlobal[j][i], 1e-6);
            }
        }
        assertTrue(keGlobal[0][0] > 0);
        assertTrue(keGlobal[1][1] > 0);
    }

    @Test
    public void rollerSupportFixesVerticalOnly() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addMaterial(new Material(1, "Steel", E, 0.3));
        data.addElement(new TrussMember(1, 1, 2, 1, A));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));
        data.addPointLoad(new PointLoad(1, 2, 1000, 0));

        TrussSolver.Result result = new TrussSolver().solve(data);
        assertEquals(0, result.displacements[1], 1e-12);
        assertEquals(0, result.displacements[3], 1e-12);
        assertTrue(result.displacements[2] > 0);
    }

    @Test
    public void prattTrussSymmetryUnderCenterLoad() {
        // 1-bay Pratt truss (symmetric about x=2)
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 4, 0));
        data.addNode(new TrussNode(3, 0, 2));
        data.addNode(new TrussNode(4, 4, 2));
        data.addNode(new TrussNode(5, 2, 0));   // center bottom
        data.addNode(new TrussNode(6, 2, 2));   // center top

        data.addMaterial(new Material(1, "Steel", E, 0.3));
        double a = 0.01;
        // Bottom chord
        data.addElement(new TrussMember(1, 1, 5, 1, a));
        data.addElement(new TrussMember(2, 5, 2, 1, a));
        // Top chord
        data.addElement(new TrussMember(3, 3, 6, 1, a));
        data.addElement(new TrussMember(4, 6, 4, 1, a));
        // Verticals
        data.addElement(new TrussMember(5, 1, 3, 1, a));
        data.addElement(new TrussMember(6, 5, 6, 1, a));
        data.addElement(new TrussMember(7, 2, 4, 1, a));
        // Pratt diagonals (slant toward center from bottom)
        data.addElement(new TrussMember(8, 1, 6, 1, a));
        data.addElement(new TrussMember(9, 5, 3, 1, a));
        data.addElement(new TrussMember(10, 5, 4, 1, a));
        data.addElement(new TrussMember(11, 2, 6, 1, a));

        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));
        data.addPointLoad(new PointLoad(1, 5, 0, -10000));

        TrussSolver.Result result = new TrussSolver().solve(data);
        assertNotNull(result);

        // Symmetric left/right verticals (5 and 7) should share equal force magnitude
        double force5 = 0, force7 = 0;
        for (var er : result.elementResults) {
            if (er.elementId == 5) force5 = er.axialForce;
            if (er.elementId == 7) force7 = er.axialForce;
        }
        assertEquals(force5, force7, 1e-2, "Pratt truss vertical symmetry");
        assertTrue(force5 < 0, "End vertical should be in compression under downward load");
    }

    @Test
    public void stressSummarySeparatesTensionAndCompression() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addNode(new TrussNode(3, 0.5, 0.5));
        data.addMaterial(new Material(1, "Steel", E, 0.3));
        data.addElement(new TrussMember(1, 1, 3, 1, A));
        data.addElement(new TrussMember(2, 2, 3, 1, A));
        data.addElement(new TrussMember(3, 1, 2, 1, A));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));
        data.addPointLoad(new PointLoad(1, 3, 0, -1000));

        TrussSolver.Result result = new TrussSolver().solve(data);
        assertTrue(result.getMaxTensileStress() > 0, "Bottom chord should be in tension");
        assertTrue(result.getMaxCompressiveStress() < 0, "Diagonal members should be in compression");
        assertNotEquals(result.getMaxTensileStress(), result.getMaxCompressiveStress());
    }

    @Test
    public void missingSupportsFailsValidation() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, A));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().toLowerCase().contains("support"));
    }

    @Test
    public void floatingTrussFailsValidation() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 5, 0));
        data.addNode(new TrussNode(3, 6, 0));
        data.addElement(new TrussMember(1, 2, 3, 0, A));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().toLowerCase().contains("floating"));
    }

    @Test
    public void mechanismThrowsIllegalStateException() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, A));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        assertThrows(IllegalStateException.class, () -> new TrussSolver().solve(data));
    }

    @Test
    public void singularMatrixInsufficientConstraints() {
        // Two nodes, one member, only roller at one end — mechanism in UX
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addMaterial(new Material(1, "Steel", E, 0.3));
        data.addElement(new TrussMember(1, 1, 2, 1, A));
        data.addSupport(new Support(1, 1, Support.Type.ROLLER));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new TrussSolver().solve(data));
        assertTrue(ex.getMessage().toLowerCase().contains("unstable") ||
                   ex.getMessage().toLowerCase().contains("singular"));
    }

    @Test
    public void trussSplitPreservesMemberProperties() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 10, 0));
        data.addElement(new TrussMember(1, 1, 2, 7, 0.05));

        int newNodeId = data.splitElementAtPoint(1, 4.0, 0.0);
        assertEquals(3, newNodeId);
        assertEquals(2, data.getElements().size());
        assertTrue(data.findNodeById(newNodeId).get() instanceof TrussNode);
        for (Element e : data.getElements()) {
            assertTrue(e instanceof TrussMember);
            assertEquals(7, e.getMaterialId());
            assertEquals(0.05, e.getArea(), 1e-9);
        }
    }

    @Test
    public void nodeDeletionCascadesTrussMembers() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addNode(new TrussNode(3, 2, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, A));
        data.addElement(new TrussMember(2, 2, 3, 0, A));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        assertTrue(data.removeNodeById(2));
        assertEquals(0, data.getElements().size(), "Both members reference the deleted node");
        assertEquals(2, data.getNodes().size());
    }
}
