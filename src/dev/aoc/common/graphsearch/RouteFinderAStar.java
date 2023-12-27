package dev.aoc.common.graphsearch;

import java.util.*;

public class RouteFinderAStar<T extends GraphNode> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;

    public RouteFinderAStar(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    public List<T> findRoute(T from, T to) {
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
                return route;
            }

            graph.getConnections(currentNode).forEach(connection -> {
                RouteNode<T> nextNode = allNodes.computeIfAbsent(connection, key -> new RouteNode<>(connection));
                long newScore = current.getRouteScore() + nextNodeScorer.computeCost(currentNode, connection);
                if (newScore < nextNode.getRouteScore()) {
                    nextNode.setPrevious(current);
                    nextNode.setRouteScore(newScore);
                    nextNode.setEstimatedScore(newScore + targetScorer.computeCost(connection, to));
                    if (!openSet.contains(nextNode)) {
                        openSet.add(nextNode);
                    }
                }
            });
        }

        throw new IllegalStateException("no route found");
    }
}
