package com.treble.feasimulation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application {
    private double lastX, lastY;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("2D FEA Simulator");

        BorderPane root = new BorderPane();

        // Menu bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> primaryStage.close());
        fileMenu.getItems().add(exit);
        Menu editMenu = new Menu("Edit");
        Menu helpMenu = new Menu("Help");
        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        root.setTop(menuBar);

        // Canvas for drawing
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc, canvas);

        // Basic freehand drawing handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastX = e.getX();
            lastY = e.getY();
            gc.beginPath();
            gc.moveTo(lastX, lastY);
            gc.stroke();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double x = e.getX();
            double y = e.getY();
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.lineTo(x, y);
            gc.stroke();
        });

        StackPane center = new StackPane(canvas);
        root.setCenter(center);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void clearCanvas(GraphicsContext gc, Canvas canvas) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
