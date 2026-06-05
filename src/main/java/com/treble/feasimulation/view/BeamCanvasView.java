package com.treble.feasimulation.view;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Node;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.Optional;

public class BeamCanvasView {
    private final Canvas canvas;
    private final FEAData model;

    private Integer tempStartNodeId = null;
    private double tempStartX, tempStartY;

    private Integer draggingNodeId = null;

    private Integer hoverElementId = null;

    private static final double NODE_RADIUS = 5.0;
    private static final double HIT_TOLERANCE = 8.0;

    public BeamCanvasView(Canvas canvas, FEAData model) {
        this.canvas = canvas;
        this.model = model;
        clear();
        installHandlers();
        redraw();
    }

    private void installHandlers() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);
    }

    private void onMouseClicked(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            // Left click: create beam endpoints
            Point2D p = new Point2D(e.getX(), e.getY());
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            if (tempStartNodeId == null) {
                if (nearNode >= 0) {
                    tempStartNodeId = nearNode;
                    Node n = model.findNodeById(nearNode).orElseThrow();
                    tempStartX = n.getX(); tempStartY = n.getY();
                } else {
                    int nid = model.nextNodeId();
                    Node n = new Node(nid, p.getX(), p.getY());
                    model.addNode(n);
                    tempStartNodeId = nid;
                    tempStartX = p.getX(); tempStartY = p.getY();
                }
                redraw();
            } else {
                int endNodeId;
                if (nearNode >= 0) {
                    endNodeId = nearNode;
                } else {
                    endNodeId = model.nextNodeId();
                    Node n = new Node(endNodeId, p.getX(), p.getY());
                    model.addNode(n);
                }

                // Create beam element
                int eid = model.nextElementId();
                BeamElement be = new BeamElement(eid, tempStartNodeId, endNodeId, 0, 1.0, 1.0);
                model.addElement(be);

                tempStartNodeId = null;
                redraw();
            }
            e.consume();
        }
    }

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            // Right-click: show context menu for element deletion
            Point2D p = new Point2D(e.getX(), e.getY());
            Integer elId = findNearestElementId(p, HIT_TOLERANCE);
            ContextMenu menu = new ContextMenu();
            if (elId != null) {
                MenuItem delete = new MenuItem("Delete Beam");
                delete.setOnAction(ae -> {
                    model.removeElementById(elId);
                    redraw();
                });
                menu.getItems().add(delete);
            }
            MenuItem exit = new MenuItem("Exit");
            exit.setOnAction(ae -> Platform.exit());
            menu.getItems().add(exit);
            menu.show(canvas, e.getScreenX(), e.getScreenY());
            e.consume();
            return;
        }

        // Left button may initiate node drag
        if (e.getButton() == MouseButton.PRIMARY) {
            Point2D p = new Point2D(e.getX(), e.getY());
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            if (nearNode >= 0) {
                draggingNodeId = nearNode;
            }
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (draggingNodeId != null) {
            // Update node position
            Node updated = new Node(draggingNodeId, e.getX(), e.getY());
            model.updateNode(updated);
            redraw();
            e.consume();
        } else if (tempStartNodeId != null) {
            // while drawing temporary beam, show rubberband
            redraw();
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setStroke(Color.GRAY);
            g.setLineDashes(5);
            g.strokeLine(tempStartX, tempStartY, e.getX(), e.getY());
            g.setLineDashes(0);
            e.consume();
        }
    }

    private void onMouseReleased(MouseEvent e) {
        draggingNodeId = null;
    }

    private void onMouseMoved(MouseEvent e) {
        Point2D p = new Point2D(e.getX(), e.getY());
        Integer el = findNearestElementId(p, HIT_TOLERANCE);
        if (el != null) hoverElementId = el; else hoverElementId = null;
        redraw();
    }

    public void clear() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void redraw() {
        clear();
        GraphicsContext g = canvas.getGraphicsContext2D();
        // draw elements
        for (BeamElement be : model.getElements()) {
            Optional<Node> s = model.findNodeById(be.getNodeStartId());
            Optional<Node> t = model.findNodeById(be.getNodeEndId());
            if (s.isPresent() && t.isPresent()) {
                Node ns = s.get(); Node nt = t.get();
                if (hoverElementId != null && hoverElementId == be.getId()) {
                    g.setStroke(Color.ORANGE);
                    g.setLineWidth(3);
                } else {
                    g.setStroke(Color.BLACK);
                    g.setLineWidth(2);
                }
                g.strokeLine(ns.getX(), ns.getY(), nt.getX(), nt.getY());
                // draw id label
                double mx = (ns.getX() + nt.getX()) / 2.0;
                double my = (ns.getY() + nt.getY()) / 2.0;
                g.setFill(Color.BLUE);
                g.fillText("E" + be.getId(), mx + 4, my - 4);
            }
        }

        // draw nodes
        g.setFill(Color.RED);
        for (Node n : model.getNodes()) {
            g.fillOval(n.getX() - NODE_RADIUS, n.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        }

        // draw temp start marker
        if (tempStartNodeId != null) {
            g.setFill(Color.GREEN);
            g.fillOval(tempStartX - NODE_RADIUS, tempStartY - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        }
    }

    private int findNearestNodeId(Point2D p, double tol) {
        int bestId = -1;
        double bestDist = Double.MAX_VALUE;
        for (Node n : model.getNodes()) {
            double d = p.distance(n.getX(), n.getY());
            if (d < bestDist && d <= tol) {
                bestDist = d; bestId = n.getId();
            }
        }
        return bestId;
    }

    private Integer findNearestElementId(Point2D p, double tol) {
        Integer best = null;
        double bestDist = Double.MAX_VALUE;
        for (BeamElement be : model.getElements()) {
            Optional<Node> s = model.findNodeById(be.getNodeStartId());
            Optional<Node> t = model.findNodeById(be.getNodeEndId());
            if (s.isPresent() && t.isPresent()) {
                Point2D a = new Point2D(s.get().getX(), s.get().getY());
                Point2D b = new Point2D(t.get().getX(), t.get().getY());
                double dist = pointToSegmentDistance(p, a, b);
                if (dist < bestDist && dist <= tol) {
                    bestDist = dist; best = be.getId();
                }
            }
        }
        return best;
    }

    private double pointToSegmentDistance(Point2D p, Point2D a, Point2D b) {
        // projection
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        if (dx == 0 && dy == 0) return p.distance(a);
        double t = ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double px = a.getX() + t * dx;
        double py = a.getY() + t * dy;
        return p.distance(px, py);
    }

    public Canvas getCanvas() { return canvas; }
}
