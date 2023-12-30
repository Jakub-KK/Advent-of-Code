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

    private record State<T extends GraphNode>(RouteNode<T> node, List<RouteNode<T>> connections) {}

    private State<T> createState(RouteNode<T> node, Map<T, RouteNode<T>> allNodes, T to) {
        List<RouteNode<T>> connections = graph.getEdges(node.getCurrent()).stream().map(connection -> {
            RouteNode<T> nextNode = allNodes.computeIfAbsent(connection, key -> new RouteNode<>(connection));
            if ((node.getPrevious() != null && nextNode.equals(node.getPrevious())) || nextNode.getPrevious() != null) {
                return null; // skip going back and ignore visited connections
            }
            // long newScore = node.getRouteScore() + nextNodeScorer.computeCost(node.getCurrent(), connection);
            // nextNode.setPrevious(node);
            // nextNode.setRouteScore(newScore);
            // nextNode.setEstimatedScore(newScore + targetScorer.computeCost(connection, to));
            return nextNode;
        }).filter(Objects::nonNull).toList();
        List<RouteNode<T>> connectionsSet = new ArrayList<>(connections);
        connectionsSet.sort(RouteNode::compareTo);
        return new State<>(node, connectionsSet);
    }

    protected void foundRoute(List<T> route, long score) {
    }

    public Pair<List<T>, Long> findRoute(T from, T to) {
        Map<T, RouteNode<T>> allNodes = new HashMap<>();
        List<State<T>> state = new LinkedList<>();

        RouteNode<T> start = new RouteNode<>(from, null, 0, targetScorer.computeCost(from, to));
        allNodes.put(from, start);
        state.add(createState(start, allNodes, to));

        List<T> maxRoute = null;
        long maxScore = Long.MIN_VALUE;

        // [1,0], [3,5], [5,13], [13,19], [13,13], [11,3], [21,11], [19,19], [21,22]
        while (!state.isEmpty()) {
            State<T> last = state.getLast();
            RouteNode<T> current = last.node;
            if (current.getCurrent().equals(to)) {
                // found the route
                List<T> route = new ArrayList<>();
                RouteNode<T> routeNode = current;
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
            RouteNode<T> nextNode = last.connections.removeFirst();
            long newScore = current.getRouteScore() + nextNodeScorer.computeCost(current.getCurrent(), nextNode.getCurrent());
            nextNode.setPrevious(current);
            nextNode.setRouteScore(newScore);
            nextNode.setEstimatedScore(newScore + targetScorer.computeCost(current.getCurrent(), to));
            State<T> nextState = createState(nextNode, allNodes, to);
            state.add(nextState);
        }
        if (maxRoute == null) {
            throw new IllegalStateException("no route found");
        }
        return new Pair<>(maxRoute, maxScore);
    }
}
