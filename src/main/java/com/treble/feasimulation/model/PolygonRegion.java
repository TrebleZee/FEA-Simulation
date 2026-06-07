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
    }
}
