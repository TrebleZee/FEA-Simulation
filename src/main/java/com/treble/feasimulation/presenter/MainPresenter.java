package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;
import com.treble.feasimulation.view.BeamCanvasView;
import com.treble.feasimulation.view.MainView;
import com.treble.feasimulation.service.ResultExplanationService;
import com.treble.feasimulation.solver.BeamSolver;
import javafx.application.Platform;

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
        view.getRunButton().setOnAction(e -> {
            try {
                // basic validation
                if (model.getElements().isEmpty()) {
                    view.clearStressSummary();
                    view.getExplanationArea().setText("No elements defined. Draw beams before running the simulation.");
                    return;
                }
                if (model.getSupports().isEmpty()) {
                    view.getExplanationArea().setText("Warning: No supports defined. The structure is likely unstable and solver may fail.");
                    // continue to let solver attempt but user is warned
                }

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
                // solver-level numerical issues
                view.clearStressSummary();
                String msg = "Simulation failed: numerical issue - " + ise.getMessage();
                view.getExplanationArea().setText(msg);
            } catch (Exception ex) {
                ex.printStackTrace();
                view.clearStressSummary();
                view.getExplanationArea().setText("Simulation failed: " + ex.getMessage());
            }
        });
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
