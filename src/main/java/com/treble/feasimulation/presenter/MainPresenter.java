package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;
import com.treble.feasimulation.view.BeamCanvasView;
import com.treble.feasimulation.view.MainView;
import com.treble.feasimulation.service.ResultExplanationService;
import com.treble.feasimulation.solver.BeamSolver;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class MainPresenter implements Presenter {
    private final FEAData model;
    private final MainView view;
    private final BeamCanvasView canvasView;

    public MainPresenter(FEAData model, MainView view) {
        this.model = model;
        this.view = view;
        this.canvasView = new BeamCanvasView(view.getCanvas(), model);

        for (Material material : MaterialLibrary.getPresets()) {
            boolean present = model.getMaterials().stream().anyMatch(existing -> existing.getId() == material.getId());
            if (!present) {
                model.addMaterial(material);
            }
        }
        canvasView.setPlacingBeamMaterialId(view.getBeamMaterialChoice().getValue().getId());
        view.getBeamMaterialChoice().getSelectionModel().selectedItemProperty().addListener((obs, oldMaterial, newMaterial) -> {
            if (newMaterial != null) {
                canvasView.setPlacingBeamMaterialId(newMaterial.getId());
            }
        });

        // Wire simple actions
        view.getExitItem().setOnAction(e -> Platform.exit());
        view.getClearItem().setOnAction(e -> {
            canvasView.clear();
            canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.DRAW);
            view.clearStressSummary();
            view.getExplanationArea().clear();
        });

        // Support placement
        view.getPlaceSupportButton().setOnAction(e -> {
            String sel = view.getSupportTypeChoice().getValue();
            try {
                com.treble.feasimulation.model.Support.Type t = com.treble.feasimulation.model.Support.Type.valueOf(sel);
                canvasView.setPlacingSupportType(t);
                canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.PLACE_SUPPORT);
            } catch (Exception ex) {
                // ignore
            }
        });

        // Point load placement
        view.getApplyLoadButton().setOnAction(e -> {
            try {
                double mag = Double.parseDouble(view.getLoadMagnitudeField().getText());
                double ang = Double.parseDouble(view.getLoadAngleField().getText());
                canvasView.setPlacingLoad(mag, ang);
                canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.PLACE_LOAD);
            } catch (NumberFormatException ex) {
                // ignore invalid input
            }
        });

        // Run simulation
        view.getRunButton().setOnAction(e -> runSimulation());
    }

    /**
     * Run simulation with comprehensive validation.
     * Checks for: at least one support, no floating beams, and proper stiffness matrix conditions.
     */
    private void runSimulation() {
        try {
            // Perform validation
            ValidationResult validation = validateModel();
            if (!validation.isValid()) {
                showErrorDialog("Simulation Validation Failed", validation.getErrorMessage());
                view.clearStressSummary();
                view.getExplanationArea().setText(validation.getErrorMessage());
                return;
            }

            // Run the solver
            BeamSolver solver = new BeamSolver();
            BeamSolver.Result r = solver.solve(model);
            
            // choose a visual scale (pixels per meter). Provide a simple heuristic
            double scale = 100.0; // user-adjustable later
            canvasView.showResult(r, scale);
            updateStressSummary(r);

            // generate plain-English explanation and show in side panel
            ResultExplanationService expl = new ResultExplanationService();
            String explanation = expl.explain(r, model);
            view.getExplanationArea().setText(explanation);
        } catch (IllegalStateException ise) {
            // solver-level numerical issues (singularity, etc.)
            view.clearStressSummary();
            String msg = "Simulation failed: Stiffness matrix is singular or ill-conditioned.\n\n" +
                    "Possible causes:\n" +
                    "• Missing or insufficient supports\n" +
                    "• Floating (disconnected) beam elements\n" +
                    "• Beams with zero or near-zero length\n\n" +
                    "Details: " + ise.getMessage();
            showErrorDialog("Stiffness Matrix Error", msg);
            view.getExplanationArea().setText(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
            view.clearStressSummary();
            String msg = "Simulation failed: " + ex.getMessage();
            showErrorDialog("Simulation Error", msg);
            view.getExplanationArea().setText(msg);
        }
    }

    /**
     * Validation result holder.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        boolean isValid() {
            return valid;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validate the model before running simulation.
     * Checks for:
     * 1. At least one element
     * 2. At least one support
     * 3. No floating (disconnected) beams
     * 4. Valid geometry (no zero-length elements)
     */
    private ValidationResult validateModel() {
        // Check: at least one element
        if (model.getElements().isEmpty()) {
            return ValidationResult.failure(
                    "Validation failed: No beam elements defined.\n\n" +
                    "Please draw at least one beam on the canvas before running the simulation."
            );
        }

        // Check: at least one support
        if (model.getSupports().isEmpty()) {
            return ValidationResult.failure(
                    "Validation failed: No supports defined.\n\n" +
                    "The structure is statically unstable. Please add at least one support\n" +
                    "(fixed, pinned, or roller) to a node."
            );
        }

        // Check: no floating beams (all elements must be connected)
        String floatingBeamError = checkForFloatingBeams();
        if (floatingBeamError != null) {
            return ValidationResult.failure(floatingBeamError);
        }

        // Check: valid geometry (no zero-length elements)
        String geometryError = checkGeometry();
        if (geometryError != null) {
            return ValidationResult.failure(geometryError);
        }

        return ValidationResult.success();
    }

    /**
     * Check if any beams are floating (disconnected from the rest of the structure).
     * Uses union-find to verify all supported nodes are connected.
     * Returns null if valid, or an error message if floating beams detected.
     */
    private String checkForFloatingBeams() {
        if (model.getElements().isEmpty()) {
            return null; // already checked elsewhere
        }

        // Collect all nodes involved in elements
        java.util.Set<Integer> nodeSet = new java.util.HashSet<>();
        for (var elem : model.getElements()) {
            nodeSet.add(elem.getNodeStartId());
            nodeSet.add(elem.getNodeEndId());
        }

        if (nodeSet.isEmpty()) {
            return null;
        }

        // Collect all supported nodes
        java.util.Set<Integer> supportedNodes = new java.util.HashSet<>();
        for (var support : model.getSupports()) {
            supportedNodes.add(support.getNodeId());
        }

        // Build connectivity using union-find
        java.util.Map<Integer, Integer> parent = new java.util.HashMap<>();
        for (int node : nodeSet) {
            parent.put(node, node);
        }

        java.util.function.BiConsumer<Integer, Integer> union = (a, b) -> {
            int rootA = find(parent, a);
            int rootB = find(parent, b);
            if (rootA != rootB) {
                parent.put(rootB, rootA);
            }
        };

        // Union all connected elements
        for (var elem : model.getElements()) {
            union.accept(elem.getNodeStartId(), elem.getNodeEndId());
        }

        // Check if all supported nodes belong to the same component
        if (supportedNodes.isEmpty()) {
            return null;
        }

        Integer firstRoot = null;
        for (int supportedNode : supportedNodes) {
            int root = find(parent, supportedNode);
            if (firstRoot == null) {
                firstRoot = root;
            } else if (firstRoot != root) {
                return "Validation failed: Floating beam detected.\n\n" +
                       "Not all supports are connected to the same structure.\n" +
                       "Please ensure all beams form a single connected network,\n" +
                       "or connect supports only to the same component.";
            }
        }

        // Check if any unsupported nodes exist in a separate component
        for (int node : nodeSet) {
            if (!supportedNodes.contains(node)) {
                int nodeRoot = find(parent, node);
                if (firstRoot == null || nodeRoot != firstRoot) {
                    return "Validation failed: Floating beam detected.\n\n" +
                           "Some beam elements are not connected to any supported node.\n" +
                           "Please ensure all beams are connected to nodes with supports.";
                }
            }
        }

        return null; // validation passed
    }

    /**
     * Union-find helper: get the root of a node.
     */
    private int find(java.util.Map<Integer, Integer> parent, int node) {
        int p = parent.get(node);
        if (p == node) {
            return node;
        }
        int root = find(parent, p);
        parent.put(node, root); // path compression
        return root;
    }

    /**
     * Check for geometry issues like zero-length elements.
     * Returns null if valid, or an error message if issues detected.
     */
    private String checkGeometry() {
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
        return null; // validation passed
    }

    /**
     * Show a user-friendly error dialog.
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void start() {
        // placeholder: load model, initialize state
    }

    private void updateStressSummary(BeamSolver.Result result) {
        BeamSolver.ElementResult maxTensileElement = null;
        BeamSolver.ElementResult maxCompressiveElement = null;
        double maxTensile = Double.NEGATIVE_INFINITY;
        double maxCompressive = Double.POSITIVE_INFINITY;

        for (BeamSolver.ElementResult er : result.elementResults) {
            if (er == null || er.bendingStress == null) {
                continue;
            }

            double tensile = er.bendingStress.maxTensileStress;
            if (!Double.isNaN(tensile) && tensile > maxTensile) {
                maxTensile = tensile;
                maxTensileElement = er;
            }

            double compressive = er.bendingStress.maxCompressiveStress;
            if (!Double.isNaN(compressive) && compressive < maxCompressive) {
                maxCompressive = compressive;
                maxCompressiveElement = er;
            }
        }

        if (maxTensileElement != null) {
            view.getMaxTensileStressLabel().setText(String.format("%.3e Pa (element %d)",
                    maxTensile, maxTensileElement.elementId));
        } else {
            view.getMaxTensileStressLabel().setText("N/A");
        }

        if (maxCompressiveElement != null) {
            view.getMaxCompressiveStressLabel().setText(String.format("%.3e Pa (element %d)",
                    maxCompressive, maxCompressiveElement.elementId));
        } else {
            view.getMaxCompressiveStressLabel().setText("N/A");
        }
    }
}
