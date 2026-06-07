package com.treble.feasimulation.service;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.Element;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.solver.BeamSolver;
import com.treble.feasimulation.solver.TrussSolver;

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
        double maxAbsAxial = 0.0;
        BeamSolver.ElementResult worstErM = null;
        BeamSolver.ElementResult worstErA = null;
        Element worstElemM = null;
        boolean worstAtStartM = true;

        for (BeamSolver.ElementResult er : result.elementResults) {
            double m1 = Math.abs(er.endMomentStart);
            double m2 = Math.abs(er.endMomentEnd);
            if (m1 >= maxAbsM) {
                maxAbsM = m1;
                worstErM = er;
                worstAtStartM = true;
            }
            if (m2 >= maxAbsM) {
                maxAbsM = m2;
                worstErM = er;
                worstAtStartM = false;
            }
            if (Math.abs(er.axialForce) > maxAbsAxial) {
                maxAbsAxial = Math.abs(er.axialForce);
                worstErA = er;
            }
        }

        StringBuilder summary = new StringBuilder();
        if (worstErM != null && maxAbsM > 1e-9) {
            worstElemM = findElementById(model, worstErM.elementId);
            String locDescM = describeLocation(model, worstElemM, worstAtStartM);
            summary.append(String.format("Maximum absolute bending moment: %.3e N*m at %s.", maxAbsM, locDescM));
        }

        if (worstErA != null && maxAbsAxial > 1e-9) {
            Element worstElemA = findElementById(model, worstErA.elementId);
            summary.append(String.format(" Maximum axial force: %.3e N (%s) in element %d.",
                    maxAbsAxial, (worstErA.axialForce >= 0 ? "tension" : "compression"), worstErA.elementId));
        }

        if (summary.length() == 0) {
            return "No significant forces or moments present in results.";
        }

        // find overall worst element for stress summary
        BeamSolver.ElementResult worstErStress = null;
        double maxStress = -1.0;
        for (BeamSolver.ElementResult er : result.elementResults) {
            double s = maxAbsoluteStress(er.bendingStress);
            if (s > maxStress) {
                maxStress = s;
                worstErStress = er;
            }
        }

        if (worstErStress != null) {
            summary.append(" ").append(describeStress(worstErStress));
        }

        String failureNote = describeFailurePrediction(result, model);
        if (!failureNote.isEmpty()) {
            summary.append(' ').append(failureNote);
        }

        return summary.toString();
    }

    /**
     * Produce a short plain-English explanation for truss solver results.
     */
    public String explain(TrussSolver.Result result, FEAData model) {
        if (result == null || result.elementResults == null || result.elementResults.isEmpty()) {
            return "No truss results to explain.";
        }

        TrussSolver.ElementResult worstEr = null;
        double maxAbsStress = -1.0;

        for (TrussSolver.ElementResult er : result.elementResults) {
            double absStress = Math.abs(er.axialStress);
            if (absStress > maxAbsStress) {
                maxAbsStress = absStress;
                worstEr = er;
            }
        }

        if (worstEr == null || maxAbsStress < 1e-9) {
            return "No significant stresses present in truss results.";
        }

        String type = worstEr.axialStress >= 0 ? "tension" : "compression";
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("The most critical member is element %d, which is in %s with a stress of %.3e Pa.",
                worstEr.elementId, type, maxAbsStress));

        Element worstElem = findElementById(model, worstEr.elementId);
        if (worstElem != null) {
            Optional<Node> n1 = model.findNodeById(worstElem.getNodeStartId());
            Optional<Node> n2 = model.findNodeById(worstElem.getNodeEndId());
            if (n1.isPresent() && n2.isPresent()) {
                summary.append(String.format(" It connects node %d to node %d.", n1.get().getId(), n2.get().getId()));
            }
        }

        String failureNote = describeTrussFailurePrediction(result, model);
        if (!failureNote.isEmpty()) {
            summary.append(" ").append(failureNote);
        }

        return summary.toString();
    }

    private String describeLocation(FEAData model, Element elem, boolean atStart) {
        if (elem == null) return "an unknown location";
        int nodeId = atStart ? elem.getNodeStartId() : elem.getNodeEndId();
        Optional<Node> nopt = model.findNodeById(nodeId);
        if (nopt.isPresent()) {
            Node n = nopt.get();
            return String.format("element %d, %s end (node %d at [%.1f, %.1f])",
                    elem.getId(), (atStart ? "start" : "end"), n.getId(), n.getX(), n.getY());
        }
        return String.format("element %d, %s end", elem.getId(), (atStart ? "start" : "end"));
    }

    private String describeStress(BeamSolver.ElementResult worstEr) {
        if (worstEr.bendingStress != null && !Double.isNaN(worstEr.bendingStress.maxTensileStress)) {
            BeamSolver.BendingStressResult stress = worstEr.bendingStress;
            return String.format(
                    "Max tensile stress is about %.3e Pa and max compressive stress is about %.3e Pa.",
                    stress.maxTensileStress, stress.maxCompressiveStress);
        }
        return "Stress could not be estimated.";
    }

    private String describeFailurePrediction(BeamSolver.Result result, FEAData model) {
        BeamSolver.ElementResult criticalEr = null;
        Element criticalElem = null;
        Material criticalMat = null;
        double criticalStress = Double.NaN;
        double criticalYield = Double.NaN;
        double criticalUtilization = Double.NEGATIVE_INFINITY;

        for (BeamSolver.ElementResult er : result.elementResults) {
            if (er == null || er.bendingStress == null) {
                continue;
            }

            Element elem = findElementById(model, er.elementId);
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
            return "Failure prediction: Yield stress undefined.";
        }

        double safetyFactor = 1.0 / criticalUtilization;
        String state = criticalUtilization > 1.0
                ? "likely to fail"
                : "unlikely to fail";
        String governingStressType = governingStressType(criticalEr.bendingStress);

        return String.format(
                "Failure prediction: element %d reaches about %.3e Pa in %s versus yield of %.3e Pa (SF=%.2f), beam is %s.",
                criticalElem.getId(), criticalStress, governingStressType, criticalYield, safetyFactor, state);
    }

    private String describeTrussFailurePrediction(TrussSolver.Result result, FEAData model) {
        TrussSolver.ElementResult criticalEr = null;
        double criticalUtilization = -1.0;
        Material criticalMat = null;

        for (TrussSolver.ElementResult er : result.elementResults) {
            Element elem = findElementById(model, er.elementId);
            if (elem == null) continue;
            Material mat = findMaterialById(model, elem.getMaterialId());
            if (mat == null || mat.getYieldStress() <= 0) continue;

            double utilization = Math.abs(er.axialStress) / mat.getYieldStress();
            if (utilization > criticalUtilization) {
                criticalUtilization = utilization;
                criticalEr = er;
                criticalMat = mat;
            }
        }

        if (criticalEr == null) return "";

        double safetyFactor = 1.0 / criticalUtilization;
        String state = criticalUtilization > 1.0 ? "likely to fail" : "unlikely to fail";
        String type = criticalEr.axialStress >= 0 ? "tension" : "compression";

        return String.format("Failure prediction: element %d reaches %.2f%% of its yield stress (%.3e Pa) in %s. The structure is %s (SF=%.2f).",
                criticalEr.elementId, criticalUtilization * 100, criticalMat.getYieldStress(), type, state, safetyFactor);
    }

    private String describeMaterial(Element worstElem, FEAData model) {
        if (worstElem != null && worstElem.getMaterialId() != 0) {
            Material mat = findMaterialById(model, worstElem.getMaterialId());
            if (mat != null) {
                return String.format("Material: %s (E=%.3e).", mat.getName(), mat.getYoungsModulus());
            }
        }
        return "";
    }

    private Element findElementById(FEAData model, int elementId) {
        return model.findElementById(elementId).orElse(null);
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
