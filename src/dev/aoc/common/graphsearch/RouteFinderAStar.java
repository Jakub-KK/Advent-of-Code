package dev.aoc.common.graphsearch;

import org.javatuples.Pair;

import java.util.*;

public class RouteFinderAStar<T extends GraphNode> implements RouteFinder<T> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;

    public RouteFinderAStar(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    public Pair<List<T>, Long> findRoute(T from, T to) {
        Queue<RouteNode<T>> openSet = new PriorityQueue<>();
        Map<T, RouteNode<T>> allNodes = new HashMap<>();

        RouteNode<T> start = new RouteNode<>(from, null, 0, targetScorer.computeCost(from, to));
        openSet.add(start);
        allNodes.put(from, start);

        while (!openSet.isEmpty()) {
            // System.out.printf("routing: open %d%n", openSet.size());
            RouteNode<T> current = openSet.poll();
            T currentNode = current.getCurrent();
            if (currentNode.equals(to)) {
                // System.out.printf("score %d%n", current.getRouteScore());
                List<T> route = new ArrayList<>();
                RouteNode<T> routeNode = current;
                do {
                    route.addFirst(routeNode.getCurrent());
                    routeNode = routeNode.getPrevious();
                } while (routeNode != null);
                return new Pair<>(route, current.getRouteScore());
            }

            graph.getEdges(currentNode).forEach(connNode -> {
                RouteNode<T> nextNode = allNodes.computeIfAbsent(connNode, key -> new RouteNode<>(connNode));
                long newScore = current.getRouteScore() + nextNodeScorer.computeCost(currentNode, connNode);
                if (newScore < nextNode.getRouteScore()) {
                    nextNode.setPrevious(current);
                    nextNode.setRouteScore(newScore);
                    nextNode.setEstimatedScore(newScore + targetScorer.computeCost(connNode, to));
                    if (!openSet.contains(nextNode)) {
                        openSet.add(nextNode);
                    }
                }
            });
        }

        throw new IllegalStateException("no route found");
    }
}
