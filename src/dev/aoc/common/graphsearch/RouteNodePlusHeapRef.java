package dev.aoc.common.graphsearch;

import org.teneighty.heap.Heap;

/** A route node with heap entry reference */
public class RouteNodePlusHeapRef<T extends GraphNode> extends RouteNode<T> {
    private Heap.Entry<RouteNodePlusHeapRef<T>, RouteNodePlusHeapRef<T>> heapEntry;

    public RouteNodePlusHeapRef(T current) {
        super(current);
    }

    public RouteNodePlusHeapRef(T current, RouteNode<T> previous, long routeScore) {
        super(current, previous, routeScore);
    }

    public Heap.Entry<RouteNodePlusHeapRef<T>, RouteNodePlusHeapRef<T>> getHeapEntry() {
        return heapEntry;
    }

    public void setHeapEntry(Heap.Entry<RouteNodePlusHeapRef<T>, RouteNodePlusHeapRef<T>> heapEntry) {
        this.heapEntry = heapEntry;
    }
}
