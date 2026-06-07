package com.treble.feasimulation.solver;

/**
 * Marker interface or base class for all solver results.
 */
public interface SolverResult {
    /**
     * @return Global displacement vector.
     */
    double[] getDisplacements();
}
