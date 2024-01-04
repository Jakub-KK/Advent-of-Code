package dev.aoc.common.graphsearch;

import org.javatuples.Pair;

import java.util.*;

public class RouteFinderMax<T extends GraphNode> implements RouteFinder<T> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;

    public RouteFinderMax(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    private record State<T extends GraphNode>(RouteNodeEstimated<T> node, List<RouteNodeEstimated<T>> connections) {}

    private State<T> createState(RouteNodeEstimated<T> node, Map<T, RouteNodeEstimated<T>> allNodes, T to) {
        List<RouteNodeEstimated<T>> connections = graph.getEdges(node.getCurrent()).stream().map(connection -> {
            RouteNodeEstimated<T> nextNode = allNodes.computeIfAbsent(connection, key -> new RouteNodeEstimated<>(connection));
            if ((node.getPrevious() != null && nextNode.equals(node.getPrevious())) || nextNode.getPrevious() != null) {
                return null; // skip going back and ignore visited connections
            }
            // long newScore = node.getRouteScore() + nextNodeScorer.computeCost(node.getCurrent(), connection);
            // nextNode.setPrevious(node);
            // nextNode.setRouteScore(newScore);
            // nextNode.setEstimatedScore(newScore + targetScorer.computeCost(connection, to));
            return nextNode;
        }).filter(Objects::nonNull).toList();
        List<RouteNodeEstimated<T>> connectionsSet = new ArrayList<>(connections);
        connectionsSet.sort(RouteNodeEstimated::compareTo);
        return new State<>(node, connectionsSet);
    }

    protected void foundRoute(List<T> route, long score) {}

    public Pair<List<T>, Long> findRoute(T start, T target) {
        return findRoute(List.of(start), target);
    }
    public Pair<List<T>, Long> findRoute(Iterable<T> starts, T target) {
        Map<T, RouteNodeEstimated<T>> allNodes = new HashMap<>();
        List<State<T>> state = new LinkedList<>();

        for (T from : starts) {
            RouteNodeEstimated<T> start = new RouteNodeEstimated<>(from, null, 0, targetScorer.computeCost(from, target));
            allNodes.put(from, start);
            state.add(createState(start, allNodes, target));
        }

        List<T> maxRoute = null;
        long maxScore = Long.MIN_VALUE;

        while (!state.isEmpty()) {
            State<T> last = state.getLast();
            RouteNodeEstimated<T> current = last.node;
            if (current.getCurrent().equals(target)) {
                // found the route
                List<T> route = new ArrayList<>();
                RouteNodeEstimated<T> routeNode = current;
                do {
                    route.addFirst(routeNode.getCurrent());
                    routeNode = routeNode.getPrevious();
                } while (routeNode != null);
                foundRoute(route, current.getRouteScore());
                if (maxScore < current.getRouteScore()) {
                    maxScore = current.getRouteScore();
                    // System.out.printf("found better route, score %d%n", maxScore);
                    maxRoute = route;
                }
                last.connections.clear();
            }
            if (last.connections.isEmpty()) {
                current.setPrevious(null);
                state.removeLast();
                continue;
            }
            RouteNodeEstimated<T> nextNode = last.connections.removeFirst();
            long newScore = current.getRouteScore() + nextNodeScorer.computeCost(current.getCurrent(), nextNode.getCurrent());
            nextNode.setPrevious(current);
            nextNode.setRouteScore(newScore);
            nextNode.setEstimatedScore(newScore + targetScorer.computeCost(current.getCurrent(), target));
            State<T> nextState = createState(nextNode, allNodes, target);
            state.add(nextState);
        }
        if (maxRoute == null) {
            throw new IllegalStateException("no route found");
        }
        return new Pair<>(maxRoute, maxScore);
    }
}
