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

import com.treble.feasimulation.model.Support;
import com.treble.feasimulation.model.PointLoad;

public class BeamCanvasView {
    public enum Mode { DRAW, PLACE_SUPPORT, PLACE_LOAD, NONE }

    private static class SegmentProjection {
        final Point2D point;
        final double distance;

        SegmentProjection(Point2D point, double distance) {
            this.point = point;
            this.distance = distance;
        }
    }

    private static class ElementHit {
        final int elementId;
        final Point2D projectedPoint;
        final double distance;

        ElementHit(int elementId, Point2D projectedPoint, double distance) {
            this.elementId = elementId;
            this.projectedPoint = projectedPoint;
            this.distance = distance;
        }
    }

    private final Canvas canvas;
    private final FEAData model;

    private Mode mode = Mode.DRAW;
    private Support.Type placingSupportType = Support.Type.FIXED;
    private double placingLoadMagnitude = 0.0;
    private double placingLoadAngleDeg = 270.0;

    private Integer tempStartNodeId = null;
    private double tempStartX, tempStartY;

    private Integer draggingNodeId = null;

    private Integer hoverElementId = null;

    // result storage
    private com.treble.feasimulation.solver.BeamSolver.Result lastResult = null;
    private double lastScale = 1.0;

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
        if (e.getButton() != MouseButton.PRIMARY) return;
        Point2D p = new Point2D(e.getX(), e.getY());

        // Modes: place support, place load, or draw beams
        if (mode == Mode.PLACE_SUPPORT) {
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            int nodeId;
            if (nearNode >= 0) {
                nodeId = nearNode;
            } else {
                nodeId = model.nextNodeId();
                model.addNode(new Node(nodeId, p.getX(), p.getY()));
            }
            int sid = model.nextSupportId();
            Support s = new Support(sid, nodeId, placingSupportType);
            model.addSupport(s);
            redraw();
            e.consume();
            return;
        }

        if (mode == Mode.PLACE_LOAD) {
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            int nodeId;
            if (nearNode >= 0) {
                nodeId = nearNode;
            } else {
                java.util.Optional<ElementHit> hit = findNearestElementHit(p, HIT_TOLERANCE);
                if (hit.isEmpty()) {
                    return;
                }
                nodeId = model.splitElementAtPoint(hit.get().elementId,
                        hit.get().projectedPoint.getX(), hit.get().projectedPoint.getY());
            }
            // convert magnitude & angle to fx, fy (y screen grows downwards so invert sin)
            double rad = Math.toRadians(placingLoadAngleDeg);
            double fx = placingLoadMagnitude * Math.cos(rad);
            double fy = -placingLoadMagnitude * Math.sin(rad);
            int lid = model.nextPointLoadId();
            PointLoad pl = new PointLoad(lid, nodeId, fx, fy);
            model.addPointLoad(pl);
            redraw();
            e.consume();
            return;
        }

        // Default: drawing beams
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
            // also offer delete support or load if clicking near a node
            int nearNodeForMenu = findNearestNodeId(p, HIT_TOLERANCE);
            if (nearNodeForMenu >= 0) {
                // supports attached to this node
                for (com.treble.feasimulation.model.Support s : model.getSupports()) {
                    if (s.getNodeId() == nearNodeForMenu) {
                        MenuItem delS = new MenuItem("Delete Support " + s.getId());
                        int sidLocal = s.getId();
                        delS.setOnAction(ae -> { model.removeSupportById(sidLocal); redraw(); });
                        menu.getItems().add(delS);
                    }
                }
                for (com.treble.feasimulation.model.PointLoad pl : model.getPointLoads()) {
                    if (pl.getNodeId() == nearNodeForMenu) {
                        MenuItem delP = new MenuItem("Delete PointLoad " + pl.getId());
                        int pidLocal = pl.getId();
                        delP.setOnAction(ae -> { model.removePointLoadById(pidLocal); redraw(); });
                        menu.getItems().add(delP);
                    }
                }
                // option to delete the node (will cascade remove attached elements/supports/loads)
                MenuItem delNode = new MenuItem("Delete Node " + nearNodeForMenu);
                int nidLocal = nearNodeForMenu;
                delNode.setOnAction(ae -> { model.removeNodeById(nidLocal); redraw(); });
                menu.getItems().add(delNode);
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
        // draw elements (undeformed black)
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

        // if we have results, draw deformed shape and moment diagram
        if (lastResult != null) {
            drawMomentDiagram(g, lastResult);
            drawDeformedShape(g, lastResult, lastScale);
        }

        // draw nodes
        g.setFill(Color.RED);
        for (Node n : model.getNodes()) {
            g.fillOval(n.getX() - NODE_RADIUS, n.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        }

        // draw supports
        g.setFill(Color.DARKGREEN);
        for (Support s : model.getSupports()) {
            model.findNodeById(s.getNodeId()).ifPresent(n -> {
                double x = n.getX(); double y = n.getY();
                switch (s.getType() == null ? Support.Type.PINNED : s.getType()) {
                    case FIXED:
                        g.fillRect(x-6, y+6, 12, 6);
                        break;
                    case PINNED:
                        g.fillOval(x-6, y+6, 12, 6);
                        break;
                    case ROLLER:
                        g.strokeOval(x-8, y+6, 16, 6);
                        break;
                }
            });
        }

        // draw point loads
        g.setStroke(Color.MAGENTA);
        for (PointLoad pl : model.getPointLoads()) {
            model.findNodeById(pl.getNodeId()).ifPresent(n -> {
                double x = n.getX(); double y = n.getY();
                double len = 20;
                // derive arrow direction from fx,fy
                double fx = pl.getFx(); double fy = pl.getFy();
                double ang = Math.atan2(-fy, fx); // invert fy for screen
                double x2 = x + len * Math.cos(ang);
                double y2 = y - len * Math.sin(ang);
                g.strokeLine(x, y, x2, y2);
            });
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
        java.util.Optional<ElementHit> hit = findNearestElementHit(p, tol);
        return hit.map(elementHit -> elementHit.elementId).orElse(null);
    }

    private java.util.Optional<ElementHit> findNearestElementHit(Point2D p, double tol) {
        Integer best = null;
        double bestDist = Double.MAX_VALUE;
        Point2D bestProjection = null;
        for (BeamElement be : model.getElements()) {
            Optional<Node> s = model.findNodeById(be.getNodeStartId());
            Optional<Node> t = model.findNodeById(be.getNodeEndId());
            if (s.isPresent() && t.isPresent()) {
                Point2D a = new Point2D(s.get().getX(), s.get().getY());
                Point2D b = new Point2D(t.get().getX(), t.get().getY());
                SegmentProjection projection = projectPointOntoSegment(p, a, b);
                double dist = projection.distance;
                if (dist < bestDist && dist <= tol) {
                    bestDist = dist;
                    best = be.getId();
                    bestProjection = projection.point;
                }
            }
        }
        if (best == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new ElementHit(best, bestProjection, bestDist));
    }

    private SegmentProjection projectPointOntoSegment(Point2D p, Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        if (dx == 0 && dy == 0) return new SegmentProjection(a, p.distance(a));
        double t = ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double px = a.getX() + t * dx;
        double py = a.getY() + t * dy;
        Point2D projected = new Point2D(px, py);
        return new SegmentProjection(projected, p.distance(projected));
    }

    public Canvas getCanvas() { return canvas; }

    public void setMode(Mode m) { this.mode = m; }
    public Mode getMode() { return mode; }

    public void setPlacingSupportType(Support.Type t) { this.placingSupportType = t; }
    public void setPlacingLoad(double magnitude, double angleDeg) { this.placingLoadMagnitude = magnitude; this.placingLoadAngleDeg = angleDeg; }

    public void showResult(com.treble.feasimulation.solver.BeamSolver.Result r, double scale) {
        this.lastResult = r;
        this.lastScale = scale;
        redraw();
    }

    private void drawDeformedShape(GraphicsContext g, com.treble.feasimulation.solver.BeamSolver.Result r, double scale) {
        // draw deformed shape in blue
        g.setStroke(Color.CORNFLOWERBLUE);
        g.setLineWidth(2);
        for (com.treble.feasimulation.model.BeamElement be : model.getElements()) {
            java.util.Optional<Node> s = model.findNodeById(be.getNodeStartId());
            java.util.Optional<Node> t = model.findNodeById(be.getNodeEndId());
            if (s.isEmpty() || t.isEmpty()) continue;
            Node ns = s.get(); Node nt = t.get();
            int si = -1, ti = -1;
            java.util.List<Node> nodes = model.getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).getId() == ns.getId()) si = i;
                if (nodes.get(i).getId() == nt.getId()) ti = i;
            }
            if (si < 0 || ti < 0) continue;
            double v1 = r.displacements[3*si + 1];
            double v2 = r.displacements[3*ti + 1];
            // screen y increases down, so add displacement directly (solver used fy with inverted sign earlier)
            g.strokeLine(ns.getX(), ns.getY() + v1*scale, nt.getX(), nt.getY() + v2*scale);
        }
    }

    private void drawMomentDiagram(GraphicsContext g, com.treble.feasimulation.solver.BeamSolver.Result r) {
        // map absolute moment to color ramp (blue -> white -> red)
        double maxM = 0.0;
        for (com.treble.feasimulation.solver.BeamSolver.ElementResult er : r.elementResults) {
            maxM = Math.max(maxM, Math.abs(er.endMomentStart));
            maxM = Math.max(maxM, Math.abs(er.endMomentEnd));
        }
        if (maxM < 1e-12) return;

        int samples = 12;
        for (com.treble.feasimulation.model.BeamElement be : model.getElements()) {
            java.util.Optional<Node> s = model.findNodeById(be.getNodeStartId());
            java.util.Optional<Node> t = model.findNodeById(be.getNodeEndId());
            if (s.isEmpty() || t.isEmpty()) continue;
            Node ns = s.get(); Node nt = t.get();
            // find element result by id
            com.treble.feasimulation.solver.BeamSolver.ElementResult er = null;
            for (com.treble.feasimulation.solver.BeamSolver.ElementResult x : r.elementResults) if (x.elementId == be.getId()) { er = x; break; }
            double m1 = (er==null?0.0:er.endMomentStart);
            double m2 = (er==null?0.0:er.endMomentEnd);

            for (int i = 0; i < samples; i++) {
                double t0 = (double)i / samples;
                double t1 = (double)(i+1) / samples;
                double x0 = ns.getX() + (nt.getX()-ns.getX())*t0;
                double y0 = ns.getY() + (nt.getY()-ns.getY())*t0;
                double x1 = ns.getX() + (nt.getX()-ns.getX())*t1;
                double y1 = ns.getY() + (nt.getY()-ns.getY())*t1;
                double m0 = m1 + (m2 - m1)*t0;
                double m1v = m1 + (m2 - m1)*t1;
                double mv = 0.5*(m0 + m1v);
                Color c = colorForMoment(mv, maxM);
                g.setStroke(c);
                g.setLineWidth(6);
                g.strokeLine(x0, y0, x1, y1);
            }
        }
    }

    private Color colorForMoment(double m, double max) {
        double v = Math.min(1.0, Math.abs(m) / max);
        // simple blue-white-red: negative moments blue, positive red
        if (m >= 0) {
            return new Color(1.0, 1.0 - v, 1.0 - v, 0.8);
        } else {
            return new Color(1.0 - v, 1.0 - v, 1.0, 0.8);
        }
    }
}

