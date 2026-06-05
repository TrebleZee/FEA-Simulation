package com.treble.feasimulation.service;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;
import com.treble.feasimulation.solver.BeamSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultExplanationServiceTest {

    @Test
    public void explainsLikelyFailureWhenStressExceedsYield() {
        FEAData data = createCantileverWithYieldStress(1.0e6);
        BeamSolver.Result result = new BeamSolver().solve(data);

        String explanation = new ResultExplanationService().explain(result, data).toLowerCase();

        assertTrue(explanation.contains("likely to fail by yielding"));
        assertTrue(explanation.contains("yield stress"));
        assertTrue(explanation.contains("utilization"));
    }

    @Test
    public void explainsLikelySafeWhenStressStaysBelowYield() {
        FEAData data = createCantileverWithYieldStress(1.0e9);
        BeamSolver.Result result = new BeamSolver().solve(data);

        String explanation = new ResultExplanationService().explain(result, data).toLowerCase();

        assertTrue(explanation.contains("unlikely to fail under this load"));
        assertTrue(explanation.contains("safety factor"));
        assertTrue(explanation.contains("yield stress"));
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
