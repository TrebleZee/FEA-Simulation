package com.treble.feasimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A closed 2D polygon region for Phase 3 continuum meshing.
 * Vertices are stored in order; the polygon is implicitly closed (last vertex connects to first).
 */
public class PolygonRegion {
    private final int id;
    private final int materialId;
    private final List<Vertex> vertices;

    public PolygonRegion(int id, List<Vertex> vertices, int materialId) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("Polygon requires at least 3 vertices");
        }
        this.id = id;
        this.materialId = materialId;
        this.vertices = new ArrayList<>(vertices);
        ensureCounterClockwise();
    }

    private void ensureCounterClockwise() {
        double area = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v1 = vertices.get(i);
            Vertex v2 = vertices.get((i + 1) % vertices.size());
            area += (v2.x - v1.x) * (v2.y + v1.y);
        }
        if (area > 0) {
            Collections.reverse(vertices);
        }
    }

    public static PolygonRegion fromCoordinates(int id, double[][] coordinates, int materialId) {
        List<Vertex> verts = new ArrayList<>();
        for (double[] c : coordinates) {
            verts.add(new Vertex(c[0], c[1]));
        }
        return new PolygonRegion(id, verts, materialId);
    }

    public int getId() {
        return id;
    }

    public int getMaterialId() {
        return materialId;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public double getX(int index) {
        return vertices.get(index).x;
    }

    public double getY(int index) {
        return vertices.get(index).y;
    }

    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    public void updateVertex(int index, double x, double y) {
        vertices.get(index).x = x;
        vertices.get(index).y = y;
    }

    /**
     * Remove a vertex. Returns false if removal would leave fewer than 3 vertices.
     */
    public boolean removeVertex(int index) {
        if (vertices.size() <= 3) {
            return false;
        }
        vertices.remove(index);
        return true;
    }

    public PolygonRegion copy() {
        List<Vertex> copy = new ArrayList<>();
        for (Vertex v : vertices) {
            copy.add(new Vertex(v.x, v.y));
        }
        return new PolygonRegion(id, copy, materialId);
    }

    @Override
    public String toString() {
        return "PolygonRegion{" + id + ", vertices=" + vertices.size() + ", mat=" + materialId + "}";
    }

    public static final class Vertex {
        public double x;
        public double y;

        public Vertex(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public double getY() { return y; }
    }

    public static final class Edge {
        private final int startVertexIndex;
        private final int endVertexIndex;

        public Edge(int startVertexIndex, int endVertexIndex) {
            this.startVertexIndex = startVertexIndex;
            this.endVertexIndex = endVertexIndex;
        }

        public int getStartVertexIndex() { return startVertexIndex; }
        public int getEndVertexIndex() { return endVertexIndex; }
    }

    public List<Edge> getEdges() {
        List<Edge> edges = new ArrayList<>();
        int count = vertices.size();
        for (int i = 0; i < count; i++) {
            edges.add(new Edge(i, (i + 1) % count));
        }
        return Collections.unmodifiableList(edges);
    }
}
