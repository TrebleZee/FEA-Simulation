package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;

import org.ejml.data.DMatrixRMaj;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Euler-Bernoulli beam solver (3 DOF per node: ux, uy, rotation).
 * - Local element uses axial (EA/L) and bending (EI/L^3) blocks.
 * - Uses full local->global transformation so angled beams behave correctly.
 * - Boundary conditions are applied by DOF elimination (system reduction) instead of penalty.
 * - Linear solve uses EJML for improved robustness.
 *
 * Limitations: small-strain linear elasticity, Euler-Bernoulli bending, no shear deformation modelled.
 */
public class BeamSolver {
    private static final double DEFAULT_E = 2.0e11; // Pa, fallback

    public static class BendingStressResult {
        public final int elementId;
        public final double extremeFiberDistance;
        public final double maxBendingMoment;
        public final double maxTensileStress;
        public final double maxCompressiveStress;

        public BendingStressResult(int elementId, double extremeFiberDistance, double maxBendingMoment,
                                   double maxTensileStress, double maxCompressiveStress) {
            this.elementId = elementId;
            this.extremeFiberDistance = extremeFiberDistance;
            this.maxBendingMoment = maxBendingMoment;
            this.maxTensileStress = maxTensileStress;
            this.maxCompressiveStress = maxCompressiveStress;
        }
    }

    public static class ElementResult {
        public final int elementId;
        public final double endMomentStart;
        public final double endMomentEnd;
        public final BendingStressResult bendingStress;

        public ElementResult(int elementId, double endMomentStart, double endMomentEnd) {
            this(elementId, endMomentStart, endMomentEnd, null);
        }

        public ElementResult(int elementId, double endMomentStart, double endMomentEnd, BendingStressResult bendingStress) {
            this.elementId = elementId;
            this.endMomentStart = endMomentStart;
            this.endMomentEnd = endMomentEnd;
            this.bendingStress = bendingStress;
        }
    }

    public static class Result {
        // global DOFs arranged [ux0, uy0, theta0, ux1, uy1, theta1, ...]
        public final double[] displacements;
        public final List<ElementResult> elementResults;

        public Result(double[] displacements, List<ElementResult> elementResults) {
            this.displacements = displacements;
            this.elementResults = elementResults;
        }
    }

    /**
     * Compute the maximum bending stress for a beam element from its end moments.
     * The solver does not store section depth directly, so the outer-fiber distance is
     * estimated from area and inertia using a rectangular-equivalent section:
     * c = sqrt(3I/A).
     */
    public BendingStressResult computeBendingStress(BeamElement beam, double endMomentStart, double endMomentEnd) {
        double c = estimateExtremeFiberDistance(beam);
        double maxMoment = Math.max(Math.abs(endMomentStart), Math.abs(endMomentEnd));

        if (Double.isNaN(c) || beam.getInertia() <= 0.0) {
            return new BendingStressResult(beam.getId(), c, maxMoment, Double.NaN, Double.NaN);
        }

        double maxStress = maxMoment * c / beam.getInertia();
        return new BendingStressResult(beam.getId(), c, maxMoment, maxStress, -maxStress);
    }

    private double estimateExtremeFiberDistance(BeamElement beam) {
        double area = beam.getArea();
        double inertia = beam.getInertia();
        if (area > 0.0 && inertia > 0.0) {
            return Math.sqrt(3.0 * inertia / area);
        }
        return Double.NaN;
    }

    public Result solve(FEAData data) {
        List<Node> nodes = data.getNodes();
        List<BeamElement> elements = data.getElements();

        int nNodes = nodes.size();
        if (nNodes == 0) return new Result(new double[0], new ArrayList<>());

        Map<Integer, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) nodeIndex.put(nodes.get(i).getId(), i);

        int ndof = 3 * nNodes;
        double[][] K = new double[ndof][ndof];
        double[] F = new double[ndof];

        // assemble each element into global K using transformation
        for (BeamElement be : elements) {
            Integer si = nodeIndex.get(be.getNodeStartId());
            Integer ti = nodeIndex.get(be.getNodeEndId());
            if (si == null || ti == null) continue;
            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L <= 1e-12) continue;

            double E = DEFAULT_E;
            int mid = be.getMaterialId();
            if (mid != 0) {
                for (Material m : data.getMaterials()) {
                    if (m.getId() == mid) { E = m.getYoungsModulus(); break; }
                }
            }
            double A = be.getArea();
            double I = be.getInertia();

            // local element stiffness (6x6)
            double[][] ke = new double[6][6];
            // axial part (u1,u2) -> indices 0 and 3
            double EA = E * A;
            double kax = EA / L;
            ke[0][0] = kax; ke[0][3] = -kax; ke[3][0] = -kax; ke[3][3] = kax;

            // bending 4x4 (v1,theta1,v2,theta2) mapped to indices 1,2,4,5
            double coef = E * I / (L * L * L);
            double[][] kb = new double[4][4];
            kb[0][0] = 12.0 * coef; kb[0][1] = 6.0 * L * coef; kb[0][2] = -12.0 * coef; kb[0][3] = 6.0 * L * coef;
            kb[1][0] = 6.0 * L * coef; kb[1][1] = 4.0 * L * L * coef; kb[1][2] = -6.0 * L * coef; kb[1][3] = 2.0 * L * L * coef;
            kb[2][0] = -12.0 * coef; kb[2][1] = -6.0 * L * coef; kb[2][2] = 12.0 * coef; kb[2][3] = -6.0 * L * coef;
            kb[3][0] = 6.0 * L * coef; kb[3][1] = 2.0 * L * L * coef; kb[3][2] = -6.0 * L * coef; kb[3][3] = 4.0 * L * L * coef;
            int[] bIdx = new int[]{1,2,4,5};
            for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) ke[bIdx[a]][bIdx[b]] = kb[a][b];

            // transformation matrix L (6x6) local -> global
            double c = dx / L, s = dy / L;
            double[][] T = new double[6][6];
            // node 1
            T[0][0] = c; T[0][1] = -s; T[0][2] = 0;
            T[1][0] = s; T[1][1] = c;  T[1][2] = 0;
            T[2][0] = 0; T[2][1] = 0;  T[2][2] = 1; // rotation
            // node 2 (offset columns/rows)
            T[3][3] = c; T[3][4] = -s; T[3][5] = 0;
            T[4][3] = s; T[4][4] = c;  T[4][5] = 0;
            T[5][3] = 0; T[5][4] = 0;  T[5][5] = 1;

            // K_global_element = T * ke * T^T (since T maps local -> global: u_global = T * u_local)
            double[][] Kglob = new double[6][6];
            // temp = ke * T^T
            double[][] temp = new double[6][6];
            for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) {
                double sum = 0.0;
                for (int k = 0; k < 6; k++) sum += ke[i][k] * T[j][k]; // note T^T element (k,j) = T[j][k]
                temp[i][j] = sum;
            }
            for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) {
                double sum = 0.0;
                for (int k = 0; k < 6; k++) sum += T[i][k] * temp[k][j];
                Kglob[i][j] = sum;
            }

            int[] dofMap = new int[]{3*si, 3*si + 1, 3*si + 2, 3*ti, 3*ti + 1, 3*ti + 2};
            for (int a = 0; a < 6; a++) for (int b = 0; b < 6; b++) K[dofMap[a]][dofMap[b]] += Kglob[a][b];
        }

        // point loads
        for (PointLoad pl : data.getPointLoads()) {
            Integer ni = nodeIndex.get(pl.getNodeId());
            if (ni == null) continue;
            double fx = pl.getFx();
            double fy = pl.getFy();
            double m = pl.getMoment();
            F[3*ni] += fx;
            F[3*ni + 1] += fy;
            F[3*ni + 2] += m;
        }

        // identify constrained DOFs
        boolean[] constrained = new boolean[ndof];
        for (Support s : data.getSupports()) {
            Integer ni = nodeIndex.get(s.getNodeId());
            if (ni == null) continue;
            if (s.isRestrainX()) constrained[3*ni] = true;
            if (s.isRestrainY()) constrained[3*ni + 1] = true;
            if (s.isRestrainRotation()) constrained[3*ni + 2] = true;
        }

        // build reduced system (eliminate constrained DOFs)
        int freeCount = 0;
        int[] freeMap = new int[ndof];
        for (int i = 0; i < ndof; i++) {
            if (!constrained[i]) { freeMap[i] = freeCount++; } else { freeMap[i] = -1; }
        }
        if (freeCount == 0) throw new IllegalStateException("All DOFs constrained or no free DOFs to solve.");

        DMatrixRMaj Ared = new DMatrixRMaj(freeCount, freeCount);
        DMatrixRMaj bred = new DMatrixRMaj(freeCount, 1);
        for (int i = 0; i < ndof; i++) {
            if (freeMap[i] < 0) continue;
            for (int j = 0; j < ndof; j++) {
                if (freeMap[j] < 0) continue;
                Ared.set(freeMap[i], freeMap[j], K[i][j]);
            }
            bred.set(freeMap[i], 0, F[i]);
        }

        // solve reduced system using EJML
        DMatrixRMaj xred = new DMatrixRMaj(freeCount, 1);
        try {
            LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.lu(freeCount);
            solver.setA(Ared);
            solver.solve(bred, xred);
        } catch (Exception ex) {
            throw new IllegalStateException("Linear solver failed: " + ex.getMessage(), ex);
        }

        // verify residual to detect singular/ill-conditioned cases
        DMatrixRMaj resid = new DMatrixRMaj(freeCount, 1);
        CommonOps_DDRM.mult(Ared, xred, resid);
        // resid = resid - bred
        for (int i = 0; i < freeCount; i++) resid.set(i, 0, resid.get(i,0) - bred.get(i,0));
        double maxRes = 0.0; double maxB = 0.0;
        for (int i = 0; i < freeCount; i++) {
            double rv = Math.abs(resid.get(i,0)); if (rv > maxRes) maxRes = rv;
            double bv = Math.abs(bred.get(i,0)); if (bv > maxB) maxB = bv;
            double xv = xred.get(i,0); if (Double.isNaN(xv) || Double.isInfinite(xv)) throw new IllegalStateException("Linear solver produced invalid values (NaN/Inf)");
        }
        double tol = 1e-6 * Math.max(1.0, maxB);
        if (maxRes > tol) {
            throw new IllegalStateException("Linear system residual too large (matrix may be singular or ill-conditioned). maxRes=" + maxRes + " tol=" + tol);
        }

        // expand solution
        double[] u = new double[ndof];
        for (int i = 0; i < ndof; i++) {
            if (freeMap[i] >= 0) u[i] = xred.get(freeMap[i], 0);
            else u[i] = 0.0; // constrained DOF
        }

        // compute element end moments (use local coordinates)
        List<ElementResult> elemResults = new ArrayList<>();
        for (BeamElement be : elements) {
            Integer si = nodeIndex.get(be.getNodeStartId());
            Integer ti = nodeIndex.get(be.getNodeEndId());
            if (si == null || ti == null) continue;
            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L <= 1e-12) continue;

            double E = DEFAULT_E;
            int mid = be.getMaterialId();
            if (mid != 0) {
                for (Material m : data.getMaterials()) if (m.getId() == mid) { E = m.getYoungsModulus(); break; }
            }
            double A = be.getArea();
            double I = be.getInertia();

            // rebuild local ke (6x6) as above
            double[][] ke = new double[6][6];
            double EA = E * A; double kax = EA / L;
            ke[0][0] = kax; ke[0][3] = -kax; ke[3][0] = -kax; ke[3][3] = kax;
            double coef = E * I / (L * L * L);
            double[][] kb = new double[4][4];
            kb[0][0] = 12.0 * coef; kb[0][1] = 6.0 * L * coef; kb[0][2] = -12.0 * coef; kb[0][3] = 6.0 * L * coef;
            kb[1][0] = 6.0 * L * coef; kb[1][1] = 4.0 * L * L * coef; kb[1][2] = -6.0 * L * coef; kb[1][3] = 2.0 * L * L * coef;
            kb[2][0] = -12.0 * coef; kb[2][1] = -6.0 * L * coef; kb[2][2] = 12.0 * coef; kb[2][3] = -6.0 * L * coef;
            kb[3][0] = 6.0 * L * coef; kb[3][1] = 2.0 * L * L * coef; kb[3][2] = -6.0 * L * coef; kb[3][3] = 4.0 * L * L * coef;
            int[] bIdx = new int[]{1,2,4,5};
            for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) ke[bIdx[a]][bIdx[b]] = kb[a][b];

            // transformation matrix T (local -> global)
            double c = dx / L, s = dy / L;
            double[][] T = new double[6][6];
            T[0][0] = c; T[0][1] = -s; T[0][2] = 0; T[1][0] = s; T[1][1] = c; T[1][2] = 0; T[2][2] = 1;
            T[3][3] = c; T[3][4] = -s; T[3][5] = 0; T[4][3] = s; T[4][4] = c; T[4][5] = 0; T[5][5] = 1;

            int[] dofMap = new int[]{3*si, 3*si + 1, 3*si + 2, 3*ti, 3*ti + 1, 3*ti + 2};
            double[] uglob = new double[6];
            for (int i = 0; i < 6; i++) uglob[i] = u[dofMap[i]];

            // u_local = T^T * u_global (T orthonormal blocks)
            double[] ulocal = new double[6];
            for (int i = 0; i < 6; i++) {
                double sum = 0.0;
                for (int j = 0; j < 6; j++) sum += T[j][i] * uglob[j];
                ulocal[i] = sum;
            }

            // fe_local = ke * u_local
            double[] felocal = new double[6];
            for (int i = 0; i < 6; i++) {
                double sum = 0.0;
                for (int j = 0; j < 6; j++) sum += ke[i][j] * ulocal[j];
                felocal[i] = sum;
            }

            double M1 = felocal[2]; // rotation DOF at node 1 index 2
            double M2 = felocal[5]; // rotation DOF at node 2 index 5
            BendingStressResult stress = computeBendingStress(be, M1, M2);
            elemResults.add(new ElementResult(be.getId(), M1, M2, stress));
        }

        return new Result(u, elemResults);
    }
}
