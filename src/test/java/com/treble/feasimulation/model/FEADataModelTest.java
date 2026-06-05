package com.treble.feasimulation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FEADataModelTest {

    @Test
    public void testAxialStiffnessComputation() {
        // E=200GPa, A=0.01m^2, L=2.0m -> EA/L = (2e11 * 0.01) / 2 = 1e9
        double E = 200e9;
        double A = 0.01;
        double L = 2.0;

        // Using a BeamElement to test the inherited method
        BeamElement be = new BeamElement(1, 1, 2, 0, A, 1.0e-6);
        assertEquals(1e9, be.computeAxialStiffness(E, L), 1e-9);

        // Using a TrussElement
        TrussElement te = new TrussElement(2, 1, 2, 0, A);
        assertEquals(1e9, te.computeAxialStiffness(E, L), 1e-9);
    }

    @Test
    public void testNodeCreationAndUpdate() {
        FEAData d = new FEAData();
        Node n = new Node(1, 10.0, 20.0);
        d.addNode(n);
        assertTrue(d.findNodeById(1).isPresent());
        Node updated = new Node(1, 15.0, 25.0);
        boolean ok = d.updateNode(updated);
        assertTrue(ok);
        Node r = d.findNodeById(1).get();
        assertEquals(15.0, r.getX(), 1e-9);
        assertEquals(25.0, r.getY(), 1e-9);
    }

    @Test
    public void testBeamCreationBetweenExistingNodes() {
        FEAData d = new FEAData();
        d.addNode(new Node(1, 0.0, 0.0));
        d.addNode(new Node(2, 1.0, 0.0));
        BeamElement be = new BeamElement(1, 1, 2, 0, 1.0, 1e-6);
        d.addElement(be);
        assertEquals(1, d.getElements().size());
        Element stored = d.getElements().get(0);
        assertEquals(1, stored.getNodeStartId());
        assertEquals(2, stored.getNodeEndId());
    }

    @Test
    public void testSplitElementAtPointCreatesTwoElements() {
        FEAData d = new FEAData();
        d.addNode(new Node(1, 0.0, 0.0));
        d.addNode(new Node(2, 10.0, 0.0));
        d.addElement(new BeamElement(1, 1, 2, 7, 2.5, 4.5));

        int newNodeId = d.splitElementAtPoint(1, 4.0, 0.0);

        assertEquals(3, newNodeId);
        assertEquals(3, d.getNodes().size());
        assertEquals(2, d.getElements().size());
        assertTrue(d.findNodeById(newNodeId).isPresent());
        assertEquals(4.0, d.findNodeById(newNodeId).get().getX(), 1e-9);
        assertTrue(d.getElements().stream().anyMatch(e ->
                e.getNodeStartId() == 1 && e.getNodeEndId() == newNodeId));
        assertTrue(d.getElements().stream().anyMatch(e ->
                e.getNodeStartId() == newNodeId && e.getNodeEndId() == 2));
        for (Element e : d.getElements()) {
            assertTrue(e instanceof BeamElement);
            BeamElement be = (BeamElement) e;
            assertEquals(7, be.getMaterialId());
            assertEquals(2.5, be.getArea(), 1e-9);
            assertEquals(4.5, be.getInertia(), 1e-9);
        }
    }

    @Test
    public void testDeletionCascade() {
        FEAData d = new FEAData();
        d.addNode(new Node(1, 0.0, 0.0));
        d.addNode(new Node(2, 1.0, 0.0));
        d.addElement(new BeamElement(1, 1, 2, 0, 1.0, 1e-6));
        d.addSupport(new Support(1, 1, Support.Type.FIXED));
        d.addPointLoad(new PointLoad(1, 2, 0.0, -1000.0));

        assertEquals(1, d.getElements().size());
        assertEquals(1, d.getSupports().size());
        assertEquals(1, d.getPointLoads().size());

        boolean removed = d.removeNodeById(1);
        assertTrue(removed);
        // element referencing node 1 should be removed
        assertEquals(0, d.getElements().size());
        // support attached to node1 removed
        assertEquals(0, d.getSupports().size());
        // pointload at node2 should remain
        assertEquals(1, d.getPointLoads().size());
    }

    @Test
    public void testSupportsAddRemove() {
        FEAData d = new FEAData();
        d.addNode(new Node(1, 0.0, 0.0));
        Support s1 = new Support(1, 1, Support.Type.FIXED);
        Support s2 = new Support(2, 1, Support.Type.ROLLER);
        d.addSupport(s1);
        d.addSupport(s2);
        assertEquals(2, d.getSupports().size());
        assertTrue(d.removeSupportById(1));
        assertEquals(1, d.getSupports().size());
    }
}
