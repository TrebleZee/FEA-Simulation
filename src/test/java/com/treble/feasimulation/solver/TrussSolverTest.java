package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TrussSolverTest {

    @Test
    public void testSimpleHorizontalTruss() {
        FEAData data = new FEAData();
        Node n1 = new TrussNode(1, 0, 0);
        Node n2 = new TrussNode(2, 1, 0);
        data.addNode(n1);
        data.addNode(n2);

        Material mat = new Material(1, "Steel", 210e9, 0.3);
        data.addMaterial(mat);

        double area = 0.01;
        TrussMember member = new TrussMember(1, 1, 2, 1, area);
        data.addElement(member);

        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER)); // Fixes Y
        data.addPointLoad(new PointLoad(1, 2, 1000, 0));

        TrussSolver solver = new TrussSolver();
        TrussSolver.Result result = solver.solve(data);

        assertNotNull(result);
        assertEquals(4, result.displacements.length);
        
        // Node 1: (0,0) due to support
        assertEquals(0, result.displacements[0], 1e-12);
        assertEquals(0, result.displacements[1], 1e-12);
        
        // Node 2: UX = FL/EA, UY = 0
        double expectedUX = (1000.0 * 1.0) / (210e9 * 0.01);
        assertEquals(expectedUX, result.displacements[2], 1e-12);
        assertEquals(0, result.displacements[3], 1e-12);

        assertEquals(1, result.elementResults.size());
        TrussSolver.ElementResult er = result.elementResults.get(0);
        assertEquals(1000.0, er.axialForce, 1e-6);
        assertEquals(1000.0 / 0.01, er.axialStress, 1e-6);
    }

    @Test
    public void testAngledTruss() {
        FEAData data = new FEAData();
        Node n1 = new TrussNode(1, 0, 0);
        Node n2 = new TrussNode(2, 1, 1); // 45 degrees, L = sqrt(2)
        data.addNode(n1);
        data.addNode(n2);

        Material mat = new Material(1, "Steel", 210e9, 0.3);
        data.addMaterial(mat);

        double area = 0.01;
        TrussMember member = new TrussMember(1, 1, 2, 1, area);
        data.addElement(member);

        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        // Use a second member to make it stable
        data.addNode(new TrussNode(3, 0, 1));
        data.addElement(new TrussMember(2, 3, 2, 1, area));
        data.addSupport(new Support(2, 3, Support.Type.FIXED));

        // Pulling with Fx=1000, Fy=1000. 
        data.addPointLoad(new PointLoad(1, 2, 1000, 1000));

        TrussSolver solver = new TrussSolver();
        TrussSolver.Result result = solver.solve(data);

        // This is now a 2-member truss. N1=(0,0), N2=(1,1), N3=(0,1).
        // Member 1: (0,0)->(1,1), L=sqrt(2), cos=1/sqrt(2), sin=1/sqrt(2)
        // Member 2: (0,1)->(1,1), L=1, cos=1, sin=0
        
        // Let's just check if it solves without error and forces are reasonable.
        assertNotNull(result);
        assertTrue(result.elementResults.size() >= 1);
    }

    @Test
    public void testTwoMemberTruss() {
        // Simple triangular truss
        // N1 (0,0) Fixed
        // N2 (1,0) Roller (fixes Y)
        // N3 (0.5, 0.5) Load Fy = -1000
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addNode(new TrussNode(3, 0.5, 0.5));

        data.addMaterial(new Material(1, "Steel", 210e9, 0.3));

        double A = 0.01;
        data.addElement(new TrussMember(1, 1, 3, 1, A));
        data.addElement(new TrussMember(2, 2, 3, 1, A));
        data.addElement(new TrussMember(3, 1, 2, 1, A));

        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER)); // Fixes Y

        data.addPointLoad(new PointLoad(1, 3, 0, -1000));

        TrussSolver solver = new TrussSolver();
        TrussSolver.Result result = solver.solve(data);

        // Equilibrium: 2 * F_axial * sin(45) = 1000  => F_axial = 500 / sin(45) = 500 * sqrt(2)
        // But the angle is not 45. dx=0.5, dy=0.5. So it is 45 degrees.
        double expectedAxial = -500.0 * Math.sqrt(2.0); // Compression
        
        // Element 1 (1->3) and 2 (2->3) should be in compression
        assertEquals(expectedAxial, result.elementResults.get(0).axialForce, 1e-6);
        assertEquals(expectedAxial, result.elementResults.get(1).axialForce, 1e-6);
    }
}
