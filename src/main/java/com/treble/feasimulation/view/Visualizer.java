package com.treble.feasimulation.view;

import com.treble.feasimulation.solver.SolverResult;

/**
 * Interface for components that can visualize FEA results.
 */
public interface Visualizer {
    /**
     * Renders the visualization onto a graphics context or similar.
     * Implementation will be added in Phase 3.
     * 
     * @param result The solver result to visualize.
     * @param scale The visual scale factor.
     */
    void render(SolverResult result, double scale);
}
