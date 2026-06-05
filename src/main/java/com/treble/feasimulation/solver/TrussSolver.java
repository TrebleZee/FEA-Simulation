package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.*;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

import java.util.*;

/**
 * Solver specialized for truss structures.
 * Assembles the global stiffness matrix using 2 DOFs per node (UX, UY).
 */
public class TrussSolver {

    private static final double DEFAULT_E = 210e9;

    public static class ElementResult {
        public final int elementId;
        public final double axialForce; // Tension positive
        public final double axialStress;
        public final int nodeStartId;
        public final int nodeEndId;

        public ElementResult(int elementId, double axialForce, double axialStress, int nodeStartId, int nodeEndId) {
            this.elementId = elementId;
            this.axialForce = axialForce;
            this.axialStress = axialStress;
            this.nodeStartId = nodeStartId;
            this.nodeEndId = nodeEndId;
        }
    }

    public static class Result {
        public final double[] displacements; // [ux0, uy0, ux1, uy1, ...]
        public final List<ElementResult> elementResults;
        public final double maxDisplacement;
        public final double maxStress;
        public final double minStress;

        public Result(double[] displacements, List<ElementResult> elementResults, double maxDisplacement, double maxStress, double minStress) {
            this.displacements = displacements;
            this.elementResults = elementResults;
            this.maxDisplacement = maxDisplacement;
            this.maxStress = maxStress;
            this.minStress = minStress;
        }
    }

    public Result solve(FEAData data) {
        List<Node> nodes = data.getNodes();
        List<Element> elements = data.getElements();

        int nNodes = nodes.size();
        if (nNodes == 0) return new Result(new double[0], new ArrayList<>(), 0, 0, 0);

        Map<Integer, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) nodeIndex.put(nodes.get(i).getId(), i);

        int ndof = 2 * nNodes;
        DMatrixRMaj K = new DMatrixRMaj(ndof, ndof);
        DMatrixRMaj F = new DMatrixRMaj(ndof, 1);

        // Assembly
        for (Element e : elements) {
            if (!(e instanceof TrussElement te)) continue;

            Integer si = nodeIndex.get(te.getNodeStartId());
            Integer ti = nodeIndex.get(te.getNodeEndId());
            if (si == null || ti == null) continue;

            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L < 1e-12) continue;

            double E = resolveYoungsModulus(te, data);
            double A = te.getArea();
            double k = (E * A) / L;

            double c = dx / L;
            double s = dy / L;

            // Local stiffness matrix (4x4)
            double[][] ke_local = new double[4][4];
            ke_local[0][0] = k; ke_local[0][2] = -k;
            ke_local[2][0] = -k; ke_local[2][2] = k;

            // Transform to global using MatrixTransformUtils
            double[][] T = MatrixTransformUtils.computeTrussRotationMatrix(c, s);
            double[][] ke_glob = MatrixTransformUtils.transformToGlobal(ke_local, T);

            int[] dofs = {2 * si, 2 * si + 1, 2 * ti, 2 * ti + 1};
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    K.add(dofs[i], dofs[j], ke_glob[i][j]);
                }
            }
        }

        // Apply point loads
        for (PointLoad pl : data.getPointLoads()) {
            Integer idx = nodeIndex.get(pl.getNodeId());
            if (idx != null) {
                F.add(2 * idx, 0, pl.getFx());
                F.add(2 * idx + 1, 0, pl.getFy());
            }
        }

        // Apply boundary conditions
        boolean[] fixed = new boolean[ndof];
        for (Support s : data.getSupports()) {
            Integer idx = nodeIndex.get(s.getNodeId());
            if (idx == null) continue;

            if (s.getType() == Support.Type.FIXED || s.getType() == Support.Type.PINNED) {
                fixed[2 * idx] = true;
                fixed[2 * idx + 1] = true;
            } else if (s.getType() == Support.Type.ROLLER) {
                // Standard assumption: roller on horizontal floor fixes Y
                fixed[2 * idx + 1] = true;
            }
        }

        // Solve reduced system (similar to BeamSolver)
        int freeCount = 0;
        int[] freeMap = new int[ndof];
        Arrays.fill(freeMap, -1);
        for (int i = 0; i < ndof; i++) if (!fixed[i]) freeMap[i] = freeCount++;

        if (freeCount == 0) return new Result(new double[ndof], new ArrayList<>(), 0, 0, 0);

        DMatrixRMaj Ared = new DMatrixRMaj(freeCount, freeCount);
        DMatrixRMaj bred = new DMatrixRMaj(freeCount, 1);
        for (int i = 0; i < ndof; i++) {
            if (freeMap[i] < 0) continue;
            bred.set(freeMap[i], 0, F.get(i, 0));
            for (int j = 0; j < ndof; j++) {
                if (freeMap[j] < 0) continue;
                Ared.set(freeMap[i], freeMap[j], K.get(i, j));
            }
        }

        DMatrixRMaj xred = new DMatrixRMaj(freeCount, 1);
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.linear(freeCount);
        if (!solver.setA(Ared)) {
            throw new RuntimeException("Singular stiffness matrix (structure unstable)");
        }
        solver.solve(bred, xred);

        double[] fullU = new double[ndof];
        double maxDispSq = 0;
        for (int i = 0; i < ndof; i++) {
            if (freeMap[i] >= 0) fullU[i] = xred.get(freeMap[i], 0);
        }
        for (int i = 0; i < nNodes; i++) {
            double ux = fullU[2 * i];
            double uy = fullU[2 * i + 1];
            maxDispSq = Math.max(maxDispSq, ux * ux + uy * uy);
        }

        // Compute member forces and stresses
        List<ElementResult> elementResults = new ArrayList<>();
        double maxStress = Double.NEGATIVE_INFINITY;
        double minStress = Double.POSITIVE_INFINITY;

        for (Element e : elements) {
            if (!(e instanceof TrussElement te)) continue;

            Integer si = nodeIndex.get(te.getNodeStartId());
            Integer ti = nodeIndex.get(te.getNodeEndId());
            if (si == null || ti == null) continue;

            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L < 1e-12) continue;

            double c = dx / L;
            double s = dy / L;

            double u1x = fullU[2 * si];
            double u1y = fullU[2 * si + 1];
            double u2x = fullU[2 * ti];
            double u2y = fullU[2 * ti + 1];

            // Member elongation: u_local_2 - u_local_1
            // u_local = u_global_x * cos + u_global_y * sin
            double deltaL = (u2x - u1x) * c + (u2y - u1y) * s;
            
            double E = resolveYoungsModulus(te, data);
            double A = te.getArea();
            double force = (E * A / L) * deltaL;
            double stress = (A > 0) ? force / A : 0.0;

            maxStress = Math.max(maxStress, stress);
            minStress = Math.min(minStress, stress);

            elementResults.add(new ElementResult(te.getId(), force, stress, te.getNodeStartId(), te.getNodeEndId()));
        }

        if (elementResults.isEmpty()) {
            maxStress = 0;
            minStress = 0;
        }

        return new Result(fullU, elementResults, Math.sqrt(maxDispSq), maxStress, minStress);
    }

    private double resolveYoungsModulus(Element element, FEAData data) {
        int materialId = element.getMaterialId();
        for (Material m : data.getMaterials()) {
            if (m.getId() == materialId) {
                return m.getYoungsModulus();
            }
        }
        return DEFAULT_E;
    }
}
