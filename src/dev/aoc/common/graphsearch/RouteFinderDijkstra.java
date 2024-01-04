package dev.aoc.common.graphsearch;

import org.teneighty.heap.FibonacciHeap;
import org.teneighty.heap.Heap;

import java.util.*;

public class RouteFinderDijkstra<T extends GraphNode> {
    private final Graph<T> graph;
    private final Scorer<T> nodeScorer;
    public RouteFinderDijkstra(Graph<T> graph, Scorer<T> nodeScorer) {
        this.graph = graph;
        this.nodeScorer = nodeScorer;
    }
    /** Searches graph computing minimal cost for getting from every node to given start node.
     * Uses Fibonnaci Heap instead of Priority Queue for efficiency. See https://gabormakrai.wordpress.com/2015/02/11/experimenting-with-dijkstras-algorithm/ */
    public Map<T, Long> search(T startNode) {
        // Queue<RouteNode<T>> openSet = new PriorityQueue<>();
        var openSet = new FibonacciHeap<RouteNodePlusHeapRef<T>, RouteNodePlusHeapRef<T>>(Comparator.naturalOrder());
        Map<T, RouteNodePlusHeapRef<T>> all = new HashMap<>();
        Map<T, Long> scores = new HashMap<>();

        var start = new RouteNodePlusHeapRef<T>(startNode, null, 0);
        // openSet.add(startNode);
        var startHeapEntry = openSet.insert(start, start);
        start.setHeapEntry(startHeapEntry);
        all.put(startNode, start);

        while (!openSet.isEmpty()) {
            RouteNodePlusHeapRef<T> current = openSet.extractMinimum().getValue();
            current.setHeapEntry(null); // we don't need heap reference no more as this route node is off the open set
            T currentNode = current.getCurrent();
            scores.put(currentNode, current.getRouteScore());

            graph.getEdges(currentNode).forEach(nextNode -> {
                var next = all.computeIfAbsent(nextNode, key -> new RouteNodePlusHeapRef<>(nextNode));
                long newScore = current.getRouteScore() + nodeScorer.computeCost(currentNode, currentNode);
                if (newScore < next.getRouteScore()) {
                    // boolean isNew = next.isUninitialized();
                    next.setPrevious(current);
                    next.setRouteScore(newScore);
                    // should use decrease_key operation but standard java PQ doesn't support it
                    // without forcing PQ to reorder, we risk processing nodes out-of-order and thus invalid result
                    // if (!isNew) {
                    //     openSet.remove(next);
                    // }
                    // openSet.add(next);
                    var heapEntry = next.getHeapEntry();
                    if (heapEntry != null) {
                        openSet.decreaseKey(heapEntry, next); // decrease key is faster than remove/insert if already part of the open set
                    } else {
                        openSet.insert(next, next);
                    }
                }
            });
        }
        return scores;
    }
}
