package com.treble.feasimulation.solver;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.Element;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.TriangularElement;
import com.treble.feasimulation.model.TrussElement;

import java.util.List;

/**
 * Factory class to select the appropriate FEA solver based on the element types present in the model.
 */
public class SolverFactory {

    /**
     * Determines and returns the appropriate solver for the given model.
     * 
     * @param data The FEA model data.
     * @return An instance of FEASolver.
     */
    public static FEASolver getSolver(FEAData data) {
        List<Element> elements = data.getElements();
        
        if (elements.isEmpty()) {
            return new BeamSolver(); // Default
        }

        boolean hasBeam = false;
        boolean hasTriangular = false;
        boolean hasTruss = false;

        for (Element e : elements) {
            if (e instanceof BeamElement) {
                hasBeam = true;
            } else if (e instanceof TriangularElement) {
                hasTriangular = true;
            } else if (e instanceof TrussElement) {
                hasTruss = true;
            }
        }

        if (hasTriangular) {
            return new PlaneStressSolver();
        }
        
        // If it has any beam, use BeamSolver (which handles both beam and truss)
        if (hasBeam) {
            return new BeamSolver();
        }
        
        // If it's purely truss elements
        if (hasTruss) {
            return new TrussSolver();
        }

        return new BeamSolver(); // Default fallback
    }
}
