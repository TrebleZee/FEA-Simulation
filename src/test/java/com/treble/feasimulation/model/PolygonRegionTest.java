package com.treble.feasimulation.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PolygonRegionTest {

    @Test
    public void requiresAtLeastThreeVertices() {
        assertThrows(IllegalArgumentException.class, () ->
                new PolygonRegion(1, List.of(
                        new PolygonRegion.Vertex(0, 0),
                        new PolygonRegion.Vertex(1, 0)
                ), 1));
    }

    @Test
    public void fromCoordinatesCreatesRegion() {
        PolygonRegion region = PolygonRegion.fromCoordinates(2, new double[][]{
                {0, 0}, {4, 0}, {4, 3}, {0, 3}
        }, 5);

        assertEquals(2, region.getId());
        assertEquals(5, region.getMaterialId());
        assertEquals(4, region.getVertexCount());
        assertEquals(4, region.getX(2), 1e-9);
    }

    @Test
    public void copyPreservesGeometry() {
        PolygonRegion original = new PolygonRegion(1, List.of(
                new PolygonRegion.Vertex(1, 2),
                new PolygonRegion.Vertex(3, 4),
                new PolygonRegion.Vertex(5, 6)
        ), 7);

        PolygonRegion copy = original.copy();
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getVertexCount(), copy.getVertexCount());
        assertEquals(3, copy.getX(1), 1e-9);
    }
}
