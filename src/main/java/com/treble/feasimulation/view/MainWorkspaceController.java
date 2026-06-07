package com.treble.feasimulation.view;

import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class MainWorkspaceController implements Initializable {

    // Top
    @FXML private MenuBar menuBar;
    @FXML private MenuItem exitItem;
    @FXML private MenuItem clearItem;

    // Center canvas
    @FXML private StackPane canvasContainer; // optional holder if needed
    @FXML private Canvas canvas;
    @FXML private Label modeIndicatorLabel;
    @FXML private Label scaleIndicatorLabel;

    // Left tools panel controls
    @FXML private ChoiceBox<String> elementTypeChoice;
    @FXML private Button drawElementButton;
    @FXML private Button drawPolygonButton;
    @FXML private Button trussModeButton;

    @FXML private ChoiceBox<String> supportTypeChoice;
    @FXML private Button placeSupportButton;
    @FXML private Button placeEdgeSupportButton;

    @FXML private ChoiceBox<String> distributedLoadTypeChoice;
    @FXML private TextField loadWxField;
    @FXML private TextField loadWyField;
    @FXML private Button applyEdgeLoadButton;

    @FXML private ChoiceBox<Material> beamMaterialChoice;
    @FXML private TextField youngsModulusField;
    @FXML private TextField poissonRatioField;
    @FXML private TextField thicknessField;

    @FXML private TextField metersPerUnitField;
    @FXML private Slider meshDensitySlider;
    @FXML private Slider deformationScaleSlider;
    @FXML private CheckBox showDisplacementArrowsCheckbox;

    // Layer visibility
    @FXML private CheckBox layerNodesCheckbox;
    @FXML private CheckBox layerSupportsCheckbox;
    @FXML private CheckBox layerLoadsCheckbox;
    @FXML private CheckBox layerMeshCheckbox;
    @FXML private CheckBox layerStressCheckbox;
    @FXML private CheckBox layerDeformationCheckbox;
    @FXML private CheckBox layerReactionsCheckbox;

    @FXML private TextField loadFxField;
    @FXML private TextField loadFyField;
    @FXML private Button applyLoadButton;
    @FXML private Button updateLoadButton;

    // Tables
    @FXML private TabPane tablesTabPane;
    @FXML private Tab loadsTab;
    @FXML private Tab supportsTab;
    @FXML private TableView<PointLoad> pointLoadTable;
    @FXML private TableColumn<PointLoad, Integer> plIdCol;
    @FXML private TableColumn<PointLoad, Integer> plNodeCol;
    @FXML private TableColumn<PointLoad, Double> plFxCol;
    @FXML private TableColumn<PointLoad, Double> plFyCol;
    @FXML private TableView<Support> supportTable;
    @FXML private TableColumn<Support, Integer> sIdCol;
    @FXML private TableColumn<Support, Integer> sNodeCol;
    @FXML private TableColumn<Support, String> sTypeCol;

    // Right results and insights
    @FXML private Button runButton;
    @FXML private Label maxStressValueLabel;
    @FXML private Label maxDeflectionValueLabel;
    @FXML private Label safetyFactorValueLabel;
    @FXML private Label solverStatusValueLabel;
    @FXML private Label maxTensileStressLabel;
    @FXML private Label maxCompressiveStressLabel;
    @FXML private Label selectedSigmaXLabel;
    @FXML private Label selectedSigmaYLabel;
    @FXML private Label selectedTauXYLabel;
    @FXML private Label selectedVonMisesLabel;
    @FXML private ListView<String> reactionsListView;
    @FXML private TextArea explanationArea;

    // Mesh quality
    @FXML private Label meshElementCountLabel;
    @FXML private Label meshAvgAspectRatioLabel;
    @FXML private Label meshQualityRatingLabel;
    @FXML private Label meshSuggestedRefinementLabel;

    // Properties Panel controls (for selected item)
    @FXML private ChoiceBox<Material> propMaterialChoice;
    @FXML private TextField propEField;
    @FXML private TextField propAField;
    @FXML private TextField propIField;
    @FXML private TextField propThicknessField;
    @FXML private TextField propLoadMagField;
    @FXML private TextField propLoadDirField;
    @FXML private ChoiceBox<String> propSupportTypeChoice;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize choices
        elementTypeChoice.getItems().setAll("Beam", "Truss Member", "Polygon");
        elementTypeChoice.setValue("Beam");

        supportTypeChoice.getItems().setAll("FIXED", "PINNED", "ROLLER");
        supportTypeChoice.setValue("FIXED");

        distributedLoadTypeChoice.getItems().setAll("DIRECTIONAL", "UNIFORM");
        distributedLoadTypeChoice.setValue("DIRECTIONAL");

        // Materials
        beamMaterialChoice.getItems().setAll(MaterialLibrary.getPresets());
        beamMaterialChoice.setValue(MaterialLibrary.getDefaultMaterial());

        // Material numeric defaults
        youngsModulusField.setText(String.valueOf(MaterialLibrary.getDefaultMaterial().getYoungsModulus()));
        poissonRatioField.setText(String.valueOf(MaterialLibrary.getDefaultMaterial().getPoissonRatio()));
        thicknessField.setText(String.valueOf(MaterialLibrary.getDefaultMaterial().getThickness()));

        // Indicators defaults
        if (modeIndicatorLabel != null) modeIndicatorLabel.setText("MODE: DRAWING BEAMS");
        if (scaleIndicatorLabel != null) scaleIndicatorLabel.setText("100 px = 1.0 m");

        // Default key results placeholders
        if (maxStressValueLabel != null) maxStressValueLabel.setText("—");
        if (maxDeflectionValueLabel != null) maxDeflectionValueLabel.setText("—");
        if (safetyFactorValueLabel != null) safetyFactorValueLabel.setText("—");
        if (solverStatusValueLabel != null) solverStatusValueLabel.setText("Ready");

        // Mesh quality placeholders
        if (meshElementCountLabel != null) meshElementCountLabel.setText("—");
        if (meshAvgAspectRatioLabel != null) meshAvgAspectRatioLabel.setText("—");
        if (meshQualityRatingLabel != null) meshQualityRatingLabel.setText("—");
        if (meshSuggestedRefinementLabel != null) meshSuggestedRefinementLabel.setText("—");

        // Units
        metersPerUnitField.setText("1.0");

        // Sliders
        meshDensitySlider.setMin(0.1);
        meshDensitySlider.setMax(10.0);
        meshDensitySlider.setValue(1.0);
        meshDensitySlider.setShowTickLabels(true);
        meshDensitySlider.setShowTickMarks(true);
        meshDensitySlider.setMajorTickUnit(2.0);
        meshDensitySlider.setMinorTickCount(5);
        meshDensitySlider.setBlockIncrement(0.5);

        deformationScaleSlider.setMin(0.0);
        deformationScaleSlider.setMax(1000.0);
        deformationScaleSlider.setValue(1.0);
        deformationScaleSlider.setShowTickLabels(true);
        deformationScaleSlider.setShowTickMarks(true);
        deformationScaleSlider.setMajorTickUnit(200.0);
        deformationScaleSlider.setMinorTickCount(5);
        deformationScaleSlider.setBlockIncrement(10.0);

        // Load defaults
        loadWxField.setText("0");
        loadWyField.setText("-1000");
        loadFxField.setText("0");
        loadFyField.setText("-1000");

        // CheckBox default
        showDisplacementArrowsCheckbox.setSelected(false);

        // Tables columns
        plIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        plNodeCol.setCellValueFactory(new PropertyValueFactory<>("nodeId"));
        plFxCol.setCellValueFactory(new PropertyValueFactory<>("fx"));
        plFyCol.setCellValueFactory(new PropertyValueFactory<>("fy"));
        pointLoadTable.setPrefHeight(150);

        sIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        sNodeCol.setCellValueFactory(new PropertyValueFactory<>("nodeId"));
        sTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        supportTable.setPrefHeight(150);

        // Results defaults
        clearStressSummary();

        // Bind canvas to its container for dominance
        canvas.widthProperty().addListener((obs, oldV, newV) -> {}); // ensure property is live
        canvas.heightProperty().addListener((obs, oldV, newV) -> {});
        // If the canvas is nested in a StackPane, bind its size to fill
        // We try to find parent StackPane if available
        if (canvas.getParent() instanceof StackPane sp) {
            canvas.widthProperty().bind(sp.widthProperty());
            canvas.heightProperty().bind(sp.heightProperty());
        }

        // Initialize Properties Panel defaults
        if (propMaterialChoice != null) {
            propMaterialChoice.getItems().setAll(MaterialLibrary.getPresets());
            propMaterialChoice.setValue(MaterialLibrary.getDefaultMaterial());
        }
        if (propSupportTypeChoice != null) {
            propSupportTypeChoice.getItems().setAll("FIXED", "PINNED", "ROLLER");
            propSupportTypeChoice.setValue("FIXED");
        }
        if (propEField != null) propEField.setText(String.valueOf(MaterialLibrary.getDefaultMaterial().getYoungsModulus()));
        if (propAField != null) propAField.setText("0.0");
        if (propIField != null) propIField.setText("0.0");
        if (propThicknessField != null) propThicknessField.setText(String.valueOf(MaterialLibrary.getDefaultMaterial().getThickness()));
        if (propLoadMagField != null) propLoadMagField.setText("0.0");
        if (propLoadDirField != null) propLoadDirField.setText("-90.0");
    }

    public void clearStressSummary() {
        maxTensileStressLabel.setText("N/A");
        maxCompressiveStressLabel.setText("N/A");
        selectedSigmaXLabel.setText("N/A");
        selectedSigmaYLabel.setText("N/A");
        selectedTauXYLabel.setText("N/A");
        selectedVonMisesLabel.setText("N/A");
    }

    // Expose accessors for MainView shim
    public MenuBar getMenuBar() { return menuBar; }
    public MenuItem getExitItem() { return exitItem; }
    public MenuItem getClearItem() { return clearItem; }
    public Canvas getCanvas() { return canvas; }
    public Label getModeIndicatorLabel() { return modeIndicatorLabel; }
    public Label getScaleIndicatorLabel() { return scaleIndicatorLabel; }
    public ChoiceBox<String> getElementTypeChoice() { return elementTypeChoice; }
    public ChoiceBox<String> getSupportTypeChoice() { return supportTypeChoice; }
    public ChoiceBox<Material> getBeamMaterialChoice() { return beamMaterialChoice; }
    public Button getDrawElementButton() { return drawElementButton; }
    public Button getDrawPolygonButton() { return drawPolygonButton; }
    public Button getTrussModeButton() { return trussModeButton; }
    public Button getPlaceSupportButton() { return placeSupportButton; }
    public Button getPlaceEdgeSupportButton() { return placeEdgeSupportButton; }
    public Button getApplyEdgeLoadButton() { return applyEdgeLoadButton; }
    public ChoiceBox<String> getDistributedLoadTypeChoice() { return distributedLoadTypeChoice; }
    public TextField getLoadWxField() { return loadWxField; }
    public TextField getLoadWyField() { return loadWyField; }
    public TextField getLoadFxField() { return loadFxField; }
    public TextField getLoadFyField() { return loadFyField; }
    public TextField getYoungsModulusField() { return youngsModulusField; }
    public TextField getPoissonRatioField() { return poissonRatioField; }
    public TextField getThicknessField() { return thicknessField; }
    public TextField getMetersPerUnitField() { return metersPerUnitField; }
    public Slider getMeshDensitySlider() { return meshDensitySlider; }
    public Slider getDeformationScaleSlider() { return deformationScaleSlider; }
    public CheckBox getShowDisplacementArrowsCheckbox() { return showDisplacementArrowsCheckbox; }
    public CheckBox getLayerNodesCheckbox() { return layerNodesCheckbox; }
    public CheckBox getLayerSupportsCheckbox() { return layerSupportsCheckbox; }
    public CheckBox getLayerLoadsCheckbox() { return layerLoadsCheckbox; }
    public CheckBox getLayerMeshCheckbox() { return layerMeshCheckbox; }
    public CheckBox getLayerStressCheckbox() { return layerStressCheckbox; }
    public CheckBox getLayerDeformationCheckbox() { return layerDeformationCheckbox; }
    public CheckBox getLayerReactionsCheckbox() { return layerReactionsCheckbox; }
    public Button getApplyLoadButton() { return applyLoadButton; }
    public Button getUpdateLoadButton() { return updateLoadButton; }
    public Button getRunButton() { return runButton; }
    public Label getMaxStressValueLabel() { return maxStressValueLabel; }
    public Label getMaxDeflectionValueLabel() { return maxDeflectionValueLabel; }
    public Label getSafetyFactorValueLabel() { return safetyFactorValueLabel; }
    public Label getSolverStatusValueLabel() { return solverStatusValueLabel; }
    public Label getMaxTensileStressLabel() { return maxTensileStressLabel; }
    public Label getMaxCompressiveStressLabel() { return maxCompressiveStressLabel; }
    public Label getSelectedSigmaXLabel() { return selectedSigmaXLabel; }
    public Label getSelectedSigmaYLabel() { return selectedSigmaYLabel; }
    public Label getSelectedTauXYLabel() { return selectedTauXYLabel; }
    public Label getSelectedVonMisesLabel() { return selectedVonMisesLabel; }
    public ListView<String> getReactionsListView() { return reactionsListView; }
    public TextArea getExplanationArea() { return explanationArea; }
    public TableView<PointLoad> getPointLoadTable() { return pointLoadTable; }
    public TableView<Support> getSupportTable() { return supportTable; }

    // Properties Panel getters
    public ChoiceBox<Material> getPropMaterialChoice() { return propMaterialChoice; }
    public TextField getPropEField() { return propEField; }
    public TextField getPropAField() { return propAField; }
    public TextField getPropIField() { return propIField; }
    public TextField getPropThicknessField() { return propThicknessField; }
    public TextField getPropLoadMagField() { return propLoadMagField; }
    public TextField getPropLoadDirField() { return propLoadDirField; }
    public ChoiceBox<String> getPropSupportTypeChoice() { return propSupportTypeChoice; }

    public Label getMeshElementCountLabel() { return meshElementCountLabel; }
    public Label getMeshAvgAspectRatioLabel() { return meshAvgAspectRatioLabel; }
    public Label getMeshQualityRatingLabel() { return meshQualityRatingLabel; }
    public Label getMeshSuggestedRefinementLabel() { return meshSuggestedRefinementLabel; }
}
