package com.treble.feasimulation.solver;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * Utility class for coordinate transformations in FEA.
 * Computes rotation matrices and transforms element stiffness matrices between local and global systems.
 */
public class MatrixTransformUtils {

    /**
     * Computes a 6x6 rotation matrix for a 2D beam element with 3 DOFs per node (ux, uy, rot).
     * The matrix T maps local displacements to global: u_global = T * u_local.
     * 
     * @param cos cos(theta) where theta is the angle between local x-axis and global x-axis.
     * @param sin sin(theta)
     * @return 6x6 rotation matrix
     */
    public static double[][] computeBeamRotationMatrix(double cos, double sin) {
        double[][] T = new double[6][6];
        // Node 1
        T[0][0] = cos; T[0][1] = -sin; T[0][2] = 0;
        T[1][0] = sin; T[1][1] = cos;  T[1][2] = 0;
        T[2][0] = 0;   T[2][1] = 0;    T[2][2] = 1;
        
        // Node 2
        T[3][3] = cos; T[3][4] = -sin; T[3][5] = 0;
        T[4][3] = sin; T[4][4] = cos;  T[4][5] = 0;
        T[5][3] = 0;   T[5][4] = 0;    T[5][5] = 1;
        
        return T;
    }

    /**
     * Computes a 4x4 rotation matrix for a 2D truss element with 2 DOFs per node (ux, uy).
     * The matrix T maps local displacements to global: u_global = T * u_local.
     *
     * @param cos cos(theta)
     * @param sin sin(theta)
     * @return 4x4 rotation matrix
     */
    public static double[][] computeTrussRotationMatrix(double cos, double sin) {
        double[][] T = new double[4][4];
        // Node 1
        T[0][0] = cos; T[0][1] = -sin;
        T[1][0] = sin; T[1][1] = cos;
        
        // Node 2
        T[2][2] = cos; T[2][3] = -sin;
        T[3][2] = sin; T[3][3] = cos;
        
        return T;
    }

    /**
     * Transforms a local stiffness matrix to global coordinates: K_global = T * K_local * T^T.
     * This assumes T maps local-to-global (u_global = T * u_local).
     * 
     * @param kLocal element stiffness matrix in local coordinates
     * @param T rotation matrix (local to global)
     * @return element stiffness matrix in global coordinates
     */
    public static double[][] transformToGlobal(double[][] kLocal, double[][] T) {
        int n = kLocal.length;
        if (T.length != n || T[0].length != n) {
            throw new IllegalArgumentException("Matrix dimensions must match");
        }

        DMatrixRMaj matKLocal = new DMatrixRMaj(kLocal);
        DMatrixRMaj matT = new DMatrixRMaj(T);
        DMatrixRMaj temp = new DMatrixRMaj(n, n);
        DMatrixRMaj matKGlobal = new DMatrixRMaj(n, n);

        // K_global = T * K_local * T^T
        // CommonOps_DDRM.multTransB(A, B, C) -> C = A * B^T
        CommonOps_DDRM.multTransB(matKLocal, matT, temp); // temp = K_local * T^T
        CommonOps_DDRM.mult(matT, temp, matKGlobal);    // K_global = T * temp

        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = matKGlobal.get(i, j);
            }
        }
        return result;
    }
}
