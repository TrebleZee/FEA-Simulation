package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.FEAData;

/**
 * Common interface for all FEA solvers.
 */
public interface FEASolver {
    /**
     * Solve the FEA problem defined in the provided data.
     * 
     * @param data The FEA model data.
     * @return A SolverResult containing displacements and element-specific results.
     */
    SolverResult solve(FEAData data);
}
