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
import javafx.scene.control.TextField;
import javafx.geometry.Insets;

public class MainView {
    private final BorderPane root = new BorderPane();
    private final MenuBar menuBar = new MenuBar();
    private final Canvas canvas;
    private final MenuItem exitItem;
    private final MenuItem clearItem;

    // Side panel controls
    private final ChoiceBox<String> supportTypeChoice;
    private final Button placeSupportButton;
    private final TextField loadMagnitudeField;
    private final TextField loadAngleField;
    private final Button applyLoadButton;

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
        supportTypeChoice = new ChoiceBox<>();
        supportTypeChoice.getItems().addAll("FIXED", "PINNED", "ROLLER");
        supportTypeChoice.setValue("FIXED");
        placeSupportButton = new Button("Place Support (click node)");

        loadMagnitudeField = new TextField("1000");
        loadAngleField = new TextField("270"); // degrees, 270 = downward
        applyLoadButton = new Button("Apply Point Load (click node)");

        VBox side = new VBox(8,
                new Label("Supports:"), supportTypeChoice, placeSupportButton,
                new Label("Point Load magnitude:"), loadMagnitudeField,
                new Label("Point Load angle (deg, 0->right, 90->up):"), loadAngleField, applyLoadButton);
        side.setPadding(new Insets(8));
        side.setPrefWidth(220);
        root.setRight(side);
    }

    public BorderPane getRoot() { return root; }
    public MenuBar getMenuBar() { return menuBar; }
    public Canvas getCanvas() { return canvas; }
    public MenuItem getExitItem() { return exitItem; }
    public MenuItem getClearItem() { return clearItem; }

    // Side panel getters
    public ChoiceBox<String> getSupportTypeChoice() { return supportTypeChoice; }
    public Button getPlaceSupportButton() { return placeSupportButton; }
    public TextField getLoadMagnitudeField() { return loadMagnitudeField; }
    public TextField getLoadAngleField() { return loadAngleField; }
    public Button getApplyLoadButton() { return applyLoadButton; }
}
