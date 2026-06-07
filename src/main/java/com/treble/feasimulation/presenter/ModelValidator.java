package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Pre-solve validation for FEA models (beams and trusses).
 */
public final class ModelValidator {

    public static final class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }

    private ModelValidator() {
    }

    public static ValidationResult validate(FEAData model) {
        boolean hasBeamOrTrussElements = !model.getElements().isEmpty();
        boolean hasPolygonRegions = !model.getPolygonRegions().isEmpty();

        // 1) Require at least one structural definition: either line elements (beam/truss)
        //    or polygon regions for plane-stress.
        if (!hasBeamOrTrussElements && !hasPolygonRegions) {
            return ValidationResult.failure(
                "Validation failed: No structural elements defined.\n\n" +
                "Please draw at least one beam/truss element or define a polygon region " +
                "before running the simulation."
            );
        }

        // 2) Supports requirement depends on modeling approach
        if (hasBeamOrTrussElements) {
            // For line elements, require at least one node-based support as before
            if (model.getSupports().isEmpty()) {
                return ValidationResult.failure(
                    "Validation failed: No supports defined.\n\n" +
                    "The structure is statically unstable. Please add at least one support\n" +
                    "(fixed, pinned, or roller) to a node."
                );
            }
        } else if (hasPolygonRegions) {
            // For continuum polygons, accept either node-based supports (if any pre-exist)
            // or edge-based supports placed on polygon edges.
            if (model.getSupports().isEmpty() && model.getEdgeSupports().isEmpty()) {
                return ValidationResult.failure(
                    "Validation failed: No supports defined for polygon region.\n\n" +
                    "Please add at least one edge support (fixed/pinned/roller) to a polygon edge,\n" +
                    "or place a node-based support if applicable."
                );
            }
        }

        // 3) Beam/truss specific checks (floating/disconnected elements, zero-length)
        //    Only meaningful when line elements exist.
        if (hasBeamOrTrussElements) {
            String floatingError = checkForFloatingElements(model);
            if (floatingError != null) {
                return ValidationResult.failure(floatingError);
            }

            String geometryError = checkGeometry(model);
            if (geometryError != null) {
                return ValidationResult.failure(geometryError);
            }
        }

        return ValidationResult.success();
    }

    static String checkForFloatingElements(FEAData model) {
        if (model.getElements().isEmpty()) {
            return null;
        }

        Set<Integer> nodeSet = new HashSet<>();
        for (var elem : model.getElements()) {
            nodeSet.add(elem.getNodeStartId());
            nodeSet.add(elem.getNodeEndId());
        }

        if (nodeSet.isEmpty()) {
            return null;
        }

        Set<Integer> supportedNodes = new HashSet<>();
        for (var support : model.getSupports()) {
            supportedNodes.add(support.getNodeId());
        }

        Map<Integer, Integer> parent = new HashMap<>();
        for (int node : nodeSet) {
            parent.put(node, node);
        }
        for (int supportedNode : supportedNodes) {
            parent.putIfAbsent(supportedNode, supportedNode);
        }

        BiConsumer<Integer, Integer> union = (a, b) -> {
            int rootA = find(parent, a);
            int rootB = find(parent, b);
            if (rootA != rootB) {
                parent.put(rootB, rootA);
            }
        };

        for (var elem : model.getElements()) {
            union.accept(elem.getNodeStartId(), elem.getNodeEndId());
        }

        if (supportedNodes.isEmpty()) {
            return null;
        }

        Integer firstRoot = null;
        for (int supportedNode : supportedNodes) {
            int root = find(parent, supportedNode);
            if (firstRoot == null) {
                firstRoot = root;
            } else if (!firstRoot.equals(root)) {
                return "Validation failed: Disconnected structure detected.\n\n" +
                       "Not all supports are connected to the same structure.\n" +
                       "Please ensure all elements form a single connected network,\n" +
                       "or connect supports only to the same component.";
            }
        }

        for (int node : nodeSet) {
            if (!supportedNodes.contains(node)) {
                int nodeRoot = find(parent, node);
                if (firstRoot == null || nodeRoot != firstRoot) {
                    return "Validation failed: Floating elements detected.\n\n" +
                           "Some elements are not connected to any supported node.\n" +
                           "Please ensure all elements are connected to nodes with supports.";
                }
            }
        }

        return null;
    }

    static String checkGeometry(FEAData model) {
        for (var elem : model.getElements()) {
            var startNode = model.findNodeById(elem.getNodeStartId());
            var endNode = model.findNodeById(elem.getNodeEndId());

            if (startNode.isEmpty() || endNode.isEmpty()) {
                return "Validation failed: Element " + elem.getId() + " references missing node.";
            }

            double dx = endNode.get().getX() - startNode.get().getX();
            double dy = endNode.get().getY() - startNode.get().getY();
            double length = Math.hypot(dx, dy);

            if (length <= 1e-12) {
                return "Validation failed: Element " + elem.getId() + " has zero or near-zero length.\n\n" +
                       "Both nodes are at the same location. Please adjust the geometry.";
            }
        }
        return null;
    }

    private static int find(Map<Integer, Integer> parent, int node) {
        int p = parent.get(node);
        if (p == node) {
            return node;
        }
        int root = find(parent, p);
        parent.put(node, root);
        return root;
    }
}
