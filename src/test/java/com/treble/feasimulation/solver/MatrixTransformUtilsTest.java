package com.treble.feasimulation.solver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MatrixTransformUtilsTest {

    @Test
    public void testBeamRotationMatrixAt90Degrees() {
        // 90 degrees: cos=0, sin=1
        double cos = 0.0;
        double sin = 1.0;
        double[][] T = MatrixTransformUtils.computeBeamRotationMatrix(cos, sin);

        // u_global = T * u_local
        // For a vertical beam (90 deg), local x is global y, local y is global -x
        // u_local = [1, 0, 0, 0, 0, 0] (local axial displacement)
        // u_global should be [0, 1, 0, 0, 0, 0] (global y displacement)
        
        assertEquals(0.0, T[0][0], 1e-9);
        assertEquals(-1.0, T[0][1], 1e-9);
        assertEquals(1.0, T[1][0], 1e-9);
        assertEquals(0.0, T[1][1], 1e-9);
        assertEquals(1.0, T[2][2], 1e-9);
    }

    @Test
    public void testTrussRotationMatrixAt45Degrees() {
        // 45 degrees: cos=sin=sqrt(2)/2
        double val = Math.sqrt(2.0) / 2.0;
        double[][] T = MatrixTransformUtils.computeTrussRotationMatrix(val, val);

        // Local displacement [1, 0] at node 1
        // Global displacement should be [cos, sin] = [val, val]
        double ux_glob = T[0][0] * 1.0 + T[0][1] * 0.0;
        double uy_glob = T[1][0] * 1.0 + T[1][1] * 0.0;

        assertEquals(val, ux_glob, 1e-9);
        assertEquals(val, uy_glob, 1e-9);
    }

    @Test
    public void testTransformStiffness() {
        // Simple 2x2 case for testing the multiplication logic
        double[][] kLocal = {
            {100, 0},
            {0, 50}
        };
        // 90 degree rotation for 2x2
        double[][] T = {
            {0, -1},
            {1, 0}
        };
        // K_global = T * K_local * T^T
        // T * K_local = [[0, -1], [1, 0]] * [[100, 0], [0, 50]] = [[0, -50], [100, 0]]
        // [[0, -50], [100, 0]] * [[0, 1], [-1, 0]] = [[50, 0], [0, 100]]
        
        double[][] kGlobal = MatrixTransformUtils.transformToGlobal(kLocal, T);
        
        assertEquals(50.0, kGlobal[0][0], 1e-9);
        assertEquals(0.0, kGlobal[0][1], 1e-9);
        assertEquals(0.0, kGlobal[1][0], 1e-9);
        assertEquals(100.0, kGlobal[1][1], 1e-9);
    }
}
