package com.treble.feasimulation.mesh;

import com.treble.feasimulation.model.*;
import com.treble.feasimulation.solver.PlaneStressResult;
import com.treble.feasimulation.solver.PlaneStressSolver;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that meshing multiple polygon regions produces globally unique node
 * and element IDs so that PlaneStressSolver can assemble a correct global
 * stiffness matrix without key collisions in nodeIdToIndex.
 */
public class MultiRegionMeshTest {

    // -----------------------------------------------------------------------
    // Mesh-generator level: IDs are unique when caller supplies offsets
    // -----------------------------------------------------------------------

    @Test
    public void twoRegions_nodeIds_areUnique() {
        TriangularMeshGenerator gen = new TriangularMeshGenerator();

        PolygonRegion r1 = square(1, 0, 0, 1);   // 4-vertex unit square
        PolygonRegion r2 = square(2, 2, 0, 1);   // 4-vertex unit square shifted right

        // First region: IDs 1..4 (nodes), 1..2 (elements)
        MeshGenerator.MeshResult mesh1 = gen.generateMesh(r1, 1.0, 1, 1);
        int nextNode = 1 + mesh1.getNodes().size();
        int nextElem = 1 + mesh1.getElements().size();

        // Second region starts where first left off
        MeshGenerator.MeshResult mesh2 = gen.generateMesh(r2, 1.0, nextNode, nextElem);

        Set<Integer> nodeIds = new HashSet<>();
        for (com.treble.feasimulation.model.Node n : mesh1.getNodes()) {
            assertTrue(nodeIds.add(n.getId()),
                "Duplicate node ID " + n.getId() + " within region 1");
        }
        for (com.treble.feasimulation.model.Node n : mesh2.getNodes()) {
            assertTrue(nodeIds.add(n.getId()),
                "Node ID " + n.getId() + " from region 2 collides with region 1");
        }

        Set<Integer> elemIds = new HashSet<>();
        for (TriangularElement e : mesh1.getElements()) {
            assertTrue(elemIds.add(e.getId()),
                "Duplicate element ID " + e.getId() + " within region 1");
        }
        for (TriangularElement e : mesh2.getElements()) {
            assertTrue(elemIds.add(e.getId()),
                "Element ID " + e.getId() + " from region 2 collides with region 1");
        }
    }

    @Test
    public void threeRegions_nodeIds_areUnique() {
        TriangularMeshGenerator gen = new TriangularMeshGenerator();

        PolygonRegion r1 = square(1, 0,  0, 1);
        PolygonRegion r2 = square(2, 2,  0, 1);
        PolygonRegion r3 = square(3, 4,  0, 1);

        Set<Integer> nodeIds = new HashSet<>();
        Set<Integer> elemIds = new HashSet<>();
        int nextNode = 1;
        int nextElem = 1;

        for (PolygonRegion region : Arrays.asList(r1, r2, r3)) {
            MeshGenerator.MeshResult mesh = gen.generateMesh(region, 1.0, nextNode, nextElem);
            nextNode += mesh.getNodes().size();
            nextElem += mesh.getElements().size();

            for (com.treble.feasimulation.model.Node n : mesh.getNodes()) {
                assertTrue(nodeIds.add(n.getId()),
                    "Collision: node ID " + n.getId() + " already seen");
            }
            for (TriangularElement e : mesh.getElements()) {
                assertTrue(elemIds.add(e.getId()),
                    "Collision: element ID " + e.getId() + " already seen");
            }
        }
        // Sanity: three 4-node squares = 12 unique node IDs
        assertEquals(12, nodeIds.size());
    }

    @Test
    public void defaultOverload_stillStartsAtOne() {
        // The default 2-arg overload must remain unchanged for single-region callers.
        TriangularMeshGenerator gen = new TriangularMeshGenerator();
        PolygonRegion r = square(1, 0, 0, 1);
        MeshGenerator.MeshResult mesh = gen.generateMesh(r, 1.0);

        int minId = mesh.getNodes().stream().mapToInt(com.treble.feasimulation.model.Node::getId).min().orElseThrow();
        assertEquals(1, minId, "Default overload should produce node IDs starting at 1");

        int minElemId = mesh.getElements().stream().mapToInt(TriangularElement::getId).min().orElseThrow();
        assertEquals(1, minElemId, "Default overload should produce element IDs starting at 1");
    }

    // -----------------------------------------------------------------------
    // Solver level: PlaneStressSolver handles two regions without ID collision
    // -----------------------------------------------------------------------

    @Test
    public void solver_twoRegions_noIdCollision_producesValidResult() {
        FEAData data = new FEAData();

        Material mat = new Material(1, "Steel", 2.0e11, 7850.0, 2.5e8, 0.3, 0.01);
        data.addMaterial(mat);

        // Two side-by-side unit squares, both fixed on their left edge, loaded on right.
        PolygonRegion r1 = square(1, 0, 0, 1);
        PolygonRegion r2 = square(2, 2, 0, 1);
        data.addPolygonRegion(r1);
        data.addPolygonRegion(r2);

        // Fix left edge of each region (edge index 3 for a CCW unit square)
        data.addEdgeSupport(new EdgeSupport(1, 1, 3, Support.Type.FIXED));
        data.addEdgeSupport(new EdgeSupport(2, 2, 3, Support.Type.FIXED));

        // Load on right edge of each region (edge index 1)
        data.addDistributedLoad(new DistributedLoad(1, 1, 1, 1000, 0));
        data.addDistributedLoad(new DistributedLoad(2, 2, 1, 1000, 0));

        PlaneStressSolver solver = new PlaneStressSolver();
        PlaneStressResult result = (PlaneStressResult) solver.solve(data);

        assertNotNull(result, "Solver must return a result");
        assertTrue(result.getDisplacements().length > 0, "Displacement vector must be non-empty");
        assertFalse(result.getElementStresses().isEmpty(), "Element stresses must be present");

        // nodeIdToIndex must contain one entry per node — no overwritten entries.
        // With two 4-node squares the total is 8 unique nodes → 8 entries.
        assertEquals(8, result.getNodeIdToIndex().size(),
            "nodeIdToIndex must have one entry per unique node across both regions");

        // All displacement values must be finite (NaN / Inf indicates a singular assembly).
        for (double d : result.getDisplacements()) {
            assertTrue(Double.isFinite(d),
                "Displacement must be finite — NaN/Inf indicates a bad assembly");
        }

        // Both regions should show positive sigmaX (tensile in X under the load).
        assertTrue(result.getElementStresses().stream().allMatch(s -> s.sigmaX > 0),
            "All elements should be in tension under the applied X-direction load");
    }

    @Test
    public void solver_singleRegion_nodeIdToIndex_unaffected() {
        // Regression: single-region behaviour must be identical to before the fix.
        FEAData data = new FEAData();
        Material mat = new Material(1, "Steel", 2.0e11, 7850.0, 2.5e8, 0.3, 0.01);
        data.addMaterial(mat);

        PolygonRegion r = square(1, 0, 0, 1);
        data.addPolygonRegion(r);
        data.addEdgeSupport(new EdgeSupport(1, 1, 3, Support.Type.FIXED));
        data.addDistributedLoad(new DistributedLoad(1, 1, 1, 1000, 0));

        PlaneStressSolver solver = new PlaneStressSolver();
        PlaneStressResult result = (PlaneStressResult) solver.solve(data);

        assertEquals(4, result.getNodeIdToIndex().size(),
            "Single unit-square region must produce exactly 4 nodes");
        assertTrue(result.getNodeIdToIndex().containsKey(1));
        assertTrue(result.getNodeIdToIndex().containsKey(2));
        assertTrue(result.getNodeIdToIndex().containsKey(3));
        assertTrue(result.getNodeIdToIndex().containsKey(4));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /** Creates a unit square whose bottom-left corner is at (ox, oy). */
    private static PolygonRegion square(int id, double ox, double oy, int materialId) {
        List<PolygonRegion.Vertex> verts = Arrays.asList(
            new PolygonRegion.Vertex(ox,       oy),
            new PolygonRegion.Vertex(ox + 1.0, oy),
            new PolygonRegion.Vertex(ox + 1.0, oy + 1.0),
            new PolygonRegion.Vertex(ox,       oy + 1.0)
        );
        return new PolygonRegion(id, verts, materialId);
    }
}