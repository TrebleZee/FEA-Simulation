package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;
import com.treble.feasimulation.view.BeamCanvasView;
import com.treble.feasimulation.view.MainView;
import com.treble.feasimulation.model.UnitSettings;
import com.treble.feasimulation.service.UnitConverter;
import com.treble.feasimulation.service.ResultExplanationService;
import com.treble.feasimulation.solver.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;

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
                view.getYoungsModulusField().setText(String.valueOf(newMaterial.getYoungsModulus()));
                view.getPoissonRatioField().setText(String.valueOf(newMaterial.getPoissonRatio()));
                view.getThicknessField().setText(String.valueOf(newMaterial.getThickness()));
            }
        });

        // Wire simple actions
        view.getExitItem().setOnAction(e -> Platform.exit());
        view.getClearItem().setOnAction(e -> {
            model.clear();
            canvasView.clear();
            canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.DRAW);
            view.clearStressSummary();
            view.getExplanationArea().clear();
            refreshTables();
        });

        // Element drawing
        view.getDrawElementButton().setOnAction(e -> {
            canvasView.setMode(BeamCanvasView.Mode.DRAW);
            String type = view.getElementTypeChoice().getValue();
            if ("Truss Member".equals(type)) {
                canvasView.setPlacingElementType(BeamCanvasView.ElementType.TRUSS);
            } else if ("Polygon".equals(type)) {
                canvasView.setPlacingElementType(BeamCanvasView.ElementType.POLYGON);
            } else {
                canvasView.setPlacingElementType(BeamCanvasView.ElementType.BEAM);
            }
        });

        view.getDrawPolygonButton().setOnAction(e -> {
            canvasView.setMode(BeamCanvasView.Mode.POLYGON);
            canvasView.setPlacingElementType(BeamCanvasView.ElementType.POLYGON);
        });

        view.getTrussModeButton().setOnAction(e -> {
            canvasView.setMode(BeamCanvasView.Mode.DRAW);
            canvasView.setPlacingElementType(BeamCanvasView.ElementType.TRUSS);
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

        view.getPlaceEdgeSupportButton().setOnAction(e -> {
            String sel = view.getSupportTypeChoice().getValue();
            try {
                com.treble.feasimulation.model.Support.Type t = com.treble.feasimulation.model.Support.Type.valueOf(sel);
                canvasView.setPlacingSupportType(t);
                canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.PLACE_EDGE_SUPPORT);
            } catch (Exception ex) {
                // ignore
            }
        });

        view.getApplyEdgeLoadButton().setOnAction(e -> {
            try {
                double wx = Double.parseDouble(view.getLoadWxField().getText());
                double wy = Double.parseDouble(view.getLoadWyField().getText());
                com.treble.feasimulation.model.DistributedLoad.Type type = com.treble.feasimulation.model.DistributedLoad.Type.valueOf(view.getDistributedLoadTypeChoice().getValue());
                canvasView.setPlacingEdgeLoad(wx, wy, type);
                canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.PLACE_EDGE_LOAD);
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid Load", "Please enter numeric values for edge loads.");
            }
        });

        // Point load placement
        view.getApplyLoadButton().setOnAction(e -> {
            try {
                double fx = Double.parseDouble(view.getLoadFxField().getText());
                double fy = Double.parseDouble(view.getLoadFyField().getText());
                canvasView.setPlacingLoad(fx, fy);
                canvasView.setMode(com.treble.feasimulation.view.BeamCanvasView.Mode.PLACE_LOAD);
            } catch (NumberFormatException ex) {
                // ignore invalid input
            }
        });

        view.getUpdateLoadButton().setOnAction(e -> {
            PointLoad selected = view.getPointLoadTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    double fx = Double.parseDouble(view.getLoadFxField().getText());
                    double fy = Double.parseDouble(view.getLoadFyField().getText());
                    model.removePointLoadById(selected.getId());
                    model.addPointLoad(new PointLoad(selected.getId(), selected.getNodeId(), fx, fy));
                    refreshTables();
                    canvasView.redraw();
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        });

        canvasView.setOnModelUpdate(this::refreshTables);
        canvasView.setOnElementSelected(stress -> {
            view.getSelectedSigmaXLabel().setText(String.format("%.3e Pa", stress.sigmaX));
            view.getSelectedSigmaYLabel().setText(String.format("%.3e Pa", stress.sigmaY));
            view.getSelectedTauXYLabel().setText(String.format("%.3e Pa", stress.tauXY));
            view.getSelectedVonMisesLabel().setText(String.format("%.3e Pa", stress.vonMises));
        });
        view.getLoadFxField().textProperty().addListener((obs, oldV, newV) -> updatePlacingLoad());
        view.getLoadFyField().textProperty().addListener((obs, oldV, newV) -> updatePlacingLoad());
        view.getDeformationScaleSlider().valueProperty().addListener((obs, oldV, newV) -> {
            canvasView.setLastScale(newV.doubleValue());
        });
        view.getShowDisplacementArrowsCheckbox().selectedProperty().addListener((obs, oldV, newV) -> {
            canvasView.setShowDisplacementArrows(newV);
        });

        setupTables();
        refreshTables();

        view.getRunButton().setOnAction(e -> {
            updateModelMaterialFromUI();
            runSimulation();
        });
    }

    private void updateModelMaterialFromUI() {
        Material selectedMaterial = view.getBeamMaterialChoice().getValue();
        if (selectedMaterial != null) {
            try {
                double e = Double.parseDouble(view.getYoungsModulusField().getText());
                double nu = Double.parseDouble(view.getPoissonRatioField().getText());
                double t = Double.parseDouble(view.getThicknessField().getText());
                
                Material updated = new Material(
                    selectedMaterial.getId(),
                    selectedMaterial.getName(),
                    e,
                    selectedMaterial.getDensity(),
                    selectedMaterial.getYieldStress(),
                    nu,
                    t
                );
                
                model.removeMaterialById(selectedMaterial.getId());
                model.addMaterial(updated);
            } catch (NumberFormatException ex) {
                // ignore invalid input, will use existing material properties
            }
        }
    }

    private void updatePlacingLoad() {
        try {
            double fx = Double.parseDouble(view.getLoadFxField().getText());
            double fy = Double.parseDouble(view.getLoadFyField().getText());
            canvasView.setPlacingLoad(fx, fy);
        } catch (NumberFormatException ex) {
            // ignore
        }
    }

    private void setupTables() {
        // Point Load table context menu
        ContextMenu plMenu = new ContextMenu();
        MenuItem deletePl = new MenuItem("Delete Load");
        deletePl.setOnAction(e -> {
            PointLoad selected = view.getPointLoadTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                model.removePointLoadById(selected.getId());
                refreshTables();
                canvasView.redraw();
            }
        });
        plMenu.getItems().add(deletePl);
        view.getPointLoadTable().setContextMenu(plMenu);

        // Selection listener to "edit"
        view.getPointLoadTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                view.getLoadFxField().setText(String.valueOf(newVal.getFx()));
                view.getLoadFyField().setText(String.valueOf(newVal.getFy()));
            }
        });

        // Support table context menu
        ContextMenu sMenu = new ContextMenu();
        
        // Selection listener for supports
        view.getSupportTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getType() != null) {
                view.getSupportTypeChoice().setValue(newVal.getType().name());
            }
        });

        MenuItem deleteS = new MenuItem("Delete Support");
        deleteS.setOnAction(e -> {
            Support selected = view.getSupportTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                model.removeSupportById(selected.getId());
                refreshTables();
                canvasView.redraw();
            }
        });
        sMenu.getItems().add(deleteS);
        view.getSupportTable().setContextMenu(sMenu);
    }

    private void refreshTables() {
        view.getPointLoadTable().setItems(FXCollections.observableArrayList(model.getPointLoads()));
        view.getSupportTable().setItems(FXCollections.observableArrayList(model.getSupports()));
    }

    /**
     * Run simulation with comprehensive validation.
     * Checks for: at least one support, no floating beams, and proper stiffness matrix conditions.
     */
    private void runSimulation() {
        try {
            // Perform validation
            ModelValidator.ValidationResult validation = ModelValidator.validate(model);
            if (!validation.isValid()) {
                showErrorDialog("Simulation Validation Failed", validation.getErrorMessage());
                view.clearStressSummary();
                view.getExplanationArea().setText(validation.getErrorMessage());
                return;
            }

            // Units: read user-defined length scale (meters per model unit)
            double metersPerUnit = 1.0;
            try {
                String txt = view.getMetersPerUnitField().getText();
                if (txt != null && !txt.isBlank()) metersPerUnit = Double.parseDouble(txt.trim());
                if (!(metersPerUnit > 0)) metersPerUnit = 1.0;
            } catch (Exception ignore) {
                metersPerUnit = 1.0;
            }
            UnitSettings units = new UnitSettings(metersPerUnit, 1.0);

            // Convert geometry to SI for solving
            com.treble.feasimulation.model.FEAData siModel = UnitConverter.toSI(model, units);

            // Run the solver via factory on SI-normalized model
            FEASolver solver = SolverFactory.getSolver(siModel);
            if (solver instanceof PlaneStressSolver pss) {
                pss.setMeshDensity(view.getMeshDensitySlider().getValue());
            }
            SolverResult resultSI = solver.solve(siModel);

            if (resultSI instanceof TrussSolver.Result trSI) {
                double scale = view.getDeformationScaleSlider().getValue();
                // Convert displacements to display units for visualization
                TrussSolver.Result trDisp = UnitConverter.trussResultToDisplay(trSI, units);
                canvasView.showTrussResult(trDisp, scale);
                updateTrussStressSummary(trSI);

                ResultExplanationService expl = new ResultExplanationService();
                String explanation = expl.explain(trSI, model);
                view.getExplanationArea().setText(explanation);
            } else if (resultSI instanceof BeamSolver.Result brSI) {
                double scale = view.getDeformationScaleSlider().getValue();
                BeamSolver.Result brDisp = UnitConverter.beamResultToDisplay(brSI, units);
                canvasView.showResult(brDisp, scale);
                updateStressSummary(brSI);

                ResultExplanationService expl = new ResultExplanationService();
                String explanation = expl.explain(brSI, model);
                view.getExplanationArea().setText(explanation);
            } else if (resultSI instanceof com.treble.feasimulation.solver.PlaneStressResult psrSI) {
                double scale = view.getDeformationScaleSlider().getValue();
                com.treble.feasimulation.solver.PlaneStressResult psrDisp = UnitConverter.planeStressResultToDisplay(psrSI, units);
                canvasView.showPlaneStressResult(psrDisp, scale);
                updatePlaneStressSummary(psrSI);

                ResultExplanationService expl = new ResultExplanationService();
                String explanation = expl.explain(psrSI, model);
                view.getExplanationArea().setText(explanation);
            } else {
                // PlaneStressSolver or other future solvers
                view.getExplanationArea().setText("Results for " + resultSI.getClass().getSimpleName() + " are not yet visually supported.");
            }
        } catch (UnsupportedOperationException uoe) {
            showErrorDialog("Not Supported", uoe.getMessage());
            view.getExplanationArea().setText(uoe.getMessage());
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

    private boolean isPureTruss() {
        if (model.getElements().isEmpty()) return false;
        for (com.treble.feasimulation.model.Element e : model.getElements()) {
            if (!(e instanceof com.treble.feasimulation.model.TrussElement)) return false;
        }
        return true;
    }

    private void updateTrussStressSummary(com.treble.feasimulation.solver.TrussSolver.Result result) {
        double maxTensile = result.getMaxTensileStress();
        double maxCompressive = result.getMaxCompressiveStress();
        view.getMaxTensileStressLabel().setText(
                maxTensile > 0 ? String.format("%.3e Pa", maxTensile) : "N/A");
        view.getMaxCompressiveStressLabel().setText(
                maxCompressive < 0 ? String.format("%.3e Pa", maxCompressive) : "N/A");
    }

    private void updatePlaneStressSummary(com.treble.feasimulation.solver.PlaneStressResult result) {
        double maxVonMises = result.getElementStresses().stream().mapToDouble(s -> s.vonMises).max().orElse(0);
        view.getMaxTensileStressLabel().setText(String.format("%.3e Pa", maxVonMises));
        view.getMaxCompressiveStressLabel().setText("N/A");
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
