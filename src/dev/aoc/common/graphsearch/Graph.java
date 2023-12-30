package dev.aoc.common.graphsearch;

import java.util.Set;

public interface Graph<T extends GraphNode> {
    T getNode(long id);
    Set<T> getEdges(T node);
}
