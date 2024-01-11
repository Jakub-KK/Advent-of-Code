package dev.aoc.common.graphsearch;

import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class RouteFinderDFS<T extends GraphNode> implements RouteFinder<T> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;
    private final Comparator<RouteNodeEstimated<T>> nodeComparator;

    public RouteFinderDFS(Graph<T> graph) {
        this(graph, null, null, null);
    }
    public RouteFinderDFS(Graph<T> graph, Scorer<T> nextNodeScorer) {
        this(graph, nextNodeScorer, null, null);
    }
    public RouteFinderDFS(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this(graph, nextNodeScorer, targetScorer, null);
    }
    public RouteFinderDFS(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer, Comparator<RouteNodeEstimated<T>> nodeComparator) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
        this.nodeComparator = nodeComparator;
    }

    private record State<T extends GraphNode>(RouteNodeEstimated<T> node, List<RouteNodeEstimated<T>> nexts) {}

    private State<T> createState(RouteNodeEstimated<T> current, Map<T, RouteNodeEstimated<T>> all, T targetNode) {
        List<RouteNodeEstimated<T>> nexts = graph.getEdges(current.getCurrent()).stream()
                .map(nextNode -> {
                    RouteNodeEstimated<T> next = all.computeIfAbsent(nextNode, key -> new RouteNodeEstimated<>(nextNode));
                    if ((current.getPrevious() != null && next.equals(current.getPrevious())) || next.getPrevious() != null) {
                        return null; // skip going back and ignore visited connections
                    }
                    return next;
                })
                .filter(Objects::nonNull)
                .sorted(nodeComparator != null ? nodeComparator : RouteNodeEstimated::compareTo)
                .collect(Collectors.toList())
                ;
        return new State<>(current, nexts);
    }

    @Override
    public FoundRouteDecision foundRoute(List<T> route, long score) {
        return FoundRouteDecision.IGNORE; // ignore every found route, search exhaustively to the end
    }

    public Pair<List<T>, Long> findRoute(T startNode, T targetNode) {
        return findRoute(List.of(startNode), targetNode);
    }
    public Pair<List<T>, Long> findRoute(Iterable<T> startNodes, T targetNode) {
        Map<T, RouteNodeEstimated<T>> all = new HashMap<>();
        List<State<T>> state = new LinkedList<>();

        for (T startNode : startNodes) {
            RouteNodeEstimated<T> start = new RouteNodeEstimated<>(startNode, null, 0, targetScorer != null ? targetScorer.computeCost(startNode, targetNode) : 0);
            all.put(startNode, start);
            state.add(createState(start, all, targetNode));
        }

        List<T> foundRoute = null;
        long foundScore = Long.MIN_VALUE;

        while (!state.isEmpty()) {
            State<T> lastState = state.getLast();
            RouteNodeEstimated<T> current = lastState.node;
            T currentNode = current.getCurrent();
            long currentRouteScore = current.getRouteScore();
            if (currentNode.equals(targetNode)) {
                // found the route
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
                lastState.nexts.clear();
            }
            if (lastState.nexts.isEmpty()) {
                current.setPrevious(null); // node exhausted, mark as non-visited and backtrack
                state.removeLast();
                continue;
            }
            RouteNodeEstimated<T> nextNode = lastState.nexts.removeFirst();
            long newScore = currentRouteScore + (nextNodeScorer != null ? nextNodeScorer.computeCost(currentNode, nextNode.getCurrent()) : 0);
            nextNode.setPrevious(current);
            nextNode.setRouteScore(newScore);
            nextNode.setEstimatedScore(newScore + (targetScorer != null ? targetScorer.computeCost(currentNode, targetNode) : 0));
            State<T> nextState = createState(nextNode, all, targetNode);
            state.add(nextState);
        }
        return foundRoute != null ? new Pair<>(foundRoute, foundScore) : null;
    }
}
