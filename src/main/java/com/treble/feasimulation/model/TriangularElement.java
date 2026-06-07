package com.treble.feasimulation.model;

import java.util.Arrays;

/**
 * A 3-node triangular element for 2D continuum problems.
 */
public class TriangularElement extends StructuralElement {
    private final Node[] nodes;
    private final double thickness;
    private double calculatedArea;
    private double[][] shapeFunctions; // Placeholder for shape function constants
    private double[][] stiffnessMatrix; // Plane-stress stiffness matrix

    public TriangularElement(int id, Node n1, Node n2, Node n3, int materialId, double thickness) {
        super(id, new int[]{n1.getId(), n2.getId(), n3.getId()}, materialId, 0); // Area to be calculated
        this.nodes = new Node[]{n1, n2, n3};
        this.thickness = thickness;
        this.calculatedArea = calculateArea();
    }

    private double calculateArea() {
        // Area = 0.5 * |x1(y2 - y3) + x2(y3 - y1) + x3(y1 - y2)|
        double x1 = nodes[0].getX();
        double y1 = nodes[0].getY();
        double x2 = nodes[1].getX();
        double y2 = nodes[1].getY();
        double x3 = nodes[2].getX();
        double y3 = nodes[2].getY();
        return 0.5 * Math.abs(x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
    }

    @Override
    public double getArea() {
        return calculatedArea;
    }

    public double getThickness() {
        return thickness;
    }

    public Node[] getNodes() {
        return nodes.clone();
    }

    public double[][] getShapeFunctions() {
        return shapeFunctions;
    }

    public double[][] getStiffnessMatrix() {
        return stiffnessMatrix;
    }

    /**
     * To be implemented later: computes the plane-stress stiffness matrix.
     */
    public void computeStiffnessMatrix() {
        // TODO: Implement plane-stress stiffness matrix calculation
    }

    @Override
    public Node.DOF[] getActiveDOFs() {
        return new Node.DOF[]{Node.DOF.UX, Node.DOF.UY};
    }

    @Override
    public String toString() {
        return "TriangularElement{" + getId() + ": nodes=" + Arrays.toString(getNodeIds()) + ", mat=" + getMaterialId() + ", area=" + calculatedArea + "}";
    }
}
