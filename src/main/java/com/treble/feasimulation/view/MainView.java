package com.treble.feasimulation.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainView {
    private final BorderPane root = new BorderPane();
    private final MenuBar menuBar = new MenuBar();
    private final Canvas canvas;
    private final MenuItem exitItem;
    private final MenuItem clearItem;

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
    }

    public BorderPane getRoot() { return root; }
    public MenuBar getMenuBar() { return menuBar; }
    public Canvas getCanvas() { return canvas; }
    public MenuItem getExitItem() { return exitItem; }
    public MenuItem getClearItem() { return clearItem; }
}
