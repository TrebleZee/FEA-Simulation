package com.treble.feasimulation.service;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.solver.BeamSolver;

import java.util.Optional;

/**
 * Generates a plain-English explanation of beam solver results.
 * The explanation includes: maximum bending moment, likely failure location, and qualitative reason.
 */
public class ResultExplanationService {

    /**
     * Produce a short plain-English explanation from solver results and model.
     */
    public String explain(BeamSolver.Result result, FEAData model) {
        if (result == null || result.elementResults == null || result.elementResults.isEmpty()) {
            return "No results to explain.";
        }

        double maxAbsM = 0.0;
        BeamSolver.ElementResult worstEr = null;
        BeamElement worstElem = null;
        boolean worstAtStart = true;

        // find element with largest absolute end moment
        for (BeamSolver.ElementResult er : result.elementResults) {
            double m1 = Math.abs(er.endMomentStart);
            double m2 = Math.abs(er.endMomentEnd);
            if (m1 >= maxAbsM) {
                maxAbsM = m1; worstEr = er; worstAtStart = true;
            }
            if (m2 >= maxAbsM) {
                maxAbsM = m2; worstEr = er; worstAtStart = false;
            }
        }

        if (worstEr == null) return "No bending moments present in results.";

        // locate associated element and node
        for (BeamElement be : model.getElements()) {
            if (be.getId() == worstEr.elementId) { worstElem = be; break; }
        }

        String locDesc = "an unknown location";
        String nodeCoords = "";
        if (worstElem != null) {
            int nodeId = worstAtStart ? worstElem.getNodeStartId() : worstElem.getNodeEndId();
            Optional<Node> nopt = model.findNodeById(nodeId);
            if (nopt.isPresent()) {
                Node n = nopt.get();
                nodeCoords = String.format(" (node %d at [%.1f, %.1f])", n.getId(), n.getX(), n.getY());
                locDesc = String.format("element %d, %s end%s", worstElem.getId(), (worstAtStart?"start":"end"), nodeCoords);
            } else {
                locDesc = String.format("element %d, %s end", worstElem.getId(), (worstAtStart?"start":"end"));
            }
        }

        String stressNote = "";
        if (worstEr.bendingStress != null && !Double.isNaN(worstEr.bendingStress.maxTensileStress)) {
            BeamSolver.BendingStressResult stress = worstEr.bendingStress;
            stressNote = String.format(
                    "Estimated outer-fiber distance y ≈ %.3e. Max tensile stress ≈ %.3e and max compressive stress ≈ %.3e.",
                    stress.extremeFiberDistance, stress.maxTensileStress, stress.maxCompressiveStress);
        } else if (worstElem != null) {
            double I = worstElem.getInertia();
            double A = worstElem.getArea();
            if (I > 0 && A > 0) {
                double c = Math.sqrt(3.0 * I / A);
                double estimatedStress = Math.abs(maxAbsM) * c / I;
                stressNote = String.format(
                        "Estimated bending stress ≈ %.3e using a rectangular-equivalent section depth.",
                        estimatedStress);
            } else {
                stressNote = "Section properties are incomplete, so bending stress could not be estimated.";
            }
        }

        // material info (if available)
        String materialNote = "";
        if (worstElem != null && worstElem.getMaterialId() != 0) {
            int mid = worstElem.getMaterialId();
            Optional<Material> mop = model.getMaterials().stream().filter(m -> m.getId() == mid).findFirst();
            if (mop.isPresent()) {
                Material mat = mop.get();
                materialNote = String.format(" Material: %s (E=%.3e, density=%.3f).", mat.getName(), mat.getYoungsModulus(), mat.getDensity());
            }
        }

        // likely failure explanation
        String failureReason = "High bending moment at this location indicates a risk of failure; check section modulus (I/c) and material strength to quantify risk.";
        if (worstEr.bendingStress != null && !Double.isNaN(worstEr.bendingStress.maxTensileStress)) {
            failureReason = "If the estimated tensile or compressive bending stress exceeds the material yield or allowable stress, " +
                    "plastic yielding or fracture can occur at this location. The extreme fiber experiences the largest stress.";
        }

        String summary = String.format("Maximum absolute bending moment: %.3e. The most critical location is %s. %s %s%s",
                maxAbsM, locDesc, stressNote, failureReason, materialNote);

        return summary;
    }
}
