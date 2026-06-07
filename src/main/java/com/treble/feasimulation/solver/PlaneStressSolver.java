package com.treble.feasimulation.solver;

import com.treble.feasimulation.mesh.MeshGenerator;
import com.treble.feasimulation.mesh.TriangularMeshGenerator;
import com.treble.feasimulation.model.*;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

import java.util.*;

/**
 * Solver for plane-stress continuum problems.
 */
public class PlaneStressSolver implements FEASolver {
    private double meshDensity = 1.0;

    public void setMeshDensity(double density) {
        this.meshDensity = density;
    }

    @Override
    public SolverResult solve(FEAData data) {
        if (data.getPolygonRegions().isEmpty()) {
            return new PlaneStressResult(new double[0], new ArrayList<>(), new HashMap<>(), new ArrayList<>());
        }

        List<Node> allNodes = new ArrayList<>();
        List<TriangularElement> allElements = new ArrayList<>();
        Map<Integer, List<Node>> regionToNodes = new HashMap<>();

        MeshGenerator generator = new TriangularMeshGenerator();
        for (PolygonRegion region : data.getPolygonRegions()) {
            MeshGenerator.MeshResult mesh = generator.generateMesh(region, meshDensity);
            
            Material material = data.getMaterials().stream()
                    .filter(m -> m.getId() == region.getMaterialId())
                    .findFirst()
                    .orElse(MaterialLibrary.getDefaultMaterial());
            
            double e = material.getYoungsModulus();
            double nu = material.getPoissonRatio();
            double t = material.getThickness();

            // Update thickness and compute stiffness for elements
            for (TriangularElement element : mesh.getElements()) {
                // Ensure we use the material thickness
                // We create a new element to ensure all internal fields (like area) are consistent 
                // but actually TriangularElement already has thickness from material.
                // Wait, it was created with 1.0 in generator. 
                // Let's use a reflected change or just set it if possible.
                // TriangularElement.thickness is final, so we MUST recreate or change it.
                // Easiest is to recreate here or modify TriangularElement.
                TriangularElement correctElement = new TriangularElement(
                    element.getId(),
                    element.getNodes()[0],
                    element.getNodes()[1],
                    element.getNodes()[2],
                    element.getMaterialId(),
                    t
                );
                correctElement.computeStiffnessMatrix(e, nu);
                allElements.add(correctElement);
            }

            regionToNodes.put(region.getId(), mesh.getNodes());
            allNodes.addAll(mesh.getNodes());
            // allElements.addAll(mesh.getElements()); // Replaced by the loop above
        }

        int nNodes = allNodes.size();
        int ndof = nNodes * 2;
        DMatrixRMaj K = new DMatrixRMaj(ndof, ndof);
        DMatrixRMaj F = new DMatrixRMaj(ndof, 1);

        Map<Integer, Integer> nodeIdToIndex = new HashMap<>();
        for (int i = 0; i < nNodes; i++) {
            nodeIdToIndex.put(allNodes.get(i).getId(), i);
        }

        // 1. Assemble Global Stiffness Matrix
        for (TriangularElement element : allElements) {
            double[][] ke = element.getStiffnessMatrix();
            Node[] eNodes = element.getNodes();
            int[] gIdx = new int[6];
            for (int i = 0; i < 3; i++) {
                int nodeIdx = nodeIdToIndex.get(eNodes[i].getId());
                gIdx[2 * i] = 2 * nodeIdx;
                gIdx[2 * i + 1] = 2 * nodeIdx + 1;
            }

            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 6; j++) {
                    K.add(gIdx[i], gIdx[j], ke[i][j]);
                }
            }
        }

        // 2. Apply Boundary Conditions
        boolean[] fixed = new boolean[ndof];

        // Node-based supports
        for (Support s : data.getSupports()) {
            Integer idx = nodeIdToIndex.get(s.getNodeId());
            if (idx == null) continue;

            if (s.getType() == Support.Type.FIXED || s.getType() == Support.Type.PINNED) {
                fixed[2 * idx] = true;
                fixed[2 * idx + 1] = true;
            } else if (s.getType() == Support.Type.ROLLER) {
                fixed[2 * idx + 1] = true; // Fixed in Y
            }
        }

        // Edge-based supports
        for (EdgeSupport es : data.getEdgeSupports()) {
            List<Node> regionNodes = regionToNodes.get(es.getPolygonId());
            if (regionNodes == null) continue;

            PolygonRegion region = data.getPolygonRegions().stream()
                    .filter(r -> r.getId() == es.getPolygonId())
                    .findFirst().orElse(null);
            if (region == null) continue;

            List<PolygonRegion.Edge> edges = region.getEdges();
            if (es.getEdgeIndex() < 0 || es.getEdgeIndex() >= edges.size()) continue;

            PolygonRegion.Edge edge = edges.get(es.getEdgeIndex());
            // In TriangularMeshGenerator, nodes are created by subdividing edges.
            // We need to find all nodes that lie on this edge.
            Node vStart = regionNodes.get(edge.getStartVertexIndex());
            Node vEnd = regionNodes.get(edge.getEndVertexIndex());

            double x1 = vStart.getX();
            double y1 = vStart.getY();
            double x2 = vEnd.getX();
            double y2 = vEnd.getY();

            for (Node n : regionNodes) {
                // Check if node n is on segment (x1,y1)-(x2,y2)
                if (isNodeOnSegment(n, x1, y1, x2, y2)) {
                    int idx = nodeIdToIndex.get(n.getId());
                    if (es.getType() == Support.Type.FIXED || es.getType() == Support.Type.PINNED) {
                        fixed[2 * idx] = true;
                        fixed[2 * idx + 1] = true;
                    } else if (es.getType() == Support.Type.ROLLER) {
                        double dx = x2 - x1;
                        double dy = y2 - y1;
                        double L = Math.hypot(dx, dy);
                        if (L > 1e-12) {
                            double nx = -dy / L;
                            double ny = dx / L;
                            if (Math.abs(nx) > Math.abs(ny)) {
                                fixed[2 * idx] = true;
                            } else {
                                fixed[2 * idx + 1] = true;
                            }
                        }
                    }
                }
            }
        }

        // 3. Apply Loads
        // Node-based point loads
        for (PointLoad pl : data.getPointLoads()) {
            Integer idx = nodeIdToIndex.get(pl.getNodeId());
            if (idx == null) continue;
            F.add(2 * idx, 0, pl.getFx());
            F.add(2 * idx + 1, 0, pl.getFy());
        }

        // Edge-based distributed loads
        for (DistributedLoad dl : data.getDistributedLoads()) {
            List<Node> regionNodes = regionToNodes.get(dl.getPolygonId());
            if (regionNodes == null) continue;

            PolygonRegion region = data.getPolygonRegions().stream()
                    .filter(r -> r.getId() == dl.getPolygonId())
                    .findFirst().orElse(null);
            if (region == null) continue;

            List<PolygonRegion.Edge> edges = region.getEdges();
            if (dl.getEdgeIndex() < 0 || dl.getEdgeIndex() >= edges.size()) continue;

            PolygonRegion.Edge edge = edges.get(dl.getEdgeIndex());
            Node vStart = regionNodes.get(edge.getStartVertexIndex());
            Node vEnd = regionNodes.get(edge.getEndVertexIndex());

            double x1 = vStart.getX();
            double y1 = vStart.getY();
            double x2 = vEnd.getX();
            double y2 = vEnd.getY();

            // Find all nodes on this edge and sort them along the edge
            List<Node> edgeNodes = new ArrayList<>();
            for (Node n : regionNodes) {
                if (isNodeOnSegment(n, x1, y1, x2, y2)) {
                    edgeNodes.add(n);
                }
            }

            // Sort edgeNodes from vStart to vEnd
            edgeNodes.sort(Comparator.comparingDouble(n -> 
                (n.getX() - x1) * (x2 - x1) + (n.getY() - y1) * (y2 - y1)));

            for (int i = 0; i < edgeNodes.size() - 1; i++) {
                Node n1 = edgeNodes.get(i);
                Node n2 = edgeNodes.get(i + 1);

                double dx = n2.getX() - n1.getX();
                double dy = n2.getY() - n1.getY();
                double L = Math.hypot(dx, dy);
                if (L < 1e-12) continue;

                double fx1, fy1, fx2, fy2;
                
                if (dl.getType() == DistributedLoad.Type.UNIFORM) {
                    double nx = -dy / L;
                    double ny = dx / L;
                    double tx = dx / L;
                    double ty = dy / L;

                    double fx = dl.getWx() * nx + dl.getWy() * tx;
                    double fy = dl.getWx() * ny + dl.getWy() * ty;

                    fx1 = fx2 = fx * L / 2.0;
                    fy1 = fy2 = fy * L / 2.0;
                } else {
                    fx1 = fx2 = dl.getWx() * L / 2.0;
                    fy1 = fy2 = dl.getWy() * L / 2.0;
                }

                int idx1 = nodeIdToIndex.get(n1.getId());
                int idx2 = nodeIdToIndex.get(n2.getId());

                F.add(2 * idx1, 0, fx1);
                F.add(2 * idx1 + 1, 0, fy1);
                F.add(2 * idx2, 0, fx2);
                F.add(2 * idx2 + 1, 0, fy2);
            }
        }

        // 4. Solve System
        int freeCount = 0;
        int[] freeMap = new int[ndof];
        Arrays.fill(freeMap, -1);
        for (int i = 0; i < ndof; i++) {
            if (!fixed[i]) freeMap[i] = freeCount++;
        }

        if (freeCount == 0) {
            return new PlaneStressResult(new double[ndof], new ArrayList<>(), nodeIdToIndex, allElements);
        }

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
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.lu(freeCount);
        if (!solver.setA(Ared)) {
            throw new IllegalStateException("Singular stiffness matrix (structure unstable or insufficient supports)");
        }
        solver.solve(bred, xred);

        double[] fullU = new double[ndof];
        for (int i = 0; i < ndof; i++) {
            if (freeMap[i] >= 0) fullU[i] = xred.get(freeMap[i], 0);
        }

        // 5. Compute Element Stresses
        List<PlaneStressResult.ElementStress> elementStresses = new ArrayList<>();
        for (TriangularElement element : allElements) {
            Material material = data.getMaterials().stream()
                    .filter(m -> m.getId() == element.getMaterialId())
                    .findFirst()
                    .orElse(MaterialLibrary.getDefaultMaterial());
            double E = material.getYoungsModulus();
            double nu = material.getPoissonRatio();

            elementStresses.add(computeElementStress(element, fullU, nodeIdToIndex, E, nu));
        }

        return new PlaneStressResult(fullU, elementStresses, nodeIdToIndex, allElements);
    }

    private boolean isNodeOnSegment(Node n, double x1, double y1, double x2, double y2) {
        double px = n.getX();
        double py = n.getY();
        
        // Check if point is collinear with segment
        // Cross product: (y2-y1)(px-x1) - (x2-x1)(py-y1) should be 0
        double cross = (y2 - y1) * (px - x1) - (x2 - x1) * (py - y1);
        if (Math.abs(cross) > 1e-7) return false;

        // Check if point is within the bounding box of the segment
        return px >= Math.min(x1, x2) - 1e-7 && px <= Math.max(x1, x2) + 1e-7 &&
               py >= Math.min(y1, y2) - 1e-7 && py <= Math.max(y1, y2) + 1e-7;
    }

    private PlaneStressResult.ElementStress computeElementStress(TriangularElement element, double[] fullU, Map<Integer, Integer> nodeIdToIndex, double E, double nu) {
        double area = element.getArea();
        Node[] nodes = element.getNodes();
        
        // Element nodal displacements
        double[] ue = new double[6];
        for (int i = 0; i < 3; i++) {
            int idx = nodeIdToIndex.get(nodes[i].getId());
            ue[2 * i] = fullU[2 * idx];
            ue[2 * i + 1] = fullU[2 * idx + 1];
        }

        // B matrix (Strain-Displacement Matrix)
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

        double inv2A = 1.0 / (2.0 * area);
        
        // Strains: {epsilon} = [B]{ue}
        double epsX = inv2A * (b1 * ue[0] + b2 * ue[2] + b3 * ue[4]);
        double epsY = inv2A * (c1 * ue[1] + c2 * ue[3] + c3 * ue[5]);
        double gammaXY = inv2A * (c1 * ue[0] + b1 * ue[1] + c2 * ue[2] + b2 * ue[3] + c3 * ue[4] + b3 * ue[5]);

        // Stresses: {sigma} = [D]{epsilon}
        // D = (E / (1 - nu^2)) * [1  nu  0;
        //                        nu  1  0;
        //                        0   0  (1-nu)/2]
        double factor = E / (1 - nu * nu);
        double sigmaX = factor * (epsX + nu * epsY);
        double sigmaY = factor * (nu * epsX + epsY);
        double tauXY = factor * ((1 - nu) / 2.0) * gammaXY;

        return new PlaneStressResult.ElementStress(element.getId(), sigmaX, sigmaY, tauXY);
    }
}
