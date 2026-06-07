package com.treble.feasimulation.view;

import com.treble.feasimulation.model.BeamElement;
import com.treble.feasimulation.model.Element;
import com.treble.feasimulation.model.TrussElement;
import com.treble.feasimulation.model.TrussMember;
import com.treble.feasimulation.model.TrussNode;
import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.model.Node;
import com.treble.feasimulation.model.MaterialLibrary;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.treble.feasimulation.model.Support;
import com.treble.feasimulation.model.PointLoad;

public class BeamCanvasView {
    public enum Mode { DRAW, PLACE_SUPPORT, PLACE_LOAD, NONE }
    public enum ElementType { BEAM, TRUSS, POLYGON }

    private interface CanvasTool {
        void onClick(Point2D p, MouseEvent e);
        void onDrag(Point2D p, MouseEvent e);
        void onMove(Point2D p, MouseEvent e);
        void onRelease(Point2D p, MouseEvent e);
        void drawOverlay(GraphicsContext g);
    }

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
    private ElementType activeElementType = ElementType.BEAM;
    private CanvasTool activeTool;
    private Support.Type placingSupportType = Support.Type.FIXED;
    private int placingBeamMaterialId = MaterialLibrary.getDefaultMaterial().getId();
    private double placingFx = 0.0;
    private double placingFy = -1000.0;

    private Integer draggingNodeId = null;

    private Integer hoverElementId = null;

    // result storage
    private com.treble.feasimulation.solver.BeamSolver.Result lastResult = null;
    private com.treble.feasimulation.solver.TrussSolver.Result lastTrussResult = null;
    private double lastScale = 1.0;
    private Runnable onModelUpdate;
    public void setOnModelUpdate(Runnable r) { this.onModelUpdate = r; }

    private void notifyModelUpdate() {
        if (onModelUpdate != null) onModelUpdate.run();
    }

    private static final double NODE_RADIUS = 5.0;
    private static final double HIT_TOLERANCE = 8.0;

    public BeamCanvasView(Canvas canvas, FEAData model) {
        this.canvas = canvas;
        this.model = model;
        clear();
        installHandlers();
        updateActiveTool(); // initialize active tool
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
        if (activeTool != null) {
            activeTool.onClick(new Point2D(e.getX(), e.getY()), e);
        }
    }

    private void onMousePressed(MouseEvent e) {
        if (activeTool != null) {
            activeTool.onDrag(new Point2D(e.getX(), e.getY()), e); // pressed starts drag
        }
        if (e.getButton() == MouseButton.SECONDARY) {
            // Right-click: show context menu for element deletion
            Point2D p = new Point2D(e.getX(), e.getY());
            Integer elId = findNearestElementId(p, HIT_TOLERANCE);
            ContextMenu menu = new ContextMenu();

            if (elId != null) {
                MenuItem delete = new MenuItem("Delete Element");
                delete.setOnAction(ae -> {
                    model.removeElementById(elId);
                    redraw();
                });
                menu.getItems().add(delete);
            }

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
        Point2D p = new Point2D(e.getX(), e.getY());
        if (activeTool != null) {
            activeTool.onDrag(p, e);
        }
        if (draggingNodeId != null) {
            // Update node position while preserving its type
            model.findNodeById(draggingNodeId).ifPresent(oldNode -> {
                model.updateNode(oldNode.copyAt(e.getX(), e.getY()));
            });
            notifyModelUpdate();
            redraw();
            e.consume();
            return;
        }
    }

    private void onMouseReleased(MouseEvent e) {
        if (activeTool != null) {
            activeTool.onRelease(new Point2D(e.getX(), e.getY()), e);
        }
        draggingNodeId = null;
    }

    private void onMouseMoved(MouseEvent e) {
        Point2D p = new Point2D(e.getX(), e.getY());
        if (activeTool != null) {
            activeTool.onMove(p, e);
        }
        Integer el = findNearestElementId(p, HIT_TOLERANCE);
        if (el != null) hoverElementId = el; else hoverElementId = null;
        redraw();
    }

    public void clear() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void redraw() {
        clear();
        GraphicsContext g = canvas.getGraphicsContext2D();
        // draw elements (undeformed black)
        for (Element e : model.getElements()) {
            Optional<Node> s = model.findNodeById(e.getNodeStartId());
            Optional<Node> t = model.findNodeById(e.getNodeEndId());
            if (s.isPresent() && t.isPresent()) {
                Node ns = s.get(); Node nt = t.get();
                if (hoverElementId != null && hoverElementId == e.getId()) {
                    g.setStroke(Color.ORANGE);
                    g.setLineWidth(3);
                } else {
                    Color base = e instanceof TrussElement ? Color.GRAY : Color.BLACK;
                    if (lastTrussResult != null || lastResult != null) {
                        g.setStroke(Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.2));
                    } else {
                        g.setStroke(base);
                    }
                    g.setLineWidth(2);
                }
                g.strokeLine(ns.getX(), ns.getY(), nt.getX(), nt.getY());
                // draw id label
                double mx = (ns.getX() + nt.getX()) / 2.0;
                double my = (ns.getY() + nt.getY()) / 2.0;
                g.setFill(lastTrussResult != null || lastResult != null ? Color.color(0, 0, 1, 0.2) : Color.BLUE);
                g.fillText((e instanceof TrussElement ? "T" : "E") + e.getId(), mx + 4, my - 4);
            }
        }

        // if we have results, draw deformed shape and moment diagram
        if (lastResult != null) {
            drawMomentDiagram(g, lastResult);
            drawDeformedShape(g, lastResult, lastScale);
        } else if (lastTrussResult != null) {
            drawTrussResult(g, lastTrussResult, lastScale);
        }

        // draw nodes
        if (lastTrussResult == null && lastResult == null) {
            g.setFill(Color.RED);
            for (Node n : model.getNodes()) {
                g.fillOval(n.getX() - NODE_RADIUS, n.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            }
        } else {
            // draw original nodes faint
            g.setFill(Color.color(1, 0, 0, 0.3));
            for (Node n : model.getNodes()) {
                g.fillOval(n.getX() - NODE_RADIUS, n.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            }
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

        // draw tool overlay last
        if (activeTool != null) {
            activeTool.drawOverlay(g);
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
        for (Element e : model.getElements()) {
            Optional<Node> s = model.findNodeById(e.getNodeStartId());
            Optional<Node> t = model.findNodeById(e.getNodeEndId());
            if (s.isPresent() && t.isPresent()) {
                Point2D a = new Point2D(s.get().getX(), s.get().getY());
                Point2D b = new Point2D(t.get().getX(), t.get().getY());
                SegmentProjection projection = projectPointOntoSegment(p, a, b);
                double dist = projection.distance;
                if (dist < bestDist && dist <= tol) {
                    bestDist = dist;
                    best = e.getId();
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

    public void setMode(Mode m) {
        this.mode = m;
        updateActiveTool();
    }
    public Mode getMode() { return mode; }

    public void setPlacingElementType(ElementType type) {
        this.activeElementType = type;
        updateActiveTool();
    }
    public ElementType getPlacingElementType() { return activeElementType; }

    private void updateActiveTool() {
        if (mode == Mode.DRAW) {
            if (activeElementType == ElementType.TRUSS) {
                activeTool = new TrussTool();
            } else if (activeElementType == ElementType.POLYGON) {
                activeTool = new PolygonTool();
            } else {
                activeTool = new BeamTool();
            }
        } else if (mode == Mode.PLACE_SUPPORT) {
            activeTool = new SupportTool();
        } else if (mode == Mode.PLACE_LOAD) {
            activeTool = new LoadTool();
        } else {
            activeTool = null;
        }
        redraw();
    }

    public void setPlacingSupportType(Support.Type t) { this.placingSupportType = t; }
    public void setPlacingBeamMaterialId(int materialId) { this.placingBeamMaterialId = materialId; }
    public void setPlacingLoad(double fx, double fy) { this.placingFx = fx; this.placingFy = fy; }

    public void showResult(com.treble.feasimulation.solver.BeamSolver.Result r, double scale) {
        this.lastResult = r;
        this.lastTrussResult = null;
        this.lastScale = scale;
        redraw();
    }

    public void showTrussResult(com.treble.feasimulation.solver.TrussSolver.Result r, double scale) {
        this.lastTrussResult = r;
        this.lastResult = null;
        this.lastScale = scale;
        redraw();
    }

    private void drawTrussResult(GraphicsContext g, com.treble.feasimulation.solver.TrussSolver.Result r, double scale) {
        // Draw deformed shape in red/blue based on axial force
        List<Node> nodes = model.getNodes();
        Map<Integer, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) nodeIndex.put(nodes.get(i).getId(), i);

        double maxAbsForce = 0;
        double maxTensionForce = 0;
        double maxCompressionForce = 0;
        for (var er : r.elementResults) {
            maxAbsForce = Math.max(maxAbsForce, Math.abs(er.axialForce));
            if (er.axialForce > maxTensionForce) maxTensionForce = er.axialForce;
            if (er.axialForce < maxCompressionForce) maxCompressionForce = er.axialForce;
        }

        for (com.treble.feasimulation.solver.TrussSolver.ElementResult er : r.elementResults) {
            Integer si = nodeIndex.get(er.nodeStartId);
            Integer ti = nodeIndex.get(er.nodeEndId);
            if (si == null || ti == null) continue;

            Node ns = nodes.get(si);
            Node nt = nodes.get(ti);

            double u1 = r.displacements[2 * si];
            double v1 = r.displacements[2 * si + 1];
            double u2 = r.displacements[2 * ti];
            double v2 = r.displacements[2 * ti + 1];

            double x1_def = ns.getX() + u1 * scale;
            double y1_def = ns.getY() + v1 * scale;
            double x2_def = nt.getX() + u2 * scale;
            double y2_def = nt.getY() + v2 * scale;

            // Color: Tension = Red, Compression = Blue
            g.setStroke(getTrussColor(er.axialForce, maxAbsForce));
            g.setLineWidth(3);
            g.strokeLine(x1_def, y1_def, x2_def, y2_def);
        }

        // Draw deformed nodes
        g.setFill(Color.DARKRED);
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            double u = r.displacements[2 * i];
            double v = r.displacements[2 * i + 1];
            double x_def = n.getX() + u * scale;
            double y_def = n.getY() + v * scale;
            g.fillOval(x_def - NODE_RADIUS, y_def - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        }

        drawTrussLegend(g, maxTensionForce, maxCompressionForce);
    }

    private Color getTrussColor(double force, double maxAbsForce) {
        if (maxAbsForce < 1e-9) return Color.GRAY;
        double intensity = Math.min(1.0, Math.abs(force) / maxAbsForce);
        if (force > 1e-6) {
            // Tension: Red
            return Color.LIGHTGRAY.interpolate(Color.RED, intensity);
        } else if (force < -1e-6) {
            // Compression: Blue
            return Color.LIGHTGRAY.interpolate(Color.BLUE, intensity);
        } else {
            return Color.GRAY;
        }
    }

    private void drawTrussLegend(GraphicsContext g, double maxTensionForce, double maxCompressionForce) {
        double x = 10;
        double y = canvas.getHeight() - 100;
        double w = 180;
        double h = 90;

        g.setFill(Color.color(1, 1, 1, 0.85));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.BLACK);
        g.setLineWidth(1);
        g.strokeRect(x, y, w, h);

        g.setFill(Color.BLACK);
        g.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        g.fillText("Truss Analysis Legend", x + 10, y + 20);

        g.setFont(javafx.scene.text.Font.font("System", 11));
        
        // Tension
        g.setFill(Color.RED);
        g.fillRect(x + 10, y + 30, 20, 10);
        g.setFill(Color.BLACK);
        g.fillText(String.format("Tension (Max: %.2e N)", maxTensionForce), x + 35, y + 39);

        // Compression
        g.setFill(Color.BLUE);
        g.fillRect(x + 10, y + 50, 20, 10);
        g.setFill(Color.BLACK);
        g.fillText(String.format("Compression (Max: %.2e N)", Math.abs(maxCompressionForce)), x + 35, y + 59);

        g.setFill(Color.DARKRED);
        g.fillOval(x + 10, y + 70, 10, 10);
        g.setFill(Color.BLACK);
        g.fillText("Deformed Nodes", x + 35, y + 79);
    }

    private void drawDeformedShape(GraphicsContext g, com.treble.feasimulation.solver.BeamSolver.Result r, double scale) {
        // draw deformed shape in blue
        g.setStroke(Color.CORNFLOWERBLUE);
        g.setLineWidth(2);
        for (Element e : model.getElements()) {
            java.util.Optional<Node> s = model.findNodeById(e.getNodeStartId());
            java.util.Optional<Node> t = model.findNodeById(e.getNodeEndId());
            if (s.isEmpty() || t.isEmpty()) continue;
            Node ns = s.get(); Node nt = t.get();
            int si = -1, ti = -1;
            java.util.List<Node> nodes = model.getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).getId() == ns.getId()) si = i;
                if (nodes.get(i).getId() == nt.getId()) ti = i;
            }
            if (si < 0 || ti < 0) continue;
            double u1 = r.displacements[3*si];
            double v1 = r.displacements[3*si + 1];
            double u2 = r.displacements[3*ti];
            double v2 = r.displacements[3*ti + 1];

            // screen y increases down. Displacements are already in screen orientation-consistent values
            double x1_def = ns.getX() + u1 * scale;
            double y1_def = ns.getY() + v1 * scale;
            double x2_def = nt.getX() + u2 * scale;
            double y2_def = nt.getY() + v2 * scale;

            g.strokeLine(x1_def, y1_def, x2_def, y2_def);
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
        for (Element e : model.getElements()) {
            java.util.Optional<Node> s = model.findNodeById(e.getNodeStartId());
            java.util.Optional<Node> t = model.findNodeById(e.getNodeEndId());
            if (s.isEmpty() || t.isEmpty()) continue;
            Node ns = s.get(); Node nt = t.get();
            // find element result by id
            com.treble.feasimulation.solver.BeamSolver.ElementResult er = null;
            for (com.treble.feasimulation.solver.BeamSolver.ElementResult x : r.elementResults) if (x.elementId == e.getId()) { er = x; break; }
            double m1 = (er==null?0.0:er.endMomentStart);
            double m2 = (er==null?0.0:er.endMomentEnd);

            double dx = nt.getX() - ns.getX();
            double dy = nt.getY() - ns.getY();
            double L = Math.hypot(dx, dy);
            if (L < 1e-6) continue;
            double nx = -dy / L;
            double ny = dx / L;

            double diagramScale = 0.05; // pixels per N*m, heuristic

            for (int i = 0; i < samples; i++) {
                double t0 = (double)i / samples;
                double t1 = (double)(i+1) / samples;
                
                double m0 = m1 + (m2 - m1)*t0;
                double m1v = m1 + (m2 - m1)*t1;

                double x0 = ns.getX() + dx*t0 + nx * m0 * diagramScale;
                double y0 = ns.getY() + dy*t0 + ny * m0 * diagramScale;
                double x1 = ns.getX() + dx*t1 + nx * m1v * diagramScale;
                double y1 = ns.getY() + dy*t1 + ny * m1v * diagramScale;
                
                double mv = 0.5*(m0 + m1v);
                Color c = colorForMoment(mv, maxM);
                g.setStroke(c);
                g.setLineWidth(2);
                g.strokeLine(x0, y0, x1, y1);
                
                // optional: draw connecting lines to base element
                if (i == 0 || i == samples - 1) {
                    g.setLineWidth(1);
                    g.setStroke(Color.LIGHTGRAY);
                    g.strokeLine(ns.getX() + dx * (i == 0 ? 0 : 1), ns.getY() + dy * (i == 0 ? 0 : 1), 
                                 (i == 0 ? x0 : x1), (i == 0 ? y0 : y1));
                }
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

    // --- Tool Implementations ---

    private abstract class BaseElementTool implements CanvasTool {
        protected Integer startNodeId;
        protected Point2D startPoint;
        protected Point2D mousePos;

        protected Node createNode(int id, double x, double y) {
            return new Node(id, x, y);
        }

        @Override
        public void onClick(Point2D p, MouseEvent e) {
            if (e.getButton() != MouseButton.PRIMARY) return;
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            if (startNodeId == null) {
                if (nearNode >= 0) {
                    startNodeId = nearNode;
                    Node n = model.findNodeById(nearNode).get();
                    startPoint = new Point2D(n.getX(), n.getY());
                } else {
                    int nid = model.nextNodeId();
                    model.addNode(createNode(nid, p.getX(), p.getY()));
                    startNodeId = nid;
                    startPoint = p;
                }
            } else {
                int endNodeId;
                if (nearNode >= 0) {
                    endNodeId = nearNode;
                } else {
                    endNodeId = model.nextNodeId();
                    model.addNode(createNode(endNodeId, p.getX(), p.getY()));
                }
                createElement(startNodeId, endNodeId);
                startNodeId = null;
                startPoint = null;
                notifyModelUpdate();
            }
            redraw();
            e.consume();
        }

        protected abstract void createElement(int s, int e);

        @Override public void onDrag(Point2D p, MouseEvent e) { mousePos = p; redraw(); }
        @Override public void onMove(Point2D p, MouseEvent e) { mousePos = p; redraw(); }
        @Override public void onRelease(Point2D p, MouseEvent e) {}

        @Override
        public void drawOverlay(GraphicsContext g) {
            if (startPoint != null) {
                g.setFill(Color.GREEN);
                g.fillOval(startPoint.getX() - NODE_RADIUS, startPoint.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
                if (mousePos != null) {
                    g.setStroke(Color.GRAY);
                    g.setLineDashes(5);
                    g.strokeLine(startPoint.getX(), startPoint.getY(), mousePos.getX(), mousePos.getY());
                    g.setLineDashes(0);
                }
            }
        }
    }

    private class BeamTool extends BaseElementTool {
        @Override
        protected void createElement(int s, int e) {
            int eid = model.nextElementId();
            model.addElement(new BeamElement(eid, s, e, placingBeamMaterialId, 1.0, 1.0));
        }
    }

    private class TrussTool extends BaseElementTool {
        @Override
        protected Node createNode(int id, double x, double y) {
            return new TrussNode(id, x, y);
        }

        @Override
        protected void createElement(int s, int e) {
            int eid = model.nextElementId();
            model.addElement(new TrussMember(eid, s, e, placingBeamMaterialId, 1.0));
        }
    }

    private class PolygonTool extends BaseElementTool {
        @Override
        protected void createElement(int s, int e) {
            int eid = model.nextElementId();
            model.addElement(new BeamElement(eid, s, e, placingBeamMaterialId, 1.0, 1.0));
        }
    }

    private Node createNodeForCurrentContext(int id, double x, double y) {
        // If we are primarily a truss (or last tool used was truss), create a TrussNode
        if (activeTool instanceof TrussTool) {
            return new TrussNode(id, x, y);
        }
        // Fallback: check if model is already mostly truss? 
        // For simplicity, let's just check the active tool or use generic Node.
        // Support and Load tools should ideally use the type of the structure.
        boolean hasBeams = model.getElements().stream().anyMatch(e -> e instanceof BeamElement);
        boolean hasTruss = model.getElements().stream().anyMatch(e -> e instanceof TrussElement);
        
        if (hasTruss && !hasBeams) {
            return new TrussNode(id, x, y);
        }
        return new Node(id, x, y);
    }

    private class SupportTool implements CanvasTool {
        @Override
        public void onClick(Point2D p, MouseEvent e) {
            if (e.getButton() != MouseButton.PRIMARY) return;
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            int nodeId;
            if (nearNode >= 0) {
                nodeId = nearNode;
            } else {
                nodeId = model.nextNodeId();
                model.addNode(createNodeForCurrentContext(nodeId, p.getX(), p.getY()));
            }
            int sid = model.nextSupportId();
            model.addSupport(new Support(sid, nodeId, placingSupportType));
            notifyModelUpdate();
            redraw();
            e.consume();
        }
        @Override public void onDrag(Point2D p, MouseEvent e) {}
        @Override public void onMove(Point2D p, MouseEvent e) {}
        @Override public void onRelease(Point2D p, MouseEvent e) {}
        @Override public void drawOverlay(GraphicsContext g) {}
    }

    private class LoadTool implements CanvasTool {
        @Override
        public void onClick(Point2D p, MouseEvent e) {
            if (e.getButton() != MouseButton.PRIMARY) return;
            int nearNode = findNearestNodeId(p, HIT_TOLERANCE);
            int nodeId;
            if (nearNode >= 0) {
                nodeId = nearNode;
            } else {
                Optional<ElementHit> hit = findNearestElementHit(p, HIT_TOLERANCE);
                if (hit.isEmpty()) {
                    nodeId = model.nextNodeId();
                    model.addNode(createNodeForCurrentContext(nodeId, p.getX(), p.getY()));
                } else {
                    nodeId = model.splitElementAtPoint(hit.get().elementId, hit.get().projectedPoint.getX(), hit.get().projectedPoint.getY());
                }
            }
            int lid = model.nextPointLoadId();
            model.addPointLoad(new PointLoad(lid, nodeId, placingFx, placingFy));
            notifyModelUpdate();
            redraw();
            e.consume();
        }
        @Override public void onDrag(Point2D p, MouseEvent e) {}
        @Override public void onMove(Point2D p, MouseEvent e) {}
        @Override public void onRelease(Point2D p, MouseEvent e) {}
        @Override public void drawOverlay(GraphicsContext g) {}
    }
}

