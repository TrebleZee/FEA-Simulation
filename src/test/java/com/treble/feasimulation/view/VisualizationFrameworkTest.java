package com.treble.feasimulation.view;

import com.treble.feasimulation.solver.SolverResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class VisualizationFrameworkTest {

    @Test
    public void testVisualizerStructure() {
        AtomicBoolean rendered = new AtomicBoolean(false);
        Visualizer mockVisualizer = (result, scale) -> rendered.set(true);

        SolverResult mockResult = () -> new double[0];
        mockVisualizer.render(mockResult, 100.0);

        assertTrue(rendered.get(), "Visualizer render method should be callable");
    }

    @Test
    public void testSpecificPlotters() {
        // Just verify they can be instantiated and implement the interface
        Visualizer contour = new ContourPlotter();
        Visualizer heatmap = new HeatmapPlotter();
        Visualizer shader = new StressShader();

        assertNotNull(contour);
        assertNotNull(heatmap);
        assertNotNull(shader);
        
        // Call render (should do nothing but not crash)
        contour.render(null, 1.0);
        heatmap.render(null, 1.0);
        shader.render(null, 1.0);
    }
}
