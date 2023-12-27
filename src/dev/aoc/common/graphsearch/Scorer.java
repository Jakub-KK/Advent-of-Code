package dev.aoc.common.graphsearch;

public interface Scorer<T extends GraphNode> {
    long computeCost(T from, T to);
}
