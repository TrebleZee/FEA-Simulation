package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.Element;
import com.treble.feasimulation.model.TrussElement;
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
 * Euler-Bernoulli beam solver for 2D frame analysis (Phase 1).
 *
 * PHASE 1 CAPABILITIES (Current Implementation):
 * ============================================
 * - 3 DOF per node: axial displacement (ux), vertical displacement (uy), rotation (θ)
 * - Local element stiffness includes both:
 *   • Axial stiffness: EA/L (can model axial force/compression)
 *   • Bending stiffness: 12EI/L³ (vertical loads, moments)
 * - Full local-to-global coordinate transformation (T matrix, 6x6)
 *   • Handles angled beams and arbitrary orientations correctly
 *   • Rotation invariant: element orientation doesn't affect results
 * - Boundary conditions via DOF elimination (system reduction), not penalty method
 * - Linear solve using EJML with numerical robustness checks
 * - Material properties per element (E, A, I resolved at solve time)
 * - Point loads and moment loads at nodes
 * - Support types: FIXED (all DOF), PINNED (ux,uy), ROLLER (specific direction)
 *
 * CURRENT LIMITATIONS:
 * ===================
 * - Small-strain linear elasticity only (no large deformations or nonlinearity)
 * - Euler-Bernoulli beam theory: no shear deformation (valid for L/h > ~10)
 * - Horizontal loads treated as distributed; no shear forces returned
 * - No distributed loads (only point loads and moments at nodes)
 * - No temperature or pre-stress effects
 * - No dynamic analysis (modal, transient)
 * - No material nonlinearity (plasticity, damage)
 * - Assumes fixed boundary conditions (no sliding supports, symmetry)
 *
 * PHASE 2 TODO:
 * =============
 * - Distributed load support (line loads along elements)
 * - Thermal loads and temperature field
 * - Timoshenko beam theory (include shear deformation effects)
 * - Partial/sliding supports (inclined rollers)
 * - Curved elements (parabolic or spline-based)
 * - Multi-material sections (composite beams)
 * - Nonlinear solver for large deformations
 * - Output: shear force diagrams, deflection plots
 * - Export to standard FEM formats
 * - Performance: sparse solver for large models
 */
public class BeamSolver {
    private static final double DEFAULT_E = 2.0e11; // Pa, fallback

    /**
     * Helper method to resolve a beam's Young's modulus from its assigned material.
     * 
     * PHASE 1: Current implementation
     * ================================
     * - Per-element material assignment (each beam can have different E)
     * - Linear material model: stress = E · strain
     * - Isotropic material (same E in all directions)
     * - Material lookup at solve-time (not pre-compiled, flexible for interactive updates)
     * - Fallback to DEFAULT_E if material ID is 0 or not found
     * 
     * PHASE 2 TODO:
     * =============
     * - Temperature-dependent E(T) for thermal analysis
     * - Anisotropic materials (composite: Ex ≠ Ey)
     * - Material nonlinearity (elastoplastic, strain-hardening)
     * - Pre-compute material lookup during init (performance optimization)
     * - Material orientation for composites (fiber angle tracking)
     * 
     * If the material ID is 0 or not found, falls back to DEFAULT_E.
     */
    private double resolveYoungsModulus(Element element, FEAData data) {
        int materialId = element.getMaterialId();
        if (materialId == 0) {
            return DEFAULT_E;
        }
        for (Material m : data.getMaterials()) {
            if (m.getId() == materialId) {
                return m.getYoungsModulus();
            }
        }
        // Material ID not found; log warning and use fallback
        System.err.println("Warning: Material ID " + materialId + " not found for element " + element.getId() + "; using DEFAULT_E");
        return DEFAULT_E;
    }

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

    /**
     * Result for a single element after solve.
     * 
     * PHASE 1: Current fields
     * =======================
     * - elementId: beam element identifier
     * - endMomentStart, endMomentEnd: bending moments at the two element ends (local coords)
     * - bendingStress: derived stresses (see BendingStressResult)
     * 
     * PHASE 2: Expected additions
     * ============================
     * - shearForceStart, shearForceEnd (V at element ends)
     * - axialForce (N in element)
     * - stressDistribution: array of σ(x) along element for plotting
     * - safetyFactor: stress / yield (if material has yield data)
     */
    public static class ElementResult {
        public final int elementId;
        public final double endMomentStart;
        public final double endMomentEnd;
        public final double axialForce;
        public final BendingStressResult bendingStress;

        public ElementResult(int elementId, double endMomentStart, double endMomentEnd) {
            this(elementId, endMomentStart, endMomentEnd, 0.0, null);
        }

        public ElementResult(int elementId, double endMomentStart, double endMomentEnd, double axialForce, BendingStressResult bendingStress) {
            this.elementId = elementId;
            this.endMomentStart = endMomentStart;
            this.endMomentEnd = endMomentEnd;
            this.axialForce = axialForce;
            this.bendingStress = bendingStress;
        }

        public ElementResult(int elementId, double endMomentStart, double endMomentEnd, BendingStressResult bendingStress) {
            this(elementId, endMomentStart, endMomentEnd, 0.0, bendingStress);
        }
    }

    /**
     * Complete solver result containing global displacements and element results.
     * 
     * PHASE 1: Current fields
     * =======================
     * - displacements: array of [ux0, uy0, θ0, ux1, uy1, θ1, ...] for all nodes
     *   • ux: axial (horizontal) displacement
     *   • uy: transverse (vertical) displacement (Euler-Bernoulli: primary)
     *   • θ: rotation (slope, duy/dx)
     *   Size: 3 × nNodes
     * - elementResults: list of ElementResult (one per element)
     * 
     * PHASE 2: Expected additions
     * ============================
     * - reactionForces: support reactions [Fx, Fy, M] at each support
     * - globalStiffness: full or reduced K matrix (for modal analysis prep)
     * - eigenvalues: buckling modes / natural frequencies (Phase 2)
     * - convergence: number of iterations, residual (for nonlinear solvers)
     */
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
     * 
     * PHASE 1: Current implementation
     * ================================
     * - Uses moments (M1, M2) calculated from displacements via stiffness equation
     * - Estimates extreme fiber distance using rectangular-equivalent section: c = sqrt(3I/A)
     * - Reports maximum tensile and compressive stress at extreme fiber
     * - Assumes bending occurs in principal plane (no biaxial bending)
     * 
     * PHASE 2 TODO:
     * =============
     * - Include shear stress (τ = V·Q / (I·b)) from shear force diagrams
     * - Combined stress: σ_combined = sqrt(σ_bending² + 3·τ_shear²) (von Mises)
     * - Handle biaxial bending (Mz and My)
     * - Report stress distribution along element (not just extreme fiber)
     * - Compare against yield criterion for design assessment
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
        List<Element> elements = data.getElements();

        int nNodes = nodes.size();
        if (nNodes == 0) return new Result(new double[0], new ArrayList<>());

        Map<Integer, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) nodeIndex.put(nodes.get(i).getId(), i);

        int ndof = 3 * nNodes;  // 3 DOF per node: ux, uy, theta
        double[][] K = new double[ndof][ndof];  // Global stiffness matrix
        double[] F = new double[ndof];          // Global force vector

        // PHASE 1: ELEMENT ASSEMBLY
        // ============================
        // For each beam element:
        // 1. Build local 6x6 stiffness matrix (2 nodes × 3 DOF)
        //    - Rows/cols 0,3: axial (EA/L)
        //    - Rows/cols 1,2,4,5: bending (Euler-Bernoulli formulation)
        // 2. Transform to global coordinates using 6x6 rotation matrix T
        //    K_global = T * K_local * T^T
        // 3. Add to global K at appropriate DOF indices
        //
        // NOTE: Phase 2 will add distributed loads here (integrate along element length)
        //       Currently only point loads are handled below.
        // assemble each element into global K using transformation
        for (Element e : elements) {
            Integer si = null;
            Integer ti = null;
            double dx = 0, dy = 0, L = 0;
            double E = 0, A = 0, I = 0;

            if (e instanceof BeamElement be) {
                si = nodeIndex.get(be.getNodeStartId());
                ti = nodeIndex.get(be.getNodeEndId());
                if (si == null || ti == null) continue;
                Node ns = nodes.get(si);
                Node nt = nodes.get(ti);
                dx = nt.getX() - ns.getX();
                dy = nt.getY() - ns.getY();
                L = Math.hypot(dx, dy);
                if (L <= 1e-12) continue;
                E = resolveYoungsModulus(be, data);
                A = be.getArea();
                I = be.getInertia();
            } else if (e instanceof TrussElement te) {
                si = nodeIndex.get(te.getNodeStartId());
                ti = nodeIndex.get(te.getNodeEndId());
                if (si == null || ti == null) continue;
                Node ns = nodes.get(si);
                Node nt = nodes.get(ti);
                dx = nt.getX() - ns.getX();
                dy = nt.getY() - ns.getY();
                L = Math.hypot(dx, dy);
                if (L <= 1e-12) continue;
                E = resolveYoungsModulus(te, data);
                A = te.getArea();
                I = 0.0; // No bending stiffness for truss
            } else {
                continue;
            }

            // local element stiffness (6x6)
            // [K11  0   0  -K11  0    0  ]
            // [ 0  K22 K23  0  -K22 K23 ]
            // [ 0  K23 K33  0 -K23 K23' ]
            // [-K11  0   0  K11  0    0  ]
            // [ 0 -K22-K23  0  K22 -K23 ]
            // [ 0  K23 K23'  0 -K23 K33 ]
            // where K11 = EA/L (axial), K22,K23,K33 = bending terms
            double[][] ke = new double[6][6];
            // axial part (u1,u2) -> indices 0 and 3
            double EA = E * A;
            double kax = EA / L;
            ke[0][0] = kax; ke[0][3] = -kax; ke[3][0] = -kax; ke[3][3] = kax;

            // bending 4x4 (v1,theta1,v2,theta2) mapped to indices 1,2,4,5
            // Standard Euler-Bernoulli beam: 12EI/L³, 6EI/L², 4EI/L, 2EI/L
            double coef = E * I / (L * L * L);
            double[][] kb = new double[4][4];
            kb[0][0] = 12.0 * coef; kb[0][1] = 6.0 * L * coef; kb[0][2] = -12.0 * coef; kb[0][3] = 6.0 * L * coef;
            kb[1][0] = 6.0 * L * coef; kb[1][1] = 4.0 * L * L * coef; kb[1][2] = -6.0 * L * coef; kb[1][3] = 2.0 * L * L * coef;
            kb[2][0] = -12.0 * coef; kb[2][1] = -6.0 * L * coef; kb[2][2] = 12.0 * coef; kb[2][3] = -6.0 * L * coef;
            kb[3][0] = 6.0 * L * coef; kb[3][1] = 2.0 * L * L * coef; kb[3][2] = -6.0 * L * coef; kb[3][3] = 4.0 * L * L * coef;
            int[] bIdx = new int[]{1,2,4,5};
            for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) ke[bIdx[a]][bIdx[b]] = kb[a][b];

            // LOCAL-TO-GLOBAL TRANSFORMATION (Phase 1 feature)
            // ================================================
            // T is orthonormal: maps local coordinates to global
            // u_global = T * u_local  (displacement transformation)
            // Stiffness transform: K_global = T * K_local * T^T
            // This is critical for angled beams; without it, rotated elements would be incorrect
            // 
            // Phase 2: When adding curved elements or material rotation, ensure T is updated
            // transformation matrix L (6x6) local -> global
            double c = dx / L, s = dy / L;  // cos(θ), sin(θ) of element orientation
            double[][] T = new double[6][6];
            // node 1 (rotation matrix for [x, y, θ])
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

        // PHASE 2: LOAD ASSEMBLY
        // ======================
        // Phase 1: Only point loads and moments at nodes
        // Phase 2 TODO: Distributed loads (line loads) require:
        //   - Consistent mass matrix assembly (load vector entries)
        //   - Simpson's rule or Gauss quadrature for integration
        //   - Element-local load case computations
        //   - Transformation back to global coordinates
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

        // PHASE 3: BOUNDARY CONDITION APPLICATION
        // ========================================
        // Method: DOF elimination (system reduction)
        // Alternative (Phase 2): penalty method for sliding/partial constraints
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
        // AUTOMATIC DOF MANAGEMENT: disable rotation if no beam element is connected to the node
        boolean[] hasRotationStiffness = new boolean[nNodes];
        for (Element e : elements) {
            if (e instanceof BeamElement) {
                for (int nodeId : e.getNodeIds()) {
                    Integer ni = nodeIndex.get(nodeId);
                    if (ni != null) hasRotationStiffness[ni] = true;
                }
            }
        }

        int freeCount = 0;
        int[] freeMap = new int[ndof];
        for (int i = 0; i < ndof; i++) {
            int ni = i / 3;
            int dofType = i % 3; // 0=ux, 1=uy, 2=theta
            boolean autoConstrain = (dofType == 2 && !hasRotationStiffness[ni]);

            if (!constrained[i] && !autoConstrain) {
                freeMap[i] = freeCount++;
            } else {
                freeMap[i] = -1;
            }
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

        // PHASE 4: LINEAR SOLVE
        // =====================
        // Phase 1: Direct solver (LU decomposition) via EJML
        // Phase 2 TODO: For large problems (1000+ DOFs):
        //   - Sparse solver (iterative or direct sparse factorization)
        //   - Preconditioner for iterative solvers
        //   - Consider: SuperLU, PARDISO, or UMFPACK wrappers
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

        // PHASE 5: POST-PROCESSING (Element Results)
        // ===========================================
        // Current: End moments and bending stress at extreme fiber
        // Phase 2 TODO:
        //   - Shear forces at element ends (V = d²(EI·u)/dx²)
        //   - Axial forces (N = EA·du/dx)
        //   - Distributed shear and bending diagrams (interpolate along element)
        //   - Safety factors and yield assessment
        //   - Combined stress (bending + axial)
        // compute element end moments (use local coordinates)
        List<ElementResult> elemResults = new ArrayList<>();
        for (Element e : elements) {
            Integer si = nodeIndex.get(e.getNodeStartId());
            Integer ti = nodeIndex.get(e.getNodeEndId());
            if (si == null || ti == null) continue;
            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);
            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L <= 1e-12) continue;

            double E = resolveYoungsModulus(e, data);
            double A = e.getArea();
            double I = (e instanceof BeamElement be) ? be.getInertia() : 0.0;

            // rebuild local ke (6x6)
            double[][] ke = new double[6][6];
            double EA = E * A; double kax = EA / L;
            ke[0][0] = kax; ke[0][3] = -kax; ke[3][0] = -kax; ke[3][3] = kax;
            
            if (e instanceof BeamElement) {
                double coef = E * I / (L * L * L);
                double[][] kb = new double[4][4];
                kb[0][0] = 12.0 * coef; kb[0][1] = 6.0 * L * coef; kb[0][2] = -12.0 * coef; kb[0][3] = 6.0 * L * coef;
                kb[1][0] = 6.0 * L * coef; kb[1][1] = 4.0 * L * L * coef; kb[1][2] = -6.0 * L * coef; kb[1][3] = 2.0 * L * L * coef;
                kb[2][0] = -12.0 * coef; kb[2][1] = -6.0 * L * coef; kb[2][2] = 12.0 * coef; kb[2][3] = -6.0 * L * coef;
                kb[3][0] = 6.0 * L * coef; kb[3][1] = 2.0 * L * L * coef; kb[3][2] = -6.0 * L * coef; kb[3][3] = 4.0 * L * L * coef;
                int[] bIdx = new int[]{1,2,4,5};
                for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) ke[bIdx[a]][bIdx[b]] = kb[a][b];
            }

            // transformation matrix T (local -> global)
            double c = dx / L, s = dy / L;
            double[][] T = new double[6][6];
            T[0][0] = c; T[0][1] = -s; T[0][2] = 0; T[1][0] = s; T[1][1] = c; T[1][2] = 0; T[2][2] = 1;
            T[3][3] = c; T[3][4] = -s; T[3][5] = 0; T[4][3] = s; T[4][4] = c; T[4][5] = 0; T[5][5] = 1;

            int[] dofMap = new int[]{3*si, 3*si + 1, 3*si + 2, 3*ti, 3*ti + 1, 3*ti + 2};
            double[] uglob = new double[6];
            for (int i = 0; i < 6; i++) uglob[i] = u[dofMap[i]];

            // u_local = T^T * u_global
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

            double axial = -felocal[0]; // axial force (tension positive)
            double M1 = felocal[2];
            double M2 = felocal[5];

            BendingStressResult stress;
            if (e instanceof BeamElement be) {
                stress = computeBendingStress(be, M1, M2);
                // add axial stress component
                if (A > 0) {
                    double sigmaAxial = axial / A;
                    stress = new BendingStressResult(be.getId(), stress.extremeFiberDistance, stress.maxBendingMoment,
                            stress.maxTensileStress + sigmaAxial, stress.maxCompressiveStress + sigmaAxial);
                }
            } else {
                // Truss element
                double sigma = (A > 0) ? axial / A : 0.0;
                stress = new BendingStressResult(e.getId(), 0.0, 0.0, sigma, sigma);
            }
            elemResults.add(new ElementResult(e.getId(), M1, M2, axial, stress));
        }

        return new Result(u, elemResults);
    }
}
