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
                maxAbsM = m1;
                worstEr = er;
                worstAtStart = true;
            }
            if (m2 >= maxAbsM) {
                maxAbsM = m2;
                worstEr = er;
                worstAtStart = false;
            }
        }

        if (worstEr == null) {
            return "No bending moments present in results.";
        }

        // locate associated element and node
        worstElem = findElementById(model, worstEr.elementId);

        String locDesc = "an unknown location";
        if (worstElem != null) {
            int nodeId = worstAtStart ? worstElem.getNodeStartId() : worstElem.getNodeEndId();
            Optional<Node> nopt = model.findNodeById(nodeId);
            if (nopt.isPresent()) {
                Node n = nopt.get();
                locDesc = String.format("element %d, %s end (node %d at [%.1f, %.1f])",
                        worstElem.getId(), (worstAtStart ? "start" : "end"), n.getId(), n.getX(), n.getY());
            } else {
                locDesc = String.format("element %d, %s end", worstElem.getId(), (worstAtStart ? "start" : "end"));
            }
        }

        String stressNote = describeStress(worstEr);
        String failureNote = describeFailurePrediction(result, model);
        String materialNote = describeMaterial(worstElem, model);

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Maximum absolute bending moment: %.3e. The most critical location is %s.",
                maxAbsM, locDesc));
        if (!stressNote.isEmpty()) {
            summary.append(' ').append(stressNote);
        }
        if (!failureNote.isEmpty()) {
            summary.append(' ').append(failureNote);
        }
        if (!materialNote.isEmpty()) {
            summary.append(' ').append(materialNote);
        }

        return summary.toString();
    }

    private String describeStress(BeamSolver.ElementResult worstEr) {
        if (worstEr.bendingStress != null && !Double.isNaN(worstEr.bendingStress.maxTensileStress)) {
            BeamSolver.BendingStressResult stress = worstEr.bendingStress;
            return String.format(
                    "Estimated outer-fiber distance y is about %.3e m. Max tensile stress is about %.3e Pa and max compressive stress is about %.3e Pa.",
                    stress.extremeFiberDistance, stress.maxTensileStress, stress.maxCompressiveStress);
        }
        return "Bending stress could not be estimated from the available section properties.";
    }

    private String describeFailurePrediction(BeamSolver.Result result, FEAData model) {
        BeamSolver.ElementResult criticalEr = null;
        BeamElement criticalElem = null;
        Material criticalMat = null;
        double criticalStress = Double.NaN;
        double criticalYield = Double.NaN;
        double criticalUtilization = Double.NEGATIVE_INFINITY;

        for (BeamSolver.ElementResult er : result.elementResults) {
            if (er == null || er.bendingStress == null) {
                continue;
            }

            BeamElement elem = findElementById(model, er.elementId);
            if (elem == null || elem.getMaterialId() == 0) {
                continue;
            }

            Material mat = findMaterialById(model, elem.getMaterialId());
            if (mat == null) {
                continue;
            }

            double yieldStress = mat.getYieldStress();
            if (!Double.isFinite(yieldStress) || yieldStress <= 0.0) {
                continue;
            }

            double elementStress = maxAbsoluteStress(er.bendingStress);
            if (!Double.isFinite(elementStress)) {
                continue;
            }

            double utilization = elementStress / yieldStress;
            if (utilization > criticalUtilization) {
                criticalUtilization = utilization;
                criticalEr = er;
                criticalElem = elem;
                criticalMat = mat;
                criticalStress = elementStress;
                criticalYield = yieldStress;
            }
        }

        if (criticalEr == null || criticalElem == null || criticalMat == null) {
            return "Failure prediction: material yield stress is not defined for the available materials, so failure cannot be assessed.";
        }

        double safetyFactor = 1.0 / criticalUtilization;
        String state = criticalUtilization > 1.0
                ? "likely to fail by yielding"
                : "unlikely to fail under this load";
        String governingStressType = governingStressType(criticalEr.bendingStress);

        return String.format(
                "Failure prediction: element %d reaches about %.3e Pa in %s versus a yield stress of %.3e Pa (utilization %.2f, safety factor %.2f), so the beam is %s.",
                criticalElem.getId(), criticalStress, governingStressType, criticalYield, criticalUtilization, safetyFactor, state);
    }

    private String describeMaterial(BeamElement worstElem, FEAData model) {
        if (worstElem != null && worstElem.getMaterialId() != 0) {
            Material mat = findMaterialById(model, worstElem.getMaterialId());
            if (mat != null) {
                if (Double.isFinite(mat.getYieldStress()) && mat.getYieldStress() > 0.0) {
                    return String.format("Material: %s (E=%.3e, density=%.3f, yield=%.3e).",
                            mat.getName(), mat.getYoungsModulus(), mat.getDensity(), mat.getYieldStress());
                }
                return String.format("Material: %s (E=%.3e, density=%.3f).",
                        mat.getName(), mat.getYoungsModulus(), mat.getDensity());
            }
        }
        return "";
    }

    private BeamElement findElementById(FEAData model, int elementId) {
        for (BeamElement be : model.getElements()) {
            if (be.getId() == elementId) {
                return be;
            }
        }
        return null;
    }

    private Material findMaterialById(FEAData model, int materialId) {
        for (Material mat : model.getMaterials()) {
            if (mat.getId() == materialId) {
                return mat;
            }
        }
        return null;
    }

    private double maxAbsoluteStress(BeamSolver.BendingStressResult stress) {
        if (stress == null) {
            return Double.NaN;
        }
        double tensile = Math.abs(stress.maxTensileStress);
        double compressive = Math.abs(stress.maxCompressiveStress);
        return Math.max(tensile, compressive);
    }

    private String governingStressType(BeamSolver.BendingStressResult stress) {
        if (stress == null) {
            return "bending";
        }
        double tensile = Math.abs(stress.maxTensileStress);
        double compressive = Math.abs(stress.maxCompressiveStress);
        if (tensile >= compressive) {
            return "tension";
        }
        return "compression";
    }
}
