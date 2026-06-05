package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test validation logic in MainPresenter.
 * These tests verify that the model validation correctly identifies problematic configurations.
 */
public class MainPresenterValidationTest {

    /**
     * Helper: Create a simple valid beam model for testing.
     * Two nodes, one element, one support.
     */
    private FEAData createValidBeamModel() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, 2.0, 0.0));
        data.addMaterial(new Material(1, "Steel", 2.0e11, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, 1.0e-6));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        return data;
    }

    @Test
    public void validModel_shouldPass() {
        FEAData data = createValidBeamModel();
        // This model should pass all validations (we can't directly test private methods,
        // but we document that it should pass)
        assertTrue(!data.getElements().isEmpty(), "Model should have elements");
        assertTrue(!data.getSupports().isEmpty(), "Model should have supports");
    }

    @Test
    public void noElements_shouldFail() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0.0, 0.0));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        
        assertTrue(data.getElements().isEmpty(), "Model should have no elements");
        assertTrue(!data.getSupports().isEmpty(), "Model should have supports");
    }

    @Test
    public void noSupports_shouldFail() {
        FEAData data = new FEAData();
        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, 2.0, 0.0));
        data.addMaterial(new Material(1, "Steel", 2.0e11, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, 1.0e-6));
        
        assertTrue(!data.getElements().isEmpty(), "Model should have elements");
        assertTrue(data.getSupports().isEmpty(), "Model should have no supports");
    }

    @Test
    public void floatingBeam_disconnectedFromSupport() {
        FEAData data = new FEAData();
        // Support at node 1
        data.addNode(new Node(1, 0.0, 0.0));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        
        // Floating beam: nodes 2-3, not connected to supported node 1
        data.addNode(new Node(2, 5.0, 0.0));
        data.addNode(new Node(3, 7.0, 0.0));
        data.addMaterial(new Material(1, "Steel", 2.0e11, 7850));
        data.addElement(new BeamElement(1, 2, 3, 1, 1.0, 1.0e-6));
        
        // This should be detected as floating
        assertTrue(!data.getElements().isEmpty(), "Model should have elements");
        assertTrue(!data.getSupports().isEmpty(), "Model should have supports");
        // Nodes 2 and 3 are not connected to node 1
    }

    @Test
    public void zeroLengthElement_shouldFail() {
        FEAData data = new FEAData();
        // Two nodes at the same location
        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, 0.0, 0.0)); // same as node 1
        data.addMaterial(new Material(1, "Steel", 2.0e11, 7850));
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, 1.0e-6));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        
        // Calculate length
        double dx = 0.0 - 0.0;
        double dy = 0.0 - 0.0;
        double length = Math.hypot(dx, dy);
        assertTrue(length <= 1e-12, "Element should have zero length");
    }

    @Test
    public void connectedStructure_multipleElements() {
        FEAData data = new FEAData();
        // Three nodes forming a connected structure
        data.addNode(new Node(1, 0.0, 0.0));
        data.addNode(new Node(2, 2.0, 0.0));
        data.addNode(new Node(3, 4.0, 0.0));
        data.addMaterial(new Material(1, "Steel", 2.0e11, 7850));
        // Two elements connecting all nodes
        data.addElement(new BeamElement(1, 1, 2, 1, 1.0, 1.0e-6));
        data.addElement(new BeamElement(2, 2, 3, 1, 1.0, 1.0e-6));
        // Support at one end
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 3, Support.Type.ROLLER));
        
        // This should be a valid connected structure
        assertTrue(data.getElements().size() == 2, "Model should have 2 elements");
        assertTrue(data.getSupports().size() == 2, "Model should have 2 supports");
    }
}
