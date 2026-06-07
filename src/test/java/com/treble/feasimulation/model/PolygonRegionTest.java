package com.treble.feasimulation.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PolygonRegionTest {

    @Test
    public void testPolygonCreation() {
        List<PolygonRegion.Vertex> vertices = Arrays.asList(
            new PolygonRegion.Vertex(0, 0),
            new PolygonRegion.Vertex(100, 0),
            new PolygonRegion.Vertex(100, 100),
            new PolygonRegion.Vertex(0, 100)
        );
        PolygonRegion region = new PolygonRegion(1, vertices, 1);
        assertEquals(1, region.getId());
        assertEquals(4, region.getVertexCount());
        assertEquals(1, region.getMaterialId());
        assertEquals(0, region.getX(0));
        assertEquals(0, region.getY(0));
        assertEquals(100, region.getX(1));
        assertEquals(0, region.getY(1));
    }

    @Test
    public void testPolygonEditing() {
        List<PolygonRegion.Vertex> vertices = Arrays.asList(
            new PolygonRegion.Vertex(0, 0),
            new PolygonRegion.Vertex(100, 0),
            new PolygonRegion.Vertex(100, 100)
        );
        PolygonRegion region = new PolygonRegion(1, vertices, 1);
        
        // Update vertex
        region.updateVertex(1, 200, 0);
        assertEquals(200, region.getX(1));
        
        // Remove vertex (should fail if < 3)
        assertFalse(region.removeVertex(0));
        assertEquals(3, region.getVertexCount());
    }

    @Test
    public void testEdgeGeneration() {
        List<PolygonRegion.Vertex> vertices = Arrays.asList(
            new PolygonRegion.Vertex(0, 0),
            new PolygonRegion.Vertex(100, 0),
            new PolygonRegion.Vertex(100, 100),
            new PolygonRegion.Vertex(0, 100)
        );
        PolygonRegion region = new PolygonRegion(1, vertices, 1);
        List<PolygonRegion.Edge> edges = region.getEdges();
        assertEquals(4, edges.size());
        assertEquals(0, edges.get(0).getStartVertexIndex());
        assertEquals(1, edges.get(0).getEndVertexIndex());
        assertEquals(3, edges.get(3).getStartVertexIndex());
        assertEquals(0, edges.get(3).getEndVertexIndex());
    }
}
