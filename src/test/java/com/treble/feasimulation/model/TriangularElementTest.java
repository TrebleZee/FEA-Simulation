package com.treble.feasimulation.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TriangularElementTest {

    @Test
    public void testAreaCalculation() {
        Node n1 = new Node(1, 0, 0);
        Node n2 = new Node(2, 1, 0);
        Node n3 = new Node(3, 0, 1);
        TriangularElement element = new TriangularElement(1, n1, n2, n3, 1, 0.1);
        
        assertEquals(0.5, element.getArea(), 1e-9);
    }

    @Test
    public void testStiffnessMatrix() {
        Node n1 = new Node(1, 0, 0);
        Node n2 = new Node(2, 1, 0);
        Node n3 = new Node(3, 0, 1);
        double thickness = 1.0;
        double E = 200e9; // 200 GPa
        double v = 0.3;
        
        TriangularElement element = new TriangularElement(1, n1, n2, n3, 1, thickness);
        element.computeStiffnessMatrix(E, v);
        
        double[][] K = element.getStiffnessMatrix();
        assertNotNull(K);
        assertEquals(6, K.length);
        assertEquals(6, K[0].length);
        
        // Symmetry check
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                assertEquals(K[i][j], K[j][i], 1e-3, "Stiffness matrix should be symmetric");
            }
        }
    }

    @Test
    public void testShapeFunctions() {
        Node n1 = new Node(1, 0, 0);
        Node n2 = new Node(2, 1, 0);
        Node n3 = new Node(3, 0, 1);
        TriangularElement element = new TriangularElement(1, n1, n2, n3, 1, 1.0);
        
        // At nodes
        assertArrayEquals(new double[]{1, 0, 0}, element.evaluateShapeFunctions(0, 0), 1e-9);
        assertArrayEquals(new double[]{0, 1, 0}, element.evaluateShapeFunctions(1, 0), 1e-9);
        assertArrayEquals(new double[]{0, 0, 1}, element.evaluateShapeFunctions(0, 1), 1e-9);
        
        // At centroid
        double[] N = element.evaluateShapeFunctions(1.0/3.0, 1.0/3.0);
        assertEquals(1.0/3.0, N[0], 1e-9);
        assertEquals(1.0/3.0, N[1], 1e-9);
        assertEquals(1.0/3.0, N[2], 1e-9);
        
        // Sum should be 1
        double x = 0.25, y = 0.4;
        N = element.evaluateShapeFunctions(x, y);
        assertEquals(1.0, N[0] + N[1] + N[2], 1e-9);
    }
}
