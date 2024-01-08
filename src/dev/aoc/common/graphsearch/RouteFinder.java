package dev.aoc.common.graphsearch;

import org.javatuples.Pair;

import java.util.List;
import java.util.function.Predicate;

public interface RouteFinder<T extends GraphNode> {
    Pair<List<T>, Long> findRoute(T startNode, T targetNode);
    Pair<List<T>, Long> findRoute(Iterable<T> startNodes, T targetNode);
}
