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

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import com.treble.feasimulation.model.Material;
import com.treble.feasimulation.model.MaterialLibrary;
import com.treble.feasimulation.model.PointLoad;
import com.treble.feasimulation.model.Support;

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
    private final Button drawPolygonButton;
    private final Button trussModeButton;
    private final Button placeSupportButton;
    private final TextField loadFxField;
    private final TextField loadFyField;
    private final TextField poissonRatioField;
    private final TextField thicknessField;
    private final Button applyLoadButton;
    private final Button updateLoadButton;
    private final Button runButton;
    private final Label maxTensileStressLabel;
    private final Label maxCompressiveStressLabel;
    private final TextArea explanationArea;
    private final javafx.scene.control.TableView<com.treble.feasimulation.model.PointLoad> pointLoadTable;
    private final javafx.scene.control.TableView<com.treble.feasimulation.model.Support> supportTable;

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
        drawPolygonButton = new Button("Draw Polygons");
        trussModeButton = new Button("Truss Mode");

        supportTypeChoice = new ChoiceBox<>();
        supportTypeChoice.getItems().addAll("FIXED", "PINNED", "ROLLER");
        supportTypeChoice.setValue("FIXED");
        placeSupportButton = new Button("Place Support (click node)");

        beamMaterialChoice = new ChoiceBox<>();
        beamMaterialChoice.getItems().addAll(MaterialLibrary.getPresets());
        beamMaterialChoice.setValue(MaterialLibrary.getDefaultMaterial());

        poissonRatioField = new TextField("0.3");
        thicknessField = new TextField("0.01");

        loadFxField = new TextField("0");
        loadFyField = new TextField("-1000");
        applyLoadButton = new Button("Apply Point Load (click beam or node)");
        updateLoadButton = new Button("Update Selected Load");

        pointLoadTable = new TableView<>();
        TableColumn<PointLoad, Integer> plIdCol = new TableColumn<>("ID");
        plIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<PointLoad, Integer> plNodeCol = new TableColumn<>("Node");
        plNodeCol.setCellValueFactory(new PropertyValueFactory<>("nodeId"));
        TableColumn<PointLoad, Double> plFxCol = new TableColumn<>("Fx");
        plFxCol.setCellValueFactory(new PropertyValueFactory<>("fx"));
        TableColumn<PointLoad, Double> plFyCol = new TableColumn<>("Fy");
        plFyCol.setCellValueFactory(new PropertyValueFactory<>("fy"));
        pointLoadTable.getColumns().addAll(plIdCol, plNodeCol, plFxCol, plFyCol);
        pointLoadTable.setPrefHeight(150);

        supportTable = new TableView<>();
        TableColumn<Support, Integer> sIdCol = new TableColumn<>("ID");
        sIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Support, Integer> sNodeCol = new TableColumn<>("Node");
        sNodeCol.setCellValueFactory(new PropertyValueFactory<>("nodeId"));
        TableColumn<Support, String> sTypeCol = new TableColumn<>("Type");
        sTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        supportTable.getColumns().addAll(sIdCol, sNodeCol, sTypeCol);
        supportTable.setPrefHeight(150);

        TabPane tablesTabPane = new TabPane();
        Tab loadsTab = new Tab("Loads", pointLoadTable);
        loadsTab.setClosable(false);
        Tab supportsTab = new Tab("Supports", supportTable);
        supportsTab.setClosable(false);
        tablesTabPane.getTabs().addAll(loadsTab, supportsTab);

        Button runButton = new Button("Run Simulation");

        Label maxTensileStressLabel = new Label("N/A");
        Label maxCompressiveStressLabel = new Label("N/A");

        TextArea explanationArea = new TextArea();
        explanationArea.setEditable(false);
        explanationArea.setWrapText(true);
        explanationArea.setPrefRowCount(8);
        explanationArea.setPromptText("Result explanation will appear here.");

        VBox side = new VBox(8,
                new Label("Drawing Tool:"), elementTypeChoice, drawElementButton, drawPolygonButton, trussModeButton,
                new Label("Supports:"), supportTypeChoice, placeSupportButton,
                new Label("Material:"), beamMaterialChoice,
                new Label("Poisson's Ratio:"), poissonRatioField,
                new Label("Thickness (m):"), thicknessField,
                new Label("Point Load Fx:"), loadFxField,
                new Label("Point Load Fy:"), loadFyField,
                new Label("Click an element or node to place the load."), applyLoadButton, updateLoadButton,
                new Label("Data Editing:"), tablesTabPane,
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
    public Button getDrawPolygonButton() { return drawPolygonButton; }
    public Button getTrussModeButton() { return trussModeButton; }
    public ChoiceBox<String> getSupportTypeChoice() { return supportTypeChoice; }
    public ChoiceBox<Material> getBeamMaterialChoice() { return beamMaterialChoice; }
    public Button getPlaceSupportButton() { return placeSupportButton; }
    public TextField getLoadFxField() { return loadFxField; }
    public TextField getLoadFyField() { return loadFyField; }
    public TextField getPoissonRatioField() { return poissonRatioField; }
    public TextField getThicknessField() { return thicknessField; }
    public Button getApplyLoadButton() { return applyLoadButton; }
    public Button getUpdateLoadButton() { return updateLoadButton; }
    public Button getRunButton() { return runButton; }
    public Label getMaxTensileStressLabel() { return maxTensileStressLabel; }
    public Label getMaxCompressiveStressLabel() { return maxCompressiveStressLabel; }
    public TextArea getExplanationArea() { return explanationArea; }
    public TableView<PointLoad> getPointLoadTable() { return pointLoadTable; }
    public TableView<Support> getSupportTable() { return supportTable; }

    public void clearStressSummary() {
        maxTensileStressLabel.setText("N/A");
        maxCompressiveStressLabel.setText("N/A");
    }
}
