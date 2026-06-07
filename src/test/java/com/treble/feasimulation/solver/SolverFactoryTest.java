package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SolverFactoryTest {

    @Test
    public void testGetSolverWithTrussOnly() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0, 0));
        data.addNode(new Node(2, 1, 0));
        data.addElement(new TrussElement(1, 1, 2, 1, 0.01));
        
        FEASolver solver = SolverFactory.getSolver(data);
        assertTrue(solver instanceof TrussSolver, "Expected TrussSolver for pure truss model");
    }

    @Test
    public void testGetSolverWithBeam() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0, 0));
        data.addNode(new Node(2, 1, 0));
        data.addElement(new BeamElement(1, 1, 2, 1, 0.01, 1e-6));
        
        FEASolver solver = SolverFactory.getSolver(data);
        assertTrue(solver instanceof BeamSolver, "Expected BeamSolver for model with beam elements");
    }

    @Test
    public void testGetSolverWithTriangular() {
        FEAData data = new FEAData();
        Node n1 = new Node(1, 0, 0);
        Node n2 = new Node(2, 1, 0);
        Node n3 = new Node(3, 0, 1);
        data.addNode(n1);
        data.addNode(n2);
        data.addNode(n3);
        data.addElement(new TriangularElement(1, n1, n2, n3, 1, 0.01));
        
        FEASolver solver = SolverFactory.getSolver(data);
        assertTrue(solver instanceof PlaneStressSolver, "Expected PlaneStressSolver for model with triangular elements");
    }

    @Test
    public void testGetSolverMixedBeamTruss() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0, 0));
        data.addNode(new Node(2, 1, 0));
        data.addNode(new Node(3, 2, 0));
        data.addElement(new BeamElement(1, 1, 2, 1, 0.01, 1e-6));
        data.addElement(new TrussElement(2, 2, 3, 1, 0.01));
        
        FEASolver solver = SolverFactory.getSolver(data);
        assertTrue(solver instanceof BeamSolver, "Expected BeamSolver for mixed beam/truss model");
    }
}
