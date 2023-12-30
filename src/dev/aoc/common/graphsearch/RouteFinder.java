package dev.aoc.common.graphsearch;

import org.javatuples.Pair;

import java.util.List;

public interface RouteFinder<T extends GraphNode> {
    Pair<List<T>, Long> findRoute(T from, T to);
}
