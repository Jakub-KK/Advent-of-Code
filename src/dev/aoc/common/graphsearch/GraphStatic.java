package dev.aoc.common.graphsearch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphStatic<T extends GraphNode> implements Graph<T> {
    // private final Set<T> nodes;
    protected final Map<Long, T> nodes;
    protected final Map<Long, Set<Long>> edges;

    public GraphStatic() {
        // nodes = new HashSet<>();
        nodes = new HashMap<>();
        edges = new HashMap<>();
    }
    public GraphStatic(GraphStatic<T> that) {
        this.nodes = new HashMap<>(that.nodes);
        this.edges = new HashMap<>(that.edges);
    }

    // public T getNodeOrCreate(long id, Supplier<T> nodeSupplier) {
    //     return nodes.computeIfAbsent(id, key -> nodeSupplier.get());
    // }

    public Set<T> getNodes() {
        return new HashSet<>(nodes.values());
    }

    public boolean hasNode(long id) {
        return nodes.containsKey(id);
    }

    public void addNode(T node) {
        nodes.putIfAbsent(node.getId(), node);
    }

    public T getNode(long id) {
        return nodes.get(id);
        // return nodes.stream()
        //         .filter(node -> node.getId() == id)
        //         .findFirst()
        //         .orElseThrow(() -> new IllegalArgumentException("No node found with ID"));
    }

    public void addEdge(T fromNode, T toNode) {
        addNode(fromNode);
        addNode(toNode);
        Set<Long> fromConnections = edges.computeIfAbsent(fromNode.getId(), id -> new HashSet<>());
        fromConnections.add(toNode.getId());
    }

    public Set<T> getEdges(T node) {
        return !edges.containsKey(node.getId()) ? Set.of() : edges.get(node.getId()).stream()
                .map(this::getNode)
                .collect(Collectors.toSet());
    }
}
