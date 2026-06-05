package com.treble.feasimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
