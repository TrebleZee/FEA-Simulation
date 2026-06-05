package com.treble.feasimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Central model container for FEA data. For Phase 1 we store BeamElement instances in the elements list.
 */
public class FEAData {
    private final List<Node> nodes = new ArrayList<>();
    private final List<Element> elements = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<Load> loads = new ArrayList<>();

    // new collections for beam-specific features
    private final List<Support> supports = new ArrayList<>();
    private final List<PointLoad> pointLoads = new ArrayList<>();

    public void addNode(Node n) { nodes.add(n); }
    public void addElement(Element e) { elements.add(e); }
    public void addMaterial(Material m) { materials.add(m); }
    public void addLoad(Load l) { loads.add(l); }

    public void addSupport(Support s) { supports.add(s); }
    public void addPointLoad(PointLoad p) { pointLoads.add(p); }

    public void clear() {
        nodes.clear();
        elements.clear();
        loads.clear();
        supports.clear();
        pointLoads.clear();
    }

    public List<Node> getNodes() { return Collections.unmodifiableList(nodes); }
    public List<Element> getElements() { return Collections.unmodifiableList(elements); }
    public List<Material> getMaterials() { return Collections.unmodifiableList(materials); }
    public List<Load> getLoads() { return Collections.unmodifiableList(loads); }
    public List<Support> getSupports() { return Collections.unmodifiableList(supports); }
    public List<PointLoad> getPointLoads() { return Collections.unmodifiableList(pointLoads); }

    // Dedicated id generators for clarity
    public int nextSupportId() { return supports.stream().mapToInt(Support::getId).max().orElse(0) + 1; }
    public int nextPointLoadId() { return pointLoads.stream().mapToInt(PointLoad::getId).max().orElse(0) + 1; }

    // Removal helpers with cascade cleanup
    public boolean removeSupportById(int id) { return supports.removeIf(s -> s.getId() == id); }
    public boolean removePointLoadById(int id) { return pointLoads.removeIf(p -> p.getId() == id); }

    public boolean removeElementById(int id) {
        return elements.removeIf(e -> e.getId() == id);
    }

    /**
     * Split an existing beam element by inserting a node at the supplied coordinates.
     * The original element is replaced by two shorter elements that reuse its properties.
     *
     * @return the inserted node id, or an existing endpoint node id if the point lies
     *         exactly on one of the element's ends.
     */
    public int splitElementAtPoint(int elementId, double x, double y) {
        Element original = findElementById(elementId)
                .orElseThrow(() -> new IllegalArgumentException("Element not found: " + elementId));

        Node start = findNodeById(original.getNodeStartId())
                .orElseThrow(() -> new IllegalStateException("Start node not found for element " + elementId));
        Node end = findNodeById(original.getNodeEndId())
                .orElseThrow(() -> new IllegalStateException("End node not found for element " + elementId));

        double endpointTolerance = 1e-6;
        if (Math.hypot(x - start.getX(), y - start.getY()) <= endpointTolerance) {
            return start.getId();
        }
        if (Math.hypot(x - end.getX(), y - end.getY()) <= endpointTolerance) {
            return end.getId();
        }

        int newNodeId = nextNodeId();
        addNode(new Node(newNodeId, x, y));

        removeElementById(elementId);
        if (original instanceof BeamElement be) {
            addElement(new BeamElement(nextElementId(), start.getId(), newNodeId,
                    be.getMaterialId(), be.getArea(), be.getInertia()));
            addElement(new BeamElement(nextElementId(), newNodeId, end.getId(),
                    be.getMaterialId(), be.getArea(), be.getInertia()));
        } else if (original instanceof TrussElement te) {
            addElement(new TrussElement(nextElementId(), start.getId(), newNodeId,
                    te.getMaterialId(), te.getArea()));
            addElement(new TrussElement(nextElementId(), newNodeId, end.getId(),
                    te.getMaterialId(), te.getArea()));
        } else {
            throw new UnsupportedOperationException("Splitting not supported for element type: " + original.getClass().getSimpleName());
        }

        return newNodeId;
    }

    public boolean removeNodeById(int id) {
        boolean removed = nodes.removeIf(n -> n.getId() == id);
        if (removed) {
            // remove elements referencing the node
            elements.removeIf(e -> {
                for (int nodeId : e.getNodeIds()) {
                    if (nodeId == id) return true;
                }
                return false;
            });
            // remove supports and point loads attached to the node
            supports.removeIf(s -> s.getNodeId() == id);
            pointLoads.removeIf(p -> p.getNodeId() == id);
        }
        return removed;
    }

    // Lookup helpers
    public Optional<Node> findNodeById(int id) {
        return nodes.stream().filter(n -> n.getId() == id).findFirst();
    }

    public Optional<Element> findElementById(int id) {
        return elements.stream().filter(e -> e.getId() == id).findFirst();
    }

    public boolean updateNode(Node updated) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId() == updated.getId()) {
                nodes.set(i, updated);
                return true;
            }
        }
        return false;
    }

    // Simple id generators based on current lists
    public int nextNodeId() {
        return nodes.stream().mapToInt(Node::getId).max().orElse(0) + 1;
    }

    public int nextElementId() {
        return elements.stream().mapToInt(Element::getId).max().orElse(0) + 1;
    }
}

