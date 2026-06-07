package com.treble.feasimulation.service;

import com.treble.feasimulation.model.*;
import com.treble.feasimulation.solver.PlaneStressResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UnitConverterTest {

    @Test
    public void geometryScaling_convertsPolygonVerticesToMeters() {
        FEAData model = new FEAData();
        double[][] coords = new double[][]{
                {0, 0}, {1000, 0}, {1000, 1000}, {0, 1000}
        };
        model.addPolygonRegion(PolygonRegion.fromCoordinates(1, coords, 1));

        UnitSettings units = new UnitSettings(0.001, 1.0); // 1 unit = 1 mm
        FEAData si = UnitConverter.toSI(model, units);

        PolygonRegion pr = si.getPolygonRegions().get(0);
        assertEquals(1.0, pr.getX(1), 1e-12);
        assertEquals(0.0, pr.getY(1), 1e-12);
        assertEquals(1.0, pr.getX(2), 1e-12);
        assertEquals(1.0, pr.getY(2), 1e-12);
    }

    @Test
    public void planeStressResultToDisplay_scalesNodesAndDisplacements() {
        // Build a minimal PlaneStressResult in SI
        Node n1 = new Node(1, 0.0, 0.0);
        Node n2 = new Node(2, 1.0, 0.0);
        Node n3 = new Node(3, 0.0, 1.0);
        com.treble.feasimulation.model.TriangularElement te =
                new com.treble.feasimulation.model.TriangularElement(10, n1, n2, n3, 1, 0.01);

        double[] uSI = new double[]{0.001, 0.0, 0.0, 0.0, 0.001, 0.0}; // meters at nodes 1,2,3
        List<PlaneStressResult.ElementStress> stresses = new ArrayList<>();
        stresses.add(new PlaneStressResult.ElementStress(10, 1e6, 0.0, 0.0));
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 0); map.put(2, 1); map.put(3, 2);
        List<com.treble.feasimulation.model.TriangularElement> elems = new ArrayList<>();
        elems.add(te);
        PlaneStressResult si = new PlaneStressResult(uSI, stresses, map, elems);

        UnitSettings units = new UnitSettings(0.001, 1.0); // 1 unit = 1 mm
        PlaneStressResult disp = UnitConverter.planeStressResultToDisplay(si, units);

        // Displacements converted to model units: 0.001 m -> 1.0 model units (mm)
        assertEquals(1.0, disp.getDisplacements()[0], 1e-12); // node 1 ux
        assertEquals(1.0, disp.getDisplacements()[4], 1e-12); // node 3 ux

        // Element node coordinates converted back to model units
        com.treble.feasimulation.model.TriangularElement teD = disp.getElements().get(0);
        Node[] ns = teD.getNodes();
        assertEquals(1000.0, ns[1].getX(), 1e-9);
        assertEquals(1000.0, ns[2].getY(), 1e-9);
    }
}
