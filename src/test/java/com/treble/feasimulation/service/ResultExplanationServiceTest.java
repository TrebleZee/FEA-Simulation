package com.treble.feasimulation.service;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;
import com.treble.feasimulation.model.TrussMember;
import com.treble.feasimulation.solver.BeamSolver;
import com.treble.feasimulation.solver.TrussSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultExplanationServiceTest {

    @Test
    public void explainsLikelyFailureWhenStressExceedsYield() {
        FEAData data = createCantileverWithYieldStress(1.0e6);
        BeamSolver.Result result = new BeamSolver().solve(data);

        String explanation = new ResultExplanationService().explain(result, data).toLowerCase();

        assertTrue(explanation.contains("likely to fail"));
        assertTrue(explanation.contains("yield"));
        assertTrue(explanation.contains("sf="));
    }

    @Test
    public void explainsLikelySafeWhenStressStaysBelowYield() {
        FEAData data = createCantileverWithYieldStress(1.0e9);
        BeamSolver.Result result = new BeamSolver().solve(data);

        String explanation = new ResultExplanationService().explain(result, data).toLowerCase();

        assertTrue(explanation.contains("unlikely to fail"));
        assertTrue(explanation.contains("sf="));
        assertTrue(explanation.contains("yield"));
    }

    @Test
    public void explainsTrussCriticalMemberAndFailure() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0, 0));
        data.addNode(new Node(2, 2, 0));
        data.addMaterial(new Material(1, "Steel", 210e9, 7850, 250e6));
        data.addElement(new TrussMember(1, 1, 2, 1, 0.01)); // A=0.01 m^2
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER)); // Fixes Y
        data.addPointLoad(new PointLoad(1, 2, 1e6, 0)); // 1 MN tension

        TrussSolver solver = new TrussSolver();
        TrussSolver.Result result = solver.solve(data);

        String explanation = new ResultExplanationService().explain(result, data).toLowerCase();

        assertTrue(explanation.contains("critical member"), "Should mention critical member");
        assertTrue(explanation.contains("tension"), "Should mention tension");
        assertTrue(explanation.contains("element 1"), "Should mention element 1");
        assertTrue(explanation.contains("yield"), "Should mention yield stress");
        assertTrue(explanation.contains("unlikely to fail"), "Should be safe");
    }

    private FEAData createCantileverWithYieldStress(double yieldStress) {
        FEAData data = new FEAData();
        double L = 2.0;
        double P = 1000.0;
        double E = 2.0e11;
        double I = 1.0e-6;

        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, L, 0.0));
        data.addMaterial(new Material(1, "Steel", E, 7850, yieldStress));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, I));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addPointLoad(new PointLoad(1, 2, 0.0, -P));
        return data;
    }
}
