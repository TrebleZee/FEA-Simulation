package com.treble.feasimulation.mesh;

import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PolygonRegion;
import com.treble.feasimulation.model.TriangularElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a triangular mesh generator using the Ear Clipping algorithm.
 */
public class TriangularMeshGenerator implements MeshGenerator {
    @Override
    public MeshResult generateMesh(PolygonRegion region, double density) {
        // Subdivide edges based on density
        List<PolygonRegion.Vertex> originalVertices = region.getVertices();
        List<PolygonRegion.Vertex> subdividedVertices = new ArrayList<>();
        
        // density parameter: target edge length (approx)
        // Smaller density -> more subdivisions. 
        // Let's interpret density as "approximate target edge length".
        // If density <= 0, we'll use 1.0 or some default.
        double targetLength = (density > 0) ? density : 50.0; 

        for (int i = 0; i < originalVertices.size(); i++) {
            PolygonRegion.Vertex v1 = originalVertices.get(i);
            PolygonRegion.Vertex v2 = originalVertices.get((i + 1) % originalVertices.size());
            
            double dx = v2.x - v1.x;
            double dy = v2.y - v1.y;
            double L = Math.hypot(dx, dy);
            
            int numSegments = (int) Math.max(1, Math.ceil(L / targetLength));
            for (int j = 0; j < numSegments; j++) {
                double t = (double) j / numSegments;
                subdividedVertices.add(new PolygonRegion.Vertex(v1.x + t * dx, v1.y + t * dy));
            }
        }

        List<Node> nodes = new ArrayList<>();
        List<TriangularElement> elements = new ArrayList<>();

        // Create nodes for each vertex
        for (int i = 0; i < subdividedVertices.size(); i++) {
            nodes.add(new Node(i + 1, subdividedVertices.get(i).x, subdividedVertices.get(i).y));
        }

        if (subdividedVertices.size() < 3) {
            return new MeshResult(nodes, elements);
        }

        // We need to keep track of the original indices to map nodes to elements
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < subdividedVertices.size(); i++) {
            indexList.add(i);
        }

        int elementIdCounter = 1;
        while (indexList.size() > 3) {
            boolean earFound = false;
            for (int i = 0; i < indexList.size(); i++) {
                int prev = indexList.get((i - 1 + indexList.size()) % indexList.size());
                int curr = indexList.get(i);
                int next = indexList.get((i + 1) % indexList.size());

                if (isEar(prev, curr, next, indexList, subdividedVertices)) {
                    // Create element
                    elements.add(new TriangularElement(
                            elementIdCounter++,
                            nodes.get(prev),
                            nodes.get(curr),
                            nodes.get(next),
                            region.getMaterialId(),
                            1.0 // Thickness will be updated by solver
                    ));

                    // Remove ear
                    indexList.remove(i);
                    earFound = true;
                    break;
                }
            }

            if (!earFound) {
                // This shouldn't happen for a simple polygon, but we avoid infinite loop
                break;
            }
        }

        // Last triangle
        if (indexList.size() == 3) {
            elements.add(new TriangularElement(
                    elementIdCounter++,
                    nodes.get(indexList.get(0)),
                    nodes.get(indexList.get(1)),
                    nodes.get(indexList.get(2)),
                    region.getMaterialId(),
                    1.0
            ));
        }

        return new MeshResult(nodes, elements);
    }

    private boolean isEar(int pIdx, int cIdx, int nIdx, List<Integer> indexList, List<PolygonRegion.Vertex> vertices) {
        PolygonRegion.Vertex p = vertices.get(pIdx);
        PolygonRegion.Vertex c = vertices.get(cIdx);
        PolygonRegion.Vertex n = vertices.get(nIdx);

        // Must be convex
        if (crossProduct(p, c, n) <= 0) {
            // Check if we are using CCW or CW. 
            // Standard ear clipping assumes CCW.
            // Let's check the signed area of the whole polygon if we want to be robust,
            // but for now assume CCW or adjust cross product sign.
            // Actually, if crossProduct <= 0 it's either concave or collinear.
            // Wait, we need to know the winding order.
            if (isClockwise(vertices)) {
                 if (crossProduct(p, c, n) >= 0) return false;
            } else {
                 if (crossProduct(p, c, n) <= 0) return false;
            }
        }

        // No other vertex should be inside the triangle (p, c, n)
        for (Integer idx : indexList) {
            if (idx == pIdx || idx == cIdx || idx == nIdx) continue;
            if (isPointInTriangle(vertices.get(idx), p, c, n)) {
                return false;
            }
        }

        return true;
    }

    private double crossProduct(PolygonRegion.Vertex a, PolygonRegion.Vertex b, PolygonRegion.Vertex c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private boolean isPointInTriangle(PolygonRegion.Vertex pt, PolygonRegion.Vertex a, PolygonRegion.Vertex b, PolygonRegion.Vertex c) {
        double cp1 = crossProduct(a, b, pt);
        double cp2 = crossProduct(b, c, pt);
        double cp3 = crossProduct(c, a, pt);

        return (cp1 >= 0 && cp2 >= 0 && cp3 >= 0) || (cp1 <= 0 && cp2 <= 0 && cp3 <= 0);
    }

    private boolean isClockwise(List<PolygonRegion.Vertex> vertices) {
        double area = 0;
        for (int i = 0; i < vertices.size(); i++) {
            PolygonRegion.Vertex v1 = vertices.get(i);
            PolygonRegion.Vertex v2 = vertices.get((i + 1) % vertices.size());
            area += (v2.x - v1.x) * (v2.y + v1.y);
        }
        return area > 0;
    }
}
