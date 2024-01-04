package dev.aoc.common.graphsearch;

public interface GraphNode {
    long getId();
    /** Returns true if this node is equal to target node. This is not necessarily the same as strict nodes equality */
    boolean equalsTarget(GraphNode target);
}
