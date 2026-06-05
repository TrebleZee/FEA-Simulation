package com.treble.feasimulation.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.geometry.Insets;

import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;

public class MainView {
    private final BorderPane root = new BorderPane();
    private final MenuBar menuBar = new MenuBar();
    private final Canvas canvas;
    private final MenuItem exitItem;
    private final MenuItem clearItem;

    // Side panel controls
    private final ChoiceBox<String> elementTypeChoice;
    private final ChoiceBox<String> supportTypeChoice;
    private final ChoiceBox<Material> beamMaterialChoice;
    private final Button drawElementButton;
    private final Button trussModeButton;
    private final Button placeSupportButton;
    private final TextField loadMagnitudeField;
    private final TextField loadAngleField;
    private final Button applyLoadButton;
    private final Button runButton;
    private final Label maxTensileStressLabel;
    private final Label maxCompressiveStressLabel;
    private final TextArea explanationArea;

    public MainView(double width, double height) {
        // Menu
        Menu file = new Menu("File");
        exitItem = new MenuItem("Exit");
        file.getItems().add(exitItem);

        Menu edit = new Menu("Edit");
        clearItem = new MenuItem("Clear");
        edit.getItems().add(clearItem);

        Menu help = new Menu("Help");

        menuBar.getMenus().addAll(file, edit, help);
        root.setTop(menuBar);

        // Canvas
        canvas = new Canvas(width, height);
        StackPane center = new StackPane(canvas);
        root.setCenter(center);

        // Side panel
        elementTypeChoice = new ChoiceBox<>();
        elementTypeChoice.getItems().addAll("Beam", "Truss Member", "Polygon");
        elementTypeChoice.setValue("Beam");
        drawElementButton = new Button("Draw Elements");
        trussModeButton = new Button("Truss Mode");

        supportTypeChoice = new ChoiceBox<>();
        supportTypeChoice.getItems().addAll("FIXED", "PINNED", "ROLLER");
        supportTypeChoice.setValue("FIXED");
        placeSupportButton = new Button("Place Support (click node)");

        beamMaterialChoice = new ChoiceBox<>();
        beamMaterialChoice.getItems().addAll(MaterialLibrary.getPresets());
        beamMaterialChoice.setValue(MaterialLibrary.getDefaultMaterial());

        loadMagnitudeField = new TextField("1000");
        loadAngleField = new TextField("270"); // degrees, 270 = downward
        applyLoadButton = new Button("Apply Point Load (click beam or node)");

        Button runButton = new Button("Run Simulation");

        Label maxTensileStressLabel = new Label("N/A");
        Label maxCompressiveStressLabel = new Label("N/A");

        TextArea explanationArea = new TextArea();
        explanationArea.setEditable(false);
        explanationArea.setWrapText(true);
        explanationArea.setPrefRowCount(8);
        explanationArea.setPromptText("Result explanation will appear here.");

        VBox side = new VBox(8,
                new Label("Drawing Tool:"), elementTypeChoice, drawElementButton, trussModeButton,
                new Label("Supports:"), supportTypeChoice, placeSupportButton,
                new Label("Beam Material:"), beamMaterialChoice,
                new Label("Point Load magnitude:"), loadMagnitudeField,
                new Label("Point Load angle (deg, 0->right, 90->up):"), loadAngleField,
                new Label("Click a beam or node to place the load."), applyLoadButton,
                new Label(""), runButton,
                new Label("Max Tensile Stress:"), maxTensileStressLabel,
                new Label("Max Compressive Stress:"), maxCompressiveStressLabel,
                new Label("Analysis Explanation:"), explanationArea);
        side.setPadding(new Insets(8));
        side.setPrefWidth(320);
        root.setRight(side);

        // expose run button and explanation area
        this.runButton = runButton;
        this.maxTensileStressLabel = maxTensileStressLabel;
        this.maxCompressiveStressLabel = maxCompressiveStressLabel;
        this.explanationArea = explanationArea;
    }

    public BorderPane getRoot() { return root; }
    public MenuBar getMenuBar() { return menuBar; }
    public Canvas getCanvas() { return canvas; }
    public MenuItem getExitItem() { return exitItem; }
    public MenuItem getClearItem() { return clearItem; }

    // Side panel getters
    public ChoiceBox<String> getElementTypeChoice() { return elementTypeChoice; }
    public Button getDrawElementButton() { return drawElementButton; }
    public Button getTrussModeButton() { return trussModeButton; }
    public ChoiceBox<String> getSupportTypeChoice() { return supportTypeChoice; }
    public ChoiceBox<Material> getBeamMaterialChoice() { return beamMaterialChoice; }
    public Button getPlaceSupportButton() { return placeSupportButton; }
    public TextField getLoadMagnitudeField() { return loadMagnitudeField; }
    public TextField getLoadAngleField() { return loadAngleField; }
    public Button getApplyLoadButton() { return applyLoadButton; }
    public Button getRunButton() { return runButton; }
    public Label getMaxTensileStressLabel() { return maxTensileStressLabel; }
    public Label getMaxCompressiveStressLabel() { return maxCompressiveStressLabel; }
    public TextArea getExplanationArea() { return explanationArea; }

    public void clearStressSummary() {
        maxTensileStressLabel.setText("N/A");
        maxCompressiveStressLabel.setText("N/A");
    }
}
