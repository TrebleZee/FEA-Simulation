package com.treble.feasimulation.solver;

/**
 * Marker interface or base class for all solver results.
 */
public interface SolverResult {
    /**
     * @return Global displacement vector.
     */
    double[] getDisplacements();

    /**
     * Optional: support reaction forces computed by the solver.
     * Default returns an empty list for solvers that don't compute reactions.
     */
    default java.util.List<com.treble.feasimulation.solver.ReactionForce> getSupportReactions() {
        return java.util.Collections.emptyList();
    }
}
