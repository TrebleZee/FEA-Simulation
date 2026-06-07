package com.treble.feasimulation.view;

import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainView {
    private final BorderPane root;
    private final MainWorkspaceController controller;

    public MainView(double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/treble/feasimulation/MainWorkspace.fxml"));
            Parent parent = loader.load();
            this.controller = loader.getController();
            this.root = (BorderPane) parent;
            // Attach stylesheet with spacing tokens
            String css = getClass().getResource("/com/treble/feasimulation/app.css").toExternalForm();
            this.root.getStylesheets().add(css);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MainWorkspace.fxml", e);
        }
    }

    public BorderPane getRoot() { return root; }
    public MenuBar getMenuBar() { return controller.getMenuBar(); }
    public Canvas getCanvas() { return controller.getCanvas(); }
    public Label getModeIndicatorLabel() { return controller.getModeIndicatorLabel(); }
    public Label getScaleIndicatorLabel() { return controller.getScaleIndicatorLabel(); }
    public MenuItem getExitItem() { return controller.getExitItem(); }
    public MenuItem getClearItem() { return controller.getClearItem(); }

    // Side panel getters (delegated)
    public ChoiceBox<String> getElementTypeChoice() { return controller.getElementTypeChoice(); }
    public Button getDrawElementButton() { return controller.getDrawElementButton(); }
    public Button getDrawPolygonButton() { return controller.getDrawPolygonButton(); }
    public Button getTrussModeButton() { return controller.getTrussModeButton(); }
    public ChoiceBox<String> getSupportTypeChoice() { return controller.getSupportTypeChoice(); }
    public ChoiceBox<com.treble.feasimulation.model.Material> getBeamMaterialChoice() { return controller.getBeamMaterialChoice(); }
    public Button getPlaceSupportButton() { return controller.getPlaceSupportButton(); }
    public Button getPlaceEdgeSupportButton() { return controller.getPlaceEdgeSupportButton(); }
    public Button getApplyEdgeLoadButton() { return controller.getApplyEdgeLoadButton(); }
    public ChoiceBox<String> getDistributedLoadTypeChoice() { return controller.getDistributedLoadTypeChoice(); }
    public TextField getLoadWxField() { return controller.getLoadWxField(); }
    public TextField getLoadWyField() { return controller.getLoadWyField(); }
    public TextField getLoadFxField() { return controller.getLoadFxField(); }
    public TextField getLoadFyField() { return controller.getLoadFyField(); }
    public TextField getYoungsModulusField() { return controller.getYoungsModulusField(); }
    public TextField getPoissonRatioField() { return controller.getPoissonRatioField(); }
    public TextField getThicknessField() { return controller.getThicknessField(); }
    public TextField getMetersPerUnitField() { return controller.getMetersPerUnitField(); }
    public Slider getMeshDensitySlider() { return controller.getMeshDensitySlider(); }
    public Slider getDeformationScaleSlider() { return controller.getDeformationScaleSlider(); }
    public CheckBox getShowDisplacementArrowsCheckbox() { return controller.getShowDisplacementArrowsCheckbox(); }
    // Layer controls
    public CheckBox getLayerNodesCheckbox() { return controller.getLayerNodesCheckbox(); }
    public CheckBox getLayerSupportsCheckbox() { return controller.getLayerSupportsCheckbox(); }
    public CheckBox getLayerLoadsCheckbox() { return controller.getLayerLoadsCheckbox(); }
    public CheckBox getLayerMeshCheckbox() { return controller.getLayerMeshCheckbox(); }
    public CheckBox getLayerStressCheckbox() { return controller.getLayerStressCheckbox(); }
    public CheckBox getLayerDeformationCheckbox() { return controller.getLayerDeformationCheckbox(); }
    public CheckBox getLayerReactionsCheckbox() { return controller.getLayerReactionsCheckbox(); }
    public Button getApplyLoadButton() { return controller.getApplyLoadButton(); }
    public Button getUpdateLoadButton() { return controller.getUpdateLoadButton(); }
    public Button getRunButton() { return controller.getRunButton(); }
    // Key results
    public Label getMaxStressValueLabel() { return controller.getMaxStressValueLabel(); }
    public Label getMaxDeflectionValueLabel() { return controller.getMaxDeflectionValueLabel(); }
    public Label getSafetyFactorValueLabel() { return controller.getSafetyFactorValueLabel(); }
    public Label getSolverStatusValueLabel() { return controller.getSolverStatusValueLabel(); }
    public Label getMaxTensileStressLabel() { return controller.getMaxTensileStressLabel(); }
    public Label getMaxCompressiveStressLabel() { return controller.getMaxCompressiveStressLabel(); }
    public Label getSelectedSigmaXLabel() { return controller.getSelectedSigmaXLabel(); }
    public Label getSelectedSigmaYLabel() { return controller.getSelectedSigmaYLabel(); }
    public Label getSelectedTauXYLabel() { return controller.getSelectedTauXYLabel(); }
    public Label getSelectedVonMisesLabel() { return controller.getSelectedVonMisesLabel(); }
    public TextArea getExplanationArea() { return controller.getExplanationArea(); }
    public TableView<PointLoad> getPointLoadTable() { return controller.getPointLoadTable(); }
    public TableView<Support> getSupportTable() { return controller.getSupportTable(); }

    // Mesh quality
    public Label getMeshElementCountLabel() { return controller.getMeshElementCountLabel(); }
    public Label getMeshAvgAspectRatioLabel() { return controller.getMeshAvgAspectRatioLabel(); }
    public Label getMeshQualityRatingLabel() { return controller.getMeshQualityRatingLabel(); }
    public Label getMeshSuggestedRefinementLabel() { return controller.getMeshSuggestedRefinementLabel(); }

    // Reactions list in Results panel
    public ListView<String> getReactionsListView() { return controller.getReactionsListView(); }

    // Properties Panel getters
    public ChoiceBox<Material> getPropMaterialChoice() { return controller.getPropMaterialChoice(); }
    public TextField getPropEField() { return controller.getPropEField(); }
    public TextField getPropAField() { return controller.getPropAField(); }
    public TextField getPropIField() { return controller.getPropIField(); }
    public TextField getPropThicknessField() { return controller.getPropThicknessField(); }
    public TextField getPropLoadMagField() { return controller.getPropLoadMagField(); }
    public TextField getPropLoadDirField() { return controller.getPropLoadDirField(); }
    public ChoiceBox<String> getPropSupportTypeChoice() { return controller.getPropSupportTypeChoice(); }

    public void clearStressSummary() {
        controller.clearStressSummary();
    }
}
