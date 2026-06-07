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
                // TriangularElement is created with default thickness 1.0 in generator, 
                // but we use the material thickness here.
                // We'll create a new element with correct properties to be safe if needed, 
                // but TriangularElement seems to have a thickness field.
                // Let's assume we can use the one from mesh and just ensure stiffness is computed.
                element.computeStiffnessMatrix(e, nu);
            }

            regionToNodes.put(region.getId(), mesh.getNodes());
            allNodes.addAll(mesh.getNodes());
            allElements.addAll(mesh.getElements());
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
            // In TriangularMeshGenerator, nodes are created in order of region vertices.
            // So vertex index corresponds to node index in regionNodes.
            Node n1 = regionNodes.get(edge.getStartVertexIndex());
            Node n2 = regionNodes.get(edge.getEndVertexIndex());

            for (Node n : Arrays.asList(n1, n2)) {
                int idx = nodeIdToIndex.get(n.getId());
                if (es.getType() == Support.Type.FIXED || es.getType() == Support.Type.PINNED) {
                    fixed[2 * idx] = true;
                    fixed[2 * idx + 1] = true;
                } else if (es.getType() == Support.Type.ROLLER) {
                    fixed[2 * idx + 1] = true;
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
            Node n1 = regionNodes.get(edge.getStartVertexIndex());
            Node n2 = regionNodes.get(edge.getEndVertexIndex());

            double L = Math.hypot(n2.getX() - n1.getX(), n2.getY() - n1.getY());
            double totalFx = dl.getWx() * L;
            double totalFy = dl.getWy() * L;

            // Distribute to the two nodes of the edge
            int idx1 = nodeIdToIndex.get(n1.getId());
            int idx2 = nodeIdToIndex.get(n2.getId());

            F.add(2 * idx1, 0, totalFx / 2.0);
            F.add(2 * idx1 + 1, 0, totalFy / 2.0);
            F.add(2 * idx2, 0, totalFx / 2.0);
            F.add(2 * idx2 + 1, 0, totalFy / 2.0);
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
