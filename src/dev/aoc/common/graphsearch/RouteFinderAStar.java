package dev.aoc.common.graphsearch;

import org.javatuples.Pair;
import org.teneighty.heap.FibonacciHeap;

import java.util.*;

/** source: https://www.baeldung.com/java-a-star-pathfinding */
public class RouteFinderAStar<T extends GraphNode> implements RouteFinder<T> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;

    public RouteFinderAStar(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    public FoundRouteDecision foundRoute(List<T> route, long score) {
        return FoundRouteDecision.ABORT_SEARCH; // abort search as soon as the first route is found
    }

    public Pair<List<T>, Long> findRoute(T startNode, T targetNode) {
        return findRoute(List.of(startNode), targetNode);
    }
    public Pair<List<T>, Long> findRoute(Iterable<T> startNodes, T targetNode) {
        var openSet = new FibonacciHeap<RouteNodeEstimatedPlusHeapRef<T>, RouteNodeEstimatedPlusHeapRef<T>>(Comparator.naturalOrder());
        Map<T, RouteNodeEstimatedPlusHeapRef<T>> all = new HashMap<>();

        for (T startNode : startNodes) {
            var start = new RouteNodeEstimatedPlusHeapRef<>(startNode, null, 0, targetScorer != null ? targetScorer.computeCost(startNode, targetNode) : 0);
            var startHeapEntry = openSet.insert(start, start);
            start.setHeapEntry(startHeapEntry);
            all.put(startNode, start);
        }

        List<T> foundRoute = null;
        long foundScore = Long.MAX_VALUE;
        // int maxOpenSetCount = openSet.getSize();
        // int countClosedNodes = 0;

        while (!openSet.isEmpty()) {
            // System.out.printf("routing: open %d%n", openSet.size());
            RouteNodeEstimatedPlusHeapRef<T> current = openSet.extractMinimum().getValue();
            current.setHeapEntry(null); // we don't need heap reference no more as this route node is off the open set
            T currentNode = current.getCurrent();
            long currentRouteScore = current.getRouteScore();
            // countClosedNodes++;
            if (currentNode.equalsTarget(targetNode)) {
                // System.out.printf("score %d%n", current.getRouteScore());
                List<T> route = new ArrayList<>();
                RouteNodeEstimated<T> routeNode = current;
                do {
                    route.addFirst(routeNode.getCurrent());
                    routeNode = routeNode.getPrevious();
                } while (routeNode != null);
                FoundRouteDecision decision = foundRoute(route, currentRouteScore);
                if (decision != FoundRouteDecision.IGNORE) {
                    foundScore = currentRouteScore;
                    foundRoute = route;
                    if (decision == FoundRouteDecision.ABORT_SEARCH) {
                        break;
                    }
                }
                // foundRoute(route, current.getRouteScore());
                // if (foundScore > current.getRouteScore()) {
                //     foundScore = current.getRouteScore();
                //     // System.out.printf("found better route, score %d%n", foundScore);
                //     foundRoute = route;
                // }
            }

            for (T nextNode : graph.getEdges(currentNode)) {
                var next = all.computeIfAbsent(nextNode, key -> new RouteNodeEstimatedPlusHeapRef<>(nextNode));
                long newScore = current.getRouteScore() + nextNodeScorer.computeCost(currentNode, nextNode);
                if (newScore < next.getRouteScore()) {
                    next.setPrevious(current);
                    next.setRouteScore(newScore);
                    next.setEstimatedScore(newScore + (targetScorer != null ? targetScorer.computeCost(nextNode, targetNode) : 0));
                    var heapEntry = next.getHeapEntry();
                    if (heapEntry != null) {
                        openSet.decreaseKey(heapEntry, next); // decrease key is faster than remove/insert if already part of the open set
                    } else {
                        openSet.insert(next, next);
                    }
                }
            }

            // if (maxOpenSetCount < openSet.getSize()) {
            //     maxOpenSetCount = openSet.getSize();
            // }
        }

        // throw new IllegalStateException("no route found");
        // if (foundRoute == null) {
        //     throw new IllegalStateException("no route found");
        // }
        return foundRoute != null ? new Pair<>(foundRoute, foundScore) : null;
    }

    /** This code is left for benchmark/comparison, it uses Java standard PriorityQueue.
     * Due to lack of "decrese-key" operation it has worse performance than FibonacciHeap. */
    public Pair<List<T>, Long> findRoutePQ(Iterable<T> startNodes, T target) {
        Queue<RouteNodeEstimated<T>> openSet = new PriorityQueue<>();
        Map<T, RouteNodeEstimated<T>> all = new HashMap<>();

        for (T startNode : startNodes) {
            RouteNodeEstimated<T> start = new RouteNodeEstimated<>(startNode, null, 0, targetScorer != null ? targetScorer.computeCost(startNode, target) : 0);
            openSet.add(start);
            all.put(startNode, start);
        }

        List<T> foundRoute = null;
        long foundScore = Long.MAX_VALUE;

        while (!openSet.isEmpty()) {
            // System.out.printf("routing: open %d%n", openSet.size());
            RouteNodeEstimated<T> current = openSet.poll();
            T currentNode = current.getCurrent();
            long currentRouteScore = current.getRouteScore();
            if (currentNode.equalsTarget(target)) {
                // System.out.printf("score %d%n", current.getRouteScore());
                List<T> route = new ArrayList<>();
                RouteNodeEstimated<T> routeNode = current;
                do {
                    route.addFirst(routeNode.getCurrent());
                    routeNode = routeNode.getPrevious();
                } while (routeNode != null);
                FoundRouteDecision decision = foundRoute(route, currentRouteScore);
                if (decision != FoundRouteDecision.IGNORE) {
                    foundScore = currentRouteScore;
                    foundRoute = route;
                    if (decision == FoundRouteDecision.ABORT_SEARCH) {
                        break;
                    }
                }
                // foundRoute(route, current.getRouteScore());
                // if (foundScore > current.getRouteScore()) {
                //     foundScore = current.getRouteScore();
                //     // System.out.printf("found better route, score %d%n", foundScore);
                //     foundRoute = route;
                // }
            }

            for (T nextNode : graph.getEdges(currentNode)) {
                var next = all.computeIfAbsent(nextNode, key -> new RouteNodeEstimated<>(nextNode));
                long newScore = current.getRouteScore() + nextNodeScorer.computeCost(currentNode, nextNode);
                if (newScore < next.getRouteScore()) {
                    boolean isNew = next.isUninitialized();
                    next.setPrevious(current);
                    next.setRouteScore(newScore);
                    next.setEstimatedScore(newScore + (targetScorer != null ? targetScorer.computeCost(nextNode, target) : 0));
                    // should use decrease_key operation but standard java PQ doesn't support it
                    // without forcing PQ to reorder, we risk processing nodes out-of-order and thus invalid result
                    if (!isNew) {
                        openSet.remove(next);
                    }
                    openSet.add(next);
                    // note: original implementation from source article at baeldung.com just added next node to open set
                    // this is not sufficient for standard java PQ and will lead to errors (likely it sees the same reference as added already and does not reorder)
                }
            }
        }

        // if (foundRoute == null) {
        //     throw new IllegalStateException("no route found");
        // }
        return foundRoute != null ? new Pair<>(foundRoute, foundScore) : null;
    }
}
