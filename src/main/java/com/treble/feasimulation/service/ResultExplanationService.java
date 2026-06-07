package com.treble.feasimulation.service;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.Element;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.PolygonRegion;
import com.treble.feasimulation.model.TriangularElement;
import com.treble.feasimulation.solver.BeamSolver;
import com.treble.feasimulation.solver.PlaneStressResult;
import com.treble.feasimulation.solver.TrussSolver;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

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

        String materialNote = describeMaterialComparison(model);
        if (!materialNote.isEmpty()) {
            summary.append(" ").append(materialNote);
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

        String materialNote = describeMaterialComparison(model);
        if (!materialNote.isEmpty()) {
            summary.append(" ").append(materialNote);
        }

        return summary.toString();
    }

    /**
     * Produce a short plain-English explanation for plane-stress solver results.
     */
    public String explain(PlaneStressResult result, FEAData model) {
        if (result == null || result.getElementStresses() == null || result.getElementStresses().isEmpty()) {
            return "No plane-stress results to explain.";
        }

        PlaneStressResult.ElementStress worstEr = null;
        double maxVonMises = -1.0;

        for (PlaneStressResult.ElementStress er : result.getElementStresses()) {
            if (er.vonMises > maxVonMises) {
                maxVonMises = er.vonMises;
                worstEr = er;
            }
        }

        if (worstEr == null || maxVonMises < 1e-9) {
            return "No significant stresses present in plane-stress results.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("The highest stress is %.3e Pa (von Mises), found in triangular element %d.",
                maxVonMises, worstEr.elementId));

        // Find the triangle and its centroid
        TriangularElement worstTri = null;
        for (TriangularElement tri : result.getElements()) {
            if (tri.getId() == worstEr.elementId) {
                worstTri = tri;
                break;
            }
        }

        if (worstTri != null) {
            Node[] nodes = worstTri.getNodes();
            double cx = (nodes[0].getX() + nodes[1].getX() + nodes[2].getX()) / 3.0;
            double cy = (nodes[0].getY() + nodes[1].getY() + nodes[2].getY()) / 3.0;

            // Qualitative Explanations
            List<String> insights = new ArrayList<>();

            // 1. Sharp Corners detection
            boolean nearCorner = false;
            for (PolygonRegion region : model.getPolygonRegions()) {
                List<PolygonRegion.Vertex> vertices = region.getVertices();
                int n = vertices.size();
                for (int i = 0; i < n; i++) {
                    PolygonRegion.Vertex v = vertices.get(i);
                    double dist = Math.hypot(v.x - cx, v.y - cy);
                    if (dist < 20.0) { // Near a vertex (heuristic distance)
                        insights.add("Stress often increases near sharp corners because the material must redistribute internal forces to follow the abrupt change in geometry.");
                        nearCorner = true;
                        break;
                    }
                }
                if (nearCorner) break;
            }

            // 2. Load Paths
            if (!model.getPointLoads().isEmpty() || !model.getDistributedLoads().isEmpty()) {
                insights.add("High-stress regions typically form along 'load paths'—the most direct routes through the material from the applied loads to the supports.");
            }

            // 3. Holes/Stress Concentration (simplified)
            // Since we don't have explicit holes in PolygonRegion yet (it's single loop),
            // but we might have concave shapes.
            
            if (insights.isEmpty()) {
                insights.add("This area is working the hardest to maintain the structure's shape under the current loading.");
            }

            summary.append(" ").append(insights.get(0));
            
            // Failure prediction
            Material mat = findMaterialById(model, worstTri.getMaterialId());
            if (mat != null && mat.getYieldStress() > 0) {
                double utilization = maxVonMises / mat.getYieldStress();
                double sf = 1.0 / utilization;
                String state = utilization > 1.0 ? "likely to fail" : "unlikely to fail";
                summary.append(String.format(" Compared to the material yield stress (%.3e Pa), this region is %s (Safety Factor = %.2f).",
                        mat.getYieldStress(), state, sf));
            }
        }

        String materialNote = describeMaterialComparison(model);
        if (!materialNote.isEmpty()) {
            summary.append(" ").append(materialNote);
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

    private String describeMaterialComparison(FEAData model) {
        List<Material> materials = model.getMaterials();
        if (materials.isEmpty()) return "";

        Material current = materials.get(0);
        Material steel = MaterialLibrary.getPresets().get(0); // Steel
        Material wood = MaterialLibrary.getPresets().get(2); // Wood

        StringBuilder comparison = new StringBuilder();

        if (current.getName().equalsIgnoreCase("Steel")) {
            comparison.append("Using steel provides high stiffness (resistance to bending). ");
            comparison.append("If you switched to wood, the structure would deform much more because wood's Young's modulus is significantly lower.");
        } else if (current.getName().equalsIgnoreCase("Aluminium")) {
            comparison.append("Aluminium is lighter but less stiff than steel. ");
            comparison.append(String.format("It will deform about %.1f times more than steel under the same load.", 
                steel.getYoungsModulus() / current.getYoungsModulus()));
        } else if (current.getName().equalsIgnoreCase("Wood")) {
            comparison.append("Wood has a very low stiffness compared to metals. ");
            comparison.append(String.format("Expect large deformations—about %d times more than steel!", 
                (int)(steel.getYoungsModulus() / current.getYoungsModulus())));
        } else {
            // Generic comparison
            double ratio = current.getYoungsModulus() / steel.getYoungsModulus();
            if (ratio > 1.1) {
                comparison.append("This custom material is even stiffer than steel, leading to very small deformations.");
            } else if (ratio < 0.9) {
                comparison.append(String.format("This material is less stiff than steel (%.1f%% of steel's stiffness), so it will deform more easily.", ratio * 100));
            } else {
                comparison.append("This material has a stiffness similar to steel.");
            }
        }

        // Stress distribution note
        comparison.append(" Note that while the amount of deformation depends on stiffness, the way stress is distributed throughout the geometry remains largely the same for most common materials; only the magnitudes and deformation scale change.");

        return comparison.toString();
    }
}
