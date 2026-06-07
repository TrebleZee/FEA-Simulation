package com.treble.feasimulation.model;

import java.util.List;

public class Mesh {
    private final List<Node> nodes;
    private final List<Element> elements;

    public Mesh(List<Node> nodes, List<Element> elements) {
        this.nodes = nodes;
        this.elements = elements;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Element> getElements() {
        return elements;
    }
}
