package com.treble.feasimulation.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class CanvasView {
    private final Canvas canvas;
    private double lastX, lastY;

    public CanvasView(Canvas canvas) {
        this.canvas = canvas;
        clear();
        installHandlers();
    }

    private void installHandlers() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

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
    }

    public void clear() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
    }

    public Canvas getCanvas() { return canvas; }
}
