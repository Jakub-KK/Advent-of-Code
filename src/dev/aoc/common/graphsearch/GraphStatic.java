package dev.aoc.common.graphsearch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphStatic<T extends GraphNode> implements Graph<T> {
    private final Set<T> nodes;
    // private final Map<Long, T> nodes;
    private final Map<Long, Set<Long>> connections;

    public GraphStatic() {
        nodes = new HashSet<>();
        // nodes = new HashMap<>();
        connections = new HashMap<>();
    }

    // public T getNodeOrCreate(long id, Supplier<T> nodeSupplier) {
    //     return nodes.computeIfAbsent(id, key -> nodeSupplier.get());
    // }

    // public boolean hasNode(long id) {
    //     return nodes.containsKey(id);
    // }

    // public void addNode(T node) {
    //     nodes.put(node.getId(), node);
    // }

    public T getNode(long id) {
        // return nodes.get(id);
        return nodes.stream()
                .filter(node -> node.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No node found with ID"));
    }

    // public void addConnection(T fromNode, T toNode) {
    //     Set<Long> fromConnections = connections.computeIfAbsent(fromNode.getId(), id -> new HashSet<>());
    //     fromConnections.add(toNode.getId());
    // }

    public Set<T> getConnections(T node) {
        return connections.get(node.getId()).stream()
                .map(this::getNode)
                .collect(Collectors.toSet());
    }
}
