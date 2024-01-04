package dev.aoc.common.graphsearch;

import org.javatuples.Pair;

import java.util.List;
import java.util.function.Predicate;

public interface RouteFinder<T extends GraphNode> {
    Pair<List<T>, Long> findRoute(T start, T target);
    Pair<List<T>, Long> findRoute(Iterable<T> starts, T target);
}
