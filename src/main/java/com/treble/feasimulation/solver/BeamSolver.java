package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic Euler-Bernoulli beam solver (2 DOF per node: vertical displacement and rotation).
 * - Uses consistent 2-node beam element (v, theta DOFs) with EI/L^3 stiffness.
 * - Applies point loads (vertical component -> transverse DOF, moment -> rotational DOF).
 * - Supports are applied via a large penalty on restrained DOFs.
 *
 * This is a minimal educational solver (no axial action, no geometric transforms for angled beams).
 */
public class BeamSolver {
    private static final double PENALTY = 1e20;
    private static final double DEFAULT_E = 2.0e11; // Pa, fallback

    public static class ElementResult {
        public final int elementId;
        public final double endMomentStart; // internal moment at start node (positive sign convention)
        public final double endMomentEnd;

        public ElementResult(int elementId, double endMomentStart, double endMomentEnd) {
            this.elementId = elementId;
            this.endMomentStart = endMomentStart;
            this.endMomentEnd = endMomentEnd;
        }
    }

    public static class Result {
        // global DOFs arranged [v0, theta0, v1, theta1, ...]
        public final double[] displacements;
        public final List<ElementResult> elementResults;

        public Result(double[] displacements, List<ElementResult> elementResults) {
            this.displacements = displacements;
            this.elementResults = elementResults;
        }
    }

    public Result solve(FEAData data) {
        List<Node> nodes = data.getNodes();
        List<BeamElement> elements = data.getElements();

        int nNodes = nodes.size();
        if (nNodes == 0) return new Result(new double[0], new ArrayList<>());

        // Map nodeId -> index
        Map<Integer, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) nodeIndex.put(nodes.get(i).getId(), i);

        int ndof = 2 * nNodes;
        double[][] K = new double[ndof][ndof];
        double[] F = new double[ndof];

        // Assemble elements
        for (BeamElement be : elements) {
            Integer si = nodeIndex.get(be.getNodeStartId());
            Integer ti = nodeIndex.get(be.getNodeEndId());
            if (si == null || ti == null) continue; // skip malformed
            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L <= 1e-12) continue;

            // material E lookup
            double E = DEFAULT_E;
            int mid = be.getMaterialId();
            if (mid != 0) {
                for (Material m : data.getMaterials()) {
                    if (m.getId() == mid) { E = m.getYoungsModulus(); break; }
                }
            }
            double I = be.getInertia();
            double EI = E * I;

            // local stiffness (v1,theta1,v2,theta2) -> [F1,M1,F2,M2]
            double coef = EI / (L * L * L);
            double[][] ke = new double[4][4];
            ke[0][0] = 12.0 * coef;
            ke[0][1] = 6.0 * L * coef;
            ke[0][2] = -12.0 * coef;
            ke[0][3] = 6.0 * L * coef;

            ke[1][0] = 6.0 * L * coef;
            ke[1][1] = 4.0 * L * L * coef;
            ke[1][2] = -6.0 * L * coef;
            ke[1][3] = 2.0 * L * L * coef;

            ke[2][0] = -12.0 * coef;
            ke[2][1] = -6.0 * L * coef;
            ke[2][2] = 12.0 * coef;
            ke[2][3] = -6.0 * L * coef;

            ke[3][0] = 6.0 * L * coef;
            ke[3][1] = 2.0 * L * L * coef;
            ke[3][2] = -6.0 * L * coef;
            ke[3][3] = 4.0 * L * L * coef;

            int[] dofMap = new int[]{2 * si, 2 * si + 1, 2 * ti, 2 * ti + 1};
            // assemble
            for (int a = 0; a < 4; a++) {
                for (int b = 0; b < 4; b++) {
                    K[dofMap[a]][dofMap[b]] += ke[a][b];
                }
            }
        }

        // apply point loads
        for (PointLoad pl : data.getPointLoads()) {
            Integer ni = nodeIndex.get(pl.getNodeId());
            if (ni == null) continue;
            double fx = pl.getFx();
            double fy = pl.getFy();
            double m = pl.getMoment();
            // for beam solver consider fy (vertical) to transverse DOF and moment to rotational DOF
            F[2 * ni] += fy;
            F[2 * ni + 1] += m;
            // axial fx ignored in this 2-DOF model
        }

        // apply supports (penalty)
        for (Support s : data.getSupports()) {
            Integer ni = nodeIndex.get(s.getNodeId());
            if (ni == null) continue;
            if (s.isRestrainY()) {
                int dof = 2 * ni;
                K[dof][dof] += PENALTY;
                F[dof] = 0.0;
            }
            if (s.isRestrainRotation()) {
                int dof = 2 * ni + 1;
                K[dof][dof] += PENALTY;
                F[dof] = 0.0;
            }
        }

        // solve Ku = F
        double[] u = solveLinearSystem(K, F);

        // compute element end moments
        List<ElementResult> elemResults = new ArrayList<>();
        for (BeamElement be : elements) {
            Integer si = nodeIndex.get(be.getNodeStartId());
            Integer ti = nodeIndex.get(be.getNodeEndId());
            if (si == null || ti == null) continue;
            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double L = Math.hypot(nt.getX() - ns.getX(), nt.getY() - ns.getY());
            if (L <= 1e-12) continue;
            double E = DEFAULT_E;
            int mid = be.getMaterialId();
            if (mid != 0) {
                for (Material m : data.getMaterials()) {
                    if (m.getId() == mid) { E = m.getYoungsModulus(); break; }
                }
            }
            double I = be.getInertia();
            double EI = E * I;
            double coef = EI / (L * L * L);
            double[][] ke = new double[4][4];
            ke[0][0] = 12.0 * coef;
            ke[0][1] = 6.0 * L * coef;
            ke[0][2] = -12.0 * coef;
            ke[0][3] = 6.0 * L * coef;

            ke[1][0] = 6.0 * L * coef;
            ke[1][1] = 4.0 * L * L * coef;
            ke[1][2] = -6.0 * L * coef;
            ke[1][3] = 2.0 * L * L * coef;

            ke[2][0] = -12.0 * coef;
            ke[2][1] = -6.0 * L * coef;
            ke[2][2] = 12.0 * coef;
            ke[2][3] = -6.0 * L * coef;

            ke[3][0] = 6.0 * L * coef;
            ke[3][1] = 2.0 * L * L * coef;
            ke[3][2] = -6.0 * L * coef;
            ke[3][3] = 4.0 * L * L * coef;

            int[] dofMap = new int[]{2 * si, 2 * si + 1, 2 * ti, 2 * ti + 1};
            double[] ue = new double[4];
            for (int a = 0; a < 4; a++) ue[a] = u[dofMap[a]];

            double[] fe = new double[4];
            for (int a = 0; a < 4; a++) {
                double sum = 0.0;
                for (int b = 0; b < 4; b++) sum += ke[a][b] * ue[b];
                fe[a] = sum;
            }
            // fe: [F1, M1, F2, M2]
            double M1 = fe[1];
            double M2 = fe[3];
            elemResults.add(new ElementResult(be.getId(), M1, M2));
        }

        return new Result(u, elemResults);
    }

    // Simple Gaussian elimination with partial pivoting
    private double[] solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        double[][] M = new double[n][n+1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = b[i];
        }

        for (int k = 0; k < n; k++) {
            // pivot
            int max = k;
            for (int i = k+1; i < n; i++) if (Math.abs(M[i][k]) > Math.abs(M[max][k])) max = i;
            if (Math.abs(M[max][k]) < 1e-16) {
                throw new IllegalStateException("Matrix is singular or ill-conditioned (pivot ~ 0)");
            }
            // swap
            double[] tmp = M[k]; M[k] = M[max]; M[max] = tmp;

            // normalize and eliminate
            for (int i = k+1; i < n; i++) {
                double factor = M[i][k] / M[k][k];
                if (factor == 0.0) continue;
                for (int j = k; j <= n; j++) M[i][j] -= factor * M[k][j];
            }
        }

        // back substitution
        double[] x = new double[n];
        for (int i = n-1; i >= 0; i--) {
            double s = M[i][n];
            double diag = M[i][i];
            if (Math.abs(diag) < 1e-16) {
                throw new IllegalStateException("Matrix is singular or ill-conditioned (diagonal ~ 0) during back substitution");
            }
            for (int j = i+1; j < n; j++) s -= M[i][j] * x[j];
            x[i] = s / diag;
        }
        return x;
    }
}
