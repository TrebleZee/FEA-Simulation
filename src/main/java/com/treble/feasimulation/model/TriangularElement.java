package com.treble.feasimulation.model;

import java.util.Arrays;

/**
 * A 3-node triangular element for 2D continuum problems.
 */
public class TriangularElement extends StructuralElement {
    private final Node[] nodes;
    private final double thickness;
    private double calculatedArea;
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

    public double[][] getStiffnessMatrix() {
        return stiffnessMatrix;
    }

    /**
     * Computes the plane-stress stiffness matrix.
     * Ke = [B]^T * [D] * [B] * Area * thickness
     * 
     * @param youngsModulus E
     * @param poissonRatio v
     */
    public void computeStiffnessMatrix(double youngsModulus, double poissonRatio) {
        double area = getArea();
        if (area <= 0) {
            stiffnessMatrix = new double[6][6];
            return;
        }

        // 1. D matrix for Plane Stress
        // D = (E / (1 - v^2)) * [1  v  0;
        //                        v  1  0;
        //                        0  0  (1-v)/2]
        double factor = youngsModulus / (1 - poissonRatio * poissonRatio);
        double[][] D = new double[3][3];
        D[0][0] = factor;
        D[0][1] = factor * poissonRatio;
        D[1][0] = factor * poissonRatio;
        D[1][1] = factor;
        D[2][2] = factor * (1 - poissonRatio) / 2.0;

        // 2. B matrix (Strain-Displacement Matrix)
        // B = (1 / 2A) * [b1  0  b2  0  b3  0;
        //                 0  c1  0  c2  0  c3;
        //                 c1 b1  c2 b2  c3 b3]
        double x1 = nodes[0].getX();
        double y1 = nodes[0].getY();
        double x2 = nodes[1].getX();
        double y2 = nodes[1].getY();
        double x3 = nodes[2].getX();
        double y3 = nodes[2].getY();

        double b1 = y2 - y3;
        double b2 = y3 - y1;
        double b3 = y1 - y2;

        double c1 = x3 - x2;
        double c2 = x1 - x3;
        double c3 = x2 - x1;

        double[][] B = new double[3][6];
        double inv2A = 1.0 / (2.0 * area);

        B[0][0] = b1 * inv2A; B[0][2] = b2 * inv2A; B[0][4] = b3 * inv2A;
        B[1][1] = c1 * inv2A; B[1][3] = c2 * inv2A; B[1][5] = c3 * inv2A;
        B[2][0] = c1 * inv2A; B[2][1] = b1 * inv2A;
        B[2][2] = c2 * inv2A; B[2][3] = b2 * inv2A;
        B[2][4] = c3 * inv2A; B[2][5] = b3 * inv2A;

        // 3. Ke = B^T * D * B * area * thickness
        // Ke = (B^T * (D * B)) * area * thickness
        
        // DB = D * B (3x6)
        double[][] DB = new double[3][6];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 3; k++) {
                    DB[i][j] += D[i][k] * B[k][j];
                }
            }
        }

        // Ke = B^T * DB (6x6)
        stiffnessMatrix = new double[6][6];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 3; k++) {
                    stiffnessMatrix[i][j] += B[k][i] * DB[k][j];
                }
                // Multiply by area and thickness
                stiffnessMatrix[i][j] *= (area * thickness);
            }
        }
    }

    /**
     * @param x X-coordinate to evaluate shape functions at.
     * @param y Y-coordinate to evaluate shape functions at.
     * @return Array of 3 shape function values [N1, N2, N3]
     */
    public double[] evaluateShapeFunctions(double x, double y) {
        double area = getArea();
        if (area <= 0) return new double[]{0, 0, 0};

        double x1 = nodes[0].getX();
        double y1 = nodes[0].getY();
        double x2 = nodes[1].getX();
        double y2 = nodes[1].getY();
        double x3 = nodes[2].getX();
        double y3 = nodes[2].getY();

        double inv2A = 1.0 / (2.0 * area);

        // N_i = (a_i + b_i*x + c_i*y) / (2A)
        // a1 = x2*y3 - x3*y2, b1 = y2 - y3, c1 = x3 - x2
        double a1 = x2 * y3 - x3 * y2;
        double b1 = y2 - y3;
        double c1 = x3 - x2;

        double a2 = x3 * y1 - x1 * y3;
        double b2 = y3 - y1;
        double c2 = x1 - x3;

        double a3 = x1 * y2 - x2 * y1;
        double b3 = y1 - y2;
        double c3 = x2 - x1;

        return new double[]{
            (a1 + b1 * x + c1 * y) * inv2A,
            (a2 + b2 * x + c2 * y) * inv2A,
            (a3 + b3 * x + c3 * y) * inv2A
        };
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
