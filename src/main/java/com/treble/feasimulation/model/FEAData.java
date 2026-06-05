package com.treble.feasimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FEAData {
    private final List<Node> nodes = new ArrayList<>();
    private final List<Element> elements = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<Load> loads = new ArrayList<>();

    public void addNode(Node n) { nodes.add(n); }
    public void addElement(Element e) { elements.add(e); }
    public void addMaterial(Material m) { materials.add(m); }
    public void addLoad(Load l) { loads.add(l); }

    public List<Node> getNodes() { return Collections.unmodifiableList(nodes); }
    public List<Element> getElements() { return Collections.unmodifiableList(elements); }
    public List<Material> getMaterials() { return Collections.unmodifiableList(materials); }
    public List<Load> getLoads() { return Collections.unmodifiableList(loads); }

    // Lookup helpers
    public Optional<Node> findNodeById(int id) {
        return nodes.stream().filter(n -> n.getId() == id).findFirst();
    }

    public Optional<Element> findElementById(int id) {
        return elements.stream().filter(e -> e.getId() == id).findFirst();
    }

    // Remove/replace helpers
    public boolean removeElementById(int id) {
        return elements.removeIf(e -> e.getId() == id);
    }

    public boolean removeNodeById(int id) {
        return nodes.removeIf(n -> n.getId() == id);
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
