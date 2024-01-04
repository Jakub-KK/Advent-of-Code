package dev.aoc.common.graphsearch;

import org.teneighty.heap.Heap;

/** A route node with estimated score and heap entry reference */
public class RouteNodeEstimatedPlusHeapRef<T extends GraphNode> extends RouteNodeEstimated<T> {
    private Heap.Entry<RouteNodeEstimatedPlusHeapRef<T>, RouteNodeEstimatedPlusHeapRef<T>> heapEntry;

    public RouteNodeEstimatedPlusHeapRef(T current) {
        super(current);
    }

    public RouteNodeEstimatedPlusHeapRef(T current, RouteNodeEstimated<T> previous, long routeScore, long estimatedScore) {
        super(current, previous, routeScore, estimatedScore);
    }

    public Heap.Entry<RouteNodeEstimatedPlusHeapRef<T>, RouteNodeEstimatedPlusHeapRef<T>> getHeapEntry() {
        return heapEntry;
    }

    public void setHeapEntry(Heap.Entry<RouteNodeEstimatedPlusHeapRef<T>, RouteNodeEstimatedPlusHeapRef<T>> heapEntry) {
        this.heapEntry = heapEntry;
    }
}
