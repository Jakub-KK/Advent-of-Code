package dev.aoc.aoc2023;

import dev.aoc.common.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day17 extends Day {
    public Day17(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day17(""));
        // _main_subset_20x20
        // _sample, _sample_subset_3x2
        // _sample_subset_9x2, _sample_subset_9x3
        // _sample_subset_10x10, _sample_subset_12x5, _sample_subset_13x6, _sample_subset_13x8, _sample_subset_12x11
        // _test_meandering, _test_meandering_loopy, _test_meandering_loopy_small0, _test_meandering_loopy_small1
        // _test_2way_12x12, _test_rainbow, _test_rainbow_3x3
        // _test_blackhole_loopy, _test_blackhole_loopy_hard
    }

    private final int MAX_STEPS = 3;

    private static class CityGrid extends Grid2<Integer> {
        public CityGrid(List<String> lines, String elementDelimiter, Function<String, Integer> parser, Supplier<Class<?>> classSupplier) {
            super(lines, elementDelimiter, parser, classSupplier);
        }

        public CityGrid(int width, int height, Integer fillElement, String elementDelimiter) {
            super(width, height, fillElement, elementDelimiter);
        }

        @Override
        protected String toStringCell(Integer cell) {
            return cell == BLACKHOLE ? "." : super.toStringCell(cell);
        }

        /** Special large cell value for easier testing. Use '.' in input files. */
        public static int BLACKHOLE = 80087;

        public static Integer parserBlackhole(String s) {
            return s.equals(".") ? CityGrid.BLACKHOLE : Integer.parseInt(s);
        }
    }

    private CityGrid cityGrid;

    // private record E(int heatLossSum, int numOfStepsSameDir, boolean dirHorizontal) {
    //     @Override
    //     public String toString() {
    //         return "<%d-%d%s>".formatted(heatLossSum, numOfStepsSameDir, dirHorizontal ? "|" : "-");
    //     }
    // }

    public interface GraphNode {
        int getId();
    }

    public static class Graph<T extends GraphNode> {
        // private final Set<T> nodes;
        private final Map<Integer, T> nodes;
        private final Map<Integer, Set<Integer>> connections;

        public Graph() {
            // nodes = new HashSet<>();
            nodes = new HashMap<>();
            connections = new HashMap<>();
        }

        public T getNodeOrCreate(int id, Supplier<T> nodeSupplier) {
            return nodes.computeIfAbsent(id, key -> nodeSupplier.get());
        }

        public boolean hasNode(int id) {
            return nodes.containsKey(id);
        }

        public void addNode(T node) {
            nodes.put(node.getId(), node);
        }

        public T getNode(int id) {
            return nodes.get(id);
            // return nodes.stream()
            //         .filter(node -> node.getId() == id)
            //         .findFirst()
            //         .orElseThrow(() -> new IllegalArgumentException("No node found with ID"));
        }

        public void addConnection(T fromNode, T toNode) {
            Set<Integer> fromConnections = connections.computeIfAbsent(fromNode.getId(), id -> new HashSet<>());
            fromConnections.add(toNode.getId());
        }

        public Set<T> getConnections(T node) {
            return connections.get(node.getId()).stream()
                    .map(this::getNode)
                    .collect(Collectors.toSet());
        }
    }

    public interface Scorer<T extends GraphNode> {
        int computeCost(T from, T to);
    }

    public static class RouteNode<T extends GraphNode> implements Comparable<RouteNode<T>> {
        private final T current;
        private RouteNode<T> previous;
        private int routeScore;
        private int estimatedScore;

        RouteNode(T current) {
            this(current, null, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        RouteNode(T current, RouteNode<T> previous, int routeScore, int estimatedScore) {
            this.current = current;
            this.previous = previous;
            this.routeScore = routeScore;
            this.estimatedScore = estimatedScore;
        }

        public T getCurrent() {
            return current;
        }

        public RouteNode<T> getPrevious() {
            return previous;
        }

        public void setPrevious(RouteNode<T> previous) {
            this.previous = previous;
        }

        public int getRouteScore() {
            return routeScore;
        }

        public void setRouteScore(int routeScore) {
            this.routeScore = routeScore;
        }

        public void setEstimatedScore(int estimatedScore) {
            this.estimatedScore = estimatedScore;
        }

        @Override
        public int compareTo(RouteNode other) {
            return Integer.compare(this.estimatedScore, other.estimatedScore);
        }
    }

    public interface RouteConstraintGuard<T extends GraphNode> {
        boolean isAllowed(RouteNode<T> from, RouteNode<T> to);
    }

    public class RouteFinder<T extends GraphNode> {
        private final Graph<T> graph;
        private final RouteConstraintGuard<T> nextNodeRouteConstraintGuard;
        private final Scorer<T> nextNodeScorer;
        private final Scorer<T> targetScorer;

        public RouteFinder(Graph<T> graph, RouteConstraintGuard<T> nextNodeRouteConstraintGuard, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
            this.graph = graph;
            this.nextNodeRouteConstraintGuard = nextNodeRouteConstraintGuard;
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
                System.out.printf("routing: open %d%n", openSet.size());
                RouteNode<T> current = openSet.poll();
                T currentNode = current.getCurrent();
                if (currentNode.equals(to)) {
                    System.out.printf("score %d%n", current.getRouteScore());
                    List<T> route = new ArrayList<>();
                    RouteNode<T> routeNode = current;
                    do {
                        route.addFirst(routeNode.getCurrent());
                        routeNode = routeNode.getPrevious();
                    } while (routeNode != null);
                    return route;
                }
                Grid2<Character> routeMap = new Grid2<>(cityGrid.getWidth(), cityGrid.getHeight(), '.', "");
                allNodes.values().forEach(n -> routeMap.set(((GridGraphNode)n.current).col, ((GridGraphNode)n.current).row, n.routeScore < scoreDebug.length ? scoreDebug[n.routeScore] : '?'));
                openSet.forEach(n -> routeMap.set(((GridGraphNode)n.current).col, ((GridGraphNode)n.current).row, '*'));
                System.out.println(routeMap);

                graph.getConnections(currentNode).forEach(connection -> {
                    RouteNode<T> nextNode = allNodes.getOrDefault(connection, new RouteNode<>(connection));
                    allNodes.put(connection, nextNode);
                    boolean isRouteAllowed = nextNodeRouteConstraintGuard == null || nextNodeRouteConstraintGuard.isAllowed(current, nextNode);
                    if (isRouteAllowed) { // instead of boolean test we could use scoring and set score to infinity to disallow routes, but it's more hassle with int/double scoring
                        int newScore = current.getRouteScore() + nextNodeScorer.computeCost(currentNode, connection);
                        if (newScore < nextNode.getRouteScore()) {
                            nextNode.setPrevious(current);
                            nextNode.setRouteScore(newScore);
                            nextNode.setEstimatedScore(newScore + targetScorer.computeCost(connection, to));
                            if (!openSet.contains(nextNode)) {
                                openSet.add(nextNode);
                            }
                        }
                    }
                });
            }

            throw new IllegalStateException("No route found");
        }
    }

    public class GridGraphNode implements GraphNode {
        public final int col;
        public final int row;
        public final int id;

        public GridGraphNode(int col, int row) {
            this.col = col;
            this.row = row;
            id = cityGrid.getUniqueId(col, row);
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GridGraphNode node = (GridGraphNode) o;
            return id == node.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "[%d,%d|%d]".formatted(col, row, id);
        }
    }

    /** Manhattan distance route score estimator.
     * As a heuristic it's admissible - never overestimates the actual cost to get to the goal.
     * Thus, A* is guaranteed to return a least-cost path from start to goal.
     */
    public class TargetEstimateScorer implements Scorer<GridGraphNode> {
        @Override
        public int computeCost(GridGraphNode from, GridGraphNode to) {
            return Math.abs(from.col - to.col) + Math.abs(from.row - to.row);
        }
    }

    public class WeightedGraph {
        private final Graph<GridGraphNode> graph;
        private final Map<Pair<Integer, Integer>, Integer> heatLossAdjacentNodes;
        private GridGraphNode start, end;

        public WeightedGraph() {
            graph = new Graph<>();
            heatLossAdjacentNodes = new HashMap<>(cityGrid.getWidth() * cityGrid.getHeight());
        }

        public void populate() {
            for (int row = 0; row < cityGrid.getHeight(); row++) {
                for (int col = 0; col < cityGrid.getWidth(); col++) {
                    int id = cityGrid.getUniqueId(col, row);
                    int finalCol = col, finalRow = row;
                    GridGraphNode node = graph.getNodeOrCreate(id, () -> new GridGraphNode(finalCol, finalRow));
                    if (col > 0) {
                        addConnection(node, col - 1, row);
                    }
                    if (col < cityGrid.getWidth() - 1) {
                        addConnection(node, col + 1, row);
                    }
                    if (row > 0) {
                        addConnection(node, col, row - 1);
                    }
                    if (row < cityGrid.getHeight() - 1) {
                        addConnection(node, col, row + 1);
                    }
                }
            }
            start = graph.getNode(cityGrid.getUniqueId(0, 0));
            int col = cityGrid.getWidth() - 1;
            int row = cityGrid.getHeight() - 1;
            end = graph.getNode(cityGrid.getUniqueId(col, row));
        }
        private void addConnection(GridGraphNode fromNode, int toCol, int toRow) {
            int toId = cityGrid.getUniqueId(toCol, toRow);
            GridGraphNode toNode = graph.getNodeOrCreate(toId, () -> new GridGraphNode(toCol, toRow));
            graph.addConnection(fromNode, toNode);
            heatLossAdjacentNodes.put(new Pair<>(fromNode.getId(), toId), cityGrid.get(toCol, toRow));
        }

        public class HeatLossScorer implements Scorer<GridGraphNode> {
            @Override
            public int computeCost(GridGraphNode from, GridGraphNode to) {
                return heatLossAdjacentNodes.get(new Pair<>(from.getId(), to.getId()));
            }
        }
        public HeatLossScorer getHeatLossScorer() { return new HeatLossScorer(); }

        /** Adds route constraint score to disallow forbidden routes.
         * AoC problem forbids routes having more than tree steps in the same direction.
         */
        public class RouteConstraint3StepsMaxGuard implements RouteConstraintGuard<GridGraphNode> {
            @Override
            public boolean isAllowed(RouteNode<GridGraphNode> from, RouteNode<GridGraphNode> to) {
                int stepsSameDir = 0;
                if (from.current.row == to.current.row) {
                    // count steps in the same direction to the left of from.current node
                    var currentRouteNode = from.getPrevious();
                    while (currentRouteNode != null &&
                            currentRouteNode.getCurrent().row == from.current.row) {
                        stepsSameDir++;
                        currentRouteNode = currentRouteNode.getPrevious();
                    }
                } else if (from.current.col == to.current.col) {
                    // count steps in the same direction to the left of from.current node
                    var currentRouteNode = from.getPrevious();
                    while (currentRouteNode != null &&
                            currentRouteNode.getCurrent().col == from.current.col) {
                        stepsSameDir++;
                        currentRouteNode = currentRouteNode.getPrevious();
                    }
                }
                return stepsSameDir < 3;
            }
        }
        public RouteConstraintGuard<GridGraphNode> getRouteConstraintGuard() {
            return null;//new RouteConstraint3StepsMaxGuard();
        }
    }

    public class WeightedGraphMax3StepsInSameDir {
        private final Graph<GridGraphNode> graph;
        private final Map<Pair<Integer, Integer>, Integer> heatLossAdjacentNodes;
        private GridGraphNode start, end;

        public WeightedGraphMax3StepsInSameDir() {
            graph = new Graph<>();
            heatLossAdjacentNodes = new HashMap<>(cityGrid.getWidth() * cityGrid.getHeight());
        }

        public void populate() {
            for (int row = 0; row < cityGrid.getHeight(); row++) {
                for (int col = 0; col < cityGrid.getWidth(); col++) {
                    int id = cityGrid.getUniqueId(col, row);
                    int finalCol = col, finalRow = row;
                    GridGraphNode node = graph.getNodeOrCreate(id, () -> new GridGraphNode(finalCol, finalRow));
                    addConnections(node, MAX_STEPS, col, row, (coords, steps) -> coords.getValue0() - (1 + MAX_STEPS - steps) >= 0, (coords, steps) -> new Pair<>(coords.getValue0() - (1 + MAX_STEPS - steps), coords.getValue1()));
                    addConnections(node, MAX_STEPS, col, row, (coords, steps) -> coords.getValue0() + (1 + MAX_STEPS - steps) < cityGrid.getWidth(), (coords, steps) -> new Pair<>(coords.getValue0() + (1 + MAX_STEPS - steps), coords.getValue1()));
                    addConnections(node, MAX_STEPS, col, row, (coords, steps) -> coords.getValue1() - (1 + MAX_STEPS - steps) >= 0, (coords, steps) -> new Pair<>(coords.getValue0(), coords.getValue1() - (1 + MAX_STEPS - steps)));
                    addConnections(node, MAX_STEPS, col, row, (coords, steps) -> coords.getValue1() + (1 + MAX_STEPS - steps) < cityGrid.getHeight(), (coords, steps) -> new Pair<>(coords.getValue0(), coords.getValue1() + (1 + MAX_STEPS - steps)));
                }
            }
            start = graph.getNode(cityGrid.getUniqueId(0, 0));
            int col = cityGrid.getWidth() - 1;
            int row = cityGrid.getHeight() - 1;
            end = graph.getNode(cityGrid.getUniqueId(col, row));
        }
        private void addConnections(GridGraphNode node, int MAX_STEPS, int col, int row, BiPredicate<Pair<Integer, Integer>, Integer> inGrid, BiFunction<Pair<Integer, Integer>, Integer, Pair<Integer, Integer>> moveCoords) {
            int steps = MAX_STEPS;
            int heatLoss = 0;
            while (steps > 0 && inGrid.test(new Pair<>(col, row), steps)) {
                Pair<Integer, Integer> newCoords = moveCoords.apply(new Pair<>(col, row), steps);
                heatLoss += cityGrid.get(newCoords.getValue0(), newCoords.getValue1());
                addConnection(node, newCoords.getValue0(), newCoords.getValue1(), heatLoss);
                steps--;
            }
        }
        private void addConnection(GridGraphNode fromNode, int toCol, int toRow, int heatLoss) {
            int toId = cityGrid.getUniqueId(toCol, toRow);
            GridGraphNode toNode = graph.getNodeOrCreate(toId, () -> new GridGraphNode(toCol, toRow));
            graph.addConnection(fromNode, toNode);
            heatLossAdjacentNodes.put(new Pair<>(fromNode.getId(), toId), heatLoss);
        }

        public class HeatLossScorer implements Scorer<GridGraphNode> {
            @Override
            public int computeCost(GridGraphNode from, GridGraphNode to) {
                return heatLossAdjacentNodes.get(new Pair<>(from.getId(), to.getId()));
            }
        }
        public HeatLossScorer getHeatLossScorer() { return new HeatLossScorer(); }

        /** Adds route constraint score to disallow forbidden routes.
         * AoC problem forbids routes having more than tree steps in the same direction.
         */
        public class RouteConstraint3StepsMaxGuard implements RouteConstraintGuard<GridGraphNode> {
            @Override
            public boolean isAllowed(RouteNode<GridGraphNode> from, RouteNode<GridGraphNode> to) {
                if (from.getPrevious() == null) {
                    return true;
                }
                boolean nextStepHorizontal = from.current.row == to.current.row;
                boolean prevStepHorizontal = from.getPrevious().current.row == from.current.row;
                boolean isDirFlipped = nextStepHorizontal ^ prevStepHorizontal;
                return isDirFlipped;
            }
        }
        public RouteConstraintGuard<GridGraphNode> getRouteConstraintGuard() {
            return new RouteConstraint3StepsMaxGuard();
        }
    }

    public static char[] scoreDebug = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
        System.out.println(cityGrid);
        // CityGrid cityGrid3 = new CityGrid(cityGrid.getWidth() / 3, cityGrid.getHeight() / 3, 0, "");
        // for (int row = 0; row < cityGrid3.getHeight(); row++) {
        //     for (int col = 0; col < cityGrid3.getWidth(); col++) {
        //         int sum3x3 = 0;
        //         for (int row3 = 0; row3 < 3; row3++) {
        //             for (int col3 = 0; col3 < 3; col3++) {
        //                 sum3x3 += cityGrid.get(col * 3 + col3, row * 3 + row3);
        //             }
        //         }
        //         cityGrid3.set(col, row, sum3x3 / 9);
        //     }
        // }
        // System.out.println(cityGrid3);
        // cityGrid = cityGrid3;
        System.out.printf("grid hash %d%n", cityGrid.hashCode());
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // ****** attempt 1: abandoned
        // dynamic programming: go through all diagonals building solution
        // problem: updating solution backwards - needed for path winding back and forth though grid
        // Grid2<E> solverState = new Grid2<>(cityGrid.getWidth(), cityGrid.getHeight(), new E(-1, -1, false), "");
        // int diagonalMaxLen = Math.min(solverState.getWidth(), solverState.getHeight());
        // int diagonalsCount = solverState.getWidth() + solverState.getHeight() - 1;
        // for (int diag = 0; diag < diagonalsCount; diag++) {
        //     if (diag < diagonalMaxLen) {
        //
        //     }
        // }
        // ****** attempt 2: failed
        // use A* search with modified grid graph (every vertex has (at most) 12 edges for every possible valid move)
        // adds path constraint that every move must change direction
        // fails because if sub-path from A to B has multiple answers, it considers only one of them
        // but path constraint means that we must consider every sub-path possibility for further path to have chance to assume constraint
        // var weightedGraph = new WeightedGraph();
        // weightedGraph.populate();
        // RouteFinder<GridGraphNode> routeFinder = new RouteFinder<>(weightedGraph.graph, weightedGraph.getRouteConstraintGuard(), weightedGraph.getHeatLossScorer(), new TargetEstimateScorer());
        // List<GridGraphNode> route = routeFinder.findRoute(weightedGraph.start, weightedGraph.end);
        // Grid2<Character> routeMap = new Grid2<>(cityGrid.getWidth(), cityGrid.getHeight(), '.', "");
        // // route.forEach(n -> routeMap.set(n.col, n.row, 'X'));
        // routeMap.set(route.getFirst().col, route.getFirst().row, '*');
        // char[] routeSymbols = "<>^v".toCharArray();
        // for (int ri = 1; ri < route.size(); ri++) {
        //     GridGraphNode node = route.get(ri), prevNode = route.get(ri - 1);
        //     routeMap.set(node.col, node.row, routeSymbols[node.row - prevNode.row == 0 ? ((1 + Math.clamp(node.col - prevNode.col, -1, 1)) / 2) : (2 + (1 + Math.clamp(node.row - prevNode.row, -1, 1)) / 2)]);
        // }
        // System.out.println(routeMap);
        // ****** attempt 2: brute-force
        graph = new WeightedGraph();
        graph.populate();
        bestPathsForConstraintAndNode = new HashMap<>();
        List<GridGraphNode> winnerPath = new LinkedList<>();
        AtomicInteger counter = new AtomicInteger(0);
        BiConsumer<List<GridGraphNode>, Integer> onPathFound = (path, score) -> {
            if (path.getFirst().getId() != 0) {
                return;
            }
            if (path != winnerPath) {
                winnerPath.clear();
                winnerPath.addAll(path);
                counter.incrementAndGet();
            }
            if (true || path == winnerPath) {
                System.out.printf("#%d: %d %s%n", counter.get(), score, String.join(",", path.stream().map(GridGraphNode::toString).toList()));
                System.out.printf("path hash %d%n", Arrays.hashCode(path.toArray(new GridGraphNode[0])));
                debugShowPath("path before reconstruction:", winnerPath);
                if (winnerPath.getLast() != graph.end) {
                    // reconstruct path finish from memo, check score, check memo best scores
                    int scoreCheck = 0;
                    int currentPathIdx = 0;
                    GridGraphNode current = winnerPath.get(currentPathIdx);
                    Direction lastDir = Direction.UNKNOWN;
                    int lastStepsCols = 0, lastStepsRows = 0;
                    do {
                        GridGraphNode next;
                        if (currentPathIdx < winnerPath.size() - 1) {
                            currentPathIdx++;
                            next = winnerPath.get(currentPathIdx);
                        } else {
                            if (lastDir == Direction.UNKNOWN) {
                                throw new IllegalStateException();
                            }
                            int memoKey = bestPathsForConstraintAndNodeMemoKey(lastDir, Math.max(lastStepsCols, lastStepsRows), current.getId());
                            Pair<Integer, GridGraphNode> memoBest = bestPathsForConstraintAndNode.get(memoKey);
                            if (memoBest == null) {
                                throw new IllegalStateException();
                            }
                            if (scoreCheck + memoBest.getValue0() != score) {
                                throw new IllegalStateException();
                            }
                            next = memoBest.getValue1();
                            currentPathIdx++;
                            winnerPath.add(next);
                        }
                        scoreCheck += cityGrid.get(next.col, next.row);
                        boolean isStepRows = current.row == next.row;
                        if (isStepRows) {
                            lastStepsCols = lastStepsCols + 1;
                            lastStepsRows = 0;
                        } else {
                            lastStepsCols = 0;
                            lastStepsRows = lastStepsRows + 1;
                        }
                        // 0 down, 2 up, 3 right, 1 left
                        if (current.row == next.row) {
                            lastDir = current.col < next.col ? Direction.RIGHT : Direction.LEFT;
                        } else {
                            lastDir = current.row < next.row ? Direction.DOWN : Direction.UP;
                        }
                        current = next;
                    } while (current != graph.end);
                    debugShowPath("path reconstructed:", winnerPath);
                    if (score != scoreCheck) {
                        throw new IllegalStateException();
                    }
                }
            }
        };
        int startingBestScore = this.getInputSuffix().isEmpty() ? 1500 : Integer.MAX_VALUE; // 1072 discovered during testing, probably way too high
        Pathfinder pathfinder = new Pathfinder(graph.start, graph.end, onPathFound, startingBestScore);

        // if (false)
        memoAll();

        pathfinder.findPath(0, 0, 0, Direction.UNKNOWN);
        pathfinder.memoStatsReport();
        System.out.print("winner ");
        onPathFound.accept(winnerPath, pathfinder.getBestScore());
        long result = pathfinder.getBestScore();
        return result;
    }

    private enum Direction { // 0 down, 2 up, 3 right, 1 left
        UNKNOWN(-1), DOWN(0), LEFT(1), UP(2), RIGHT(3);
        public final int dir;
        Direction(int dir) {
            this.dir = dir;
        }
        static Direction[] values = values();
        static Direction fromValue(int dirValue) {
            for (Direction dir : values) {
                if (dir.dir == dirValue) {
                    return dir;
                }
            }
            return Direction.UNKNOWN;
        }
    }

    private WeightedGraph graph;
    private Map<Integer, Pair<Integer, GridGraphNode>> bestPathsForConstraintAndNode;
    private int bestPathsForConstraintAndNodeMemoKey(Direction lastDir, int lastStepsInTheSameDir, int nodeId) {
        return lastDir.dir + 4 * (lastStepsInTheSameDir - 1 + 3 * nodeId);
    }

    private void memoAll() {
        if (cityGrid.getWidth() != cityGrid.getHeight()) {
            throw new IllegalArgumentException("non square");
        }
        int memoAllBestScore = Integer.MAX_VALUE;
        if (this.getInputSuffix().equals("_test_meandering_loopy")) memoAllBestScore = 190;
        else if (this.getInputSuffix().equals("_test_meandering_loopy_small0")) memoAllBestScore = 63;
        // else if (this.getInputSuffix().equals("_test_meandering_loopy_small1")) memoAllBestScore = 116;
        // if (false)
        for (int col = cityGrid.getWidth() - 1 - 1; col >= 0; col--) {
            // if (cityGrid.getWidth() - (col + 1) >= 60) break;
            int diagMax = cityGrid.getWidth() - 1 - col;
            for (int diag = 0; diag <= diagMax; diag++) {
                System.out.printf("memo all: col diagonal %d/%d, pos %d/%d >", cityGrid.getWidth() - (col + 1), cityGrid.getWidth(), diag + 1, diagMax + 1);
                int startCol = col + diag, startRow = cityGrid.getWidth() - 1 - diag;
                memoAll(startCol, startRow, memoAllBestScore);
                // System.out.println();
            }
            // memoDebug();
        }
        // if (false)
        for (int row = cityGrid.getWidth() - 1 - 1; row > 0; row--) {
            int diagMax = row;
            for (int diag = 0; diag <= diagMax; diag++) {
                System.out.printf("memo all: row diagonal %d/%d, pos %d/%d >", cityGrid.getWidth() - (row + 1), cityGrid.getWidth(), diag + 1, diagMax + 1);
                int startCol = 0 + diag, startRow = row - diag;
                // System.out.printf("%d,%d%n", startCol, startRow);
                memoAll(startCol, startRow, memoAllBestScore);
                // System.out.println();
            }
            // memoDebug();
        }
    }
    private void memoAll(int startCol, int startRow, int memoBestScore) {
        Pathfinder pathfinder = new Pathfinder(graph.graph.getNode(cityGrid.getUniqueId(startCol, startRow)), graph.end, null, memoBestScore);
        if (startCol != 0) {
            for (int steps = 1; steps <= MAX_STEPS && startCol - steps >= 0; steps++) {
                pathfinder.pathMark(startCol - steps, startRow, true);
                pathfinder.setBestScore(memoBestScore);
                pathfinder.findPath(0, steps, 0, Direction.RIGHT);
            }
            for (int steps = 1; steps <= MAX_STEPS && startCol - steps >= 0; steps++) {
                pathfinder.pathMark(startCol - steps, startRow, false);
            }
        }
        if (startRow != 0) {
            for (int steps = 1; steps <= MAX_STEPS && startRow - steps >= 0; steps++) {
                pathfinder.pathMark(startCol, startRow - steps, true);
                pathfinder.setBestScore(memoBestScore);
                pathfinder.findPath(0, 0, steps, Direction.DOWN);
            }
            for (int steps = 1; steps <= MAX_STEPS && startRow - steps >= 0; steps++) {
                pathfinder.pathMark(startCol, startRow - steps, false);
            }
        }
        if (false)
        if (startCol != cityGrid.getWidth() - 1) {
            for (int steps = 1; steps <= MAX_STEPS && startCol + steps < cityGrid.getWidth(); steps++) {
                pathfinder.pathMark(startCol + steps, startRow, true);
                pathfinder.setBestScore(memoBestScore);
                pathfinder.findPath(0, steps, 0, Direction.LEFT);
            }
            for (int steps = 1; steps <= MAX_STEPS && startCol + steps < cityGrid.getWidth(); steps++) {
                pathfinder.pathMark(startCol + steps, startRow, false);
            }
        }
        if (false)
        if (startRow != cityGrid.getHeight() - 1) {
            for (int steps = 1; steps <= MAX_STEPS && startRow + steps < cityGrid.getHeight(); steps++) {
                pathfinder.pathMark(startCol, startRow + steps, true);
                pathfinder.setBestScore(memoBestScore);
                pathfinder.findPath(0, 0, steps, Direction.UP);
            }
            for (int steps = 1; steps <= MAX_STEPS && startRow + steps < cityGrid.getHeight(); steps++) {
                pathfinder.pathMark(startCol, startRow + steps, false);
            }
        }
        pathfinder.memoStatsReport();
    }
    private void memoDebug() {
        Grid2<List<Triplet<Direction, Integer, GridGraphNode>>> memoCheck = new Grid2<>(cityGrid.getWidth(), cityGrid.getHeight(), null, ArrayList.class, " ||| ");
        for (Map.Entry<Integer, Pair<Integer, GridGraphNode>> entry : bestPathsForConstraintAndNode.entrySet()) {
            int key = entry.getKey();
            // memoKey = lastDir + 4 * (lastStepsInTheSameDir - 1 + 3 * current.getId());
            int nodeId = key / 4 / 3;
            Direction lastDir = Direction.fromValue(key % 4);
            int lastStepsInTheSameDir = (key / 4) % 3 + 1;
            GridGraphNode node = graph.graph.getNode(nodeId);
            GridGraphNode memoNode = entry.getValue().getValue1();
            List<Triplet<Direction, Integer, GridGraphNode>> memos;
            if (memoCheck.has(node.col, node.row)) {
                memos = memoCheck.get(node.col, node.row);
                // if (!memoCheck.is(node.col, node.row, memoNode)) {
                //     System.out.println("multiple winning");
                // }
            } else {
                memos = new ArrayList<>();
                memoCheck.set(node.col, node.row, memos);
            }
            memos.add(new Triplet<>(lastDir, lastStepsInTheSameDir, memoNode));
            memos.sort(Comparator.comparingInt(n -> (n.getValue0().dir * 3 + (n.getValue1() - 1)) * cityGrid.getUniqueIdMax() + n.getValue2().getId()));
        }
        return;
    }

    private class Pathfinder {
        private final GridGraphNode target;
        private final BiConsumer<List<GridGraphNode>, Integer> onPathFound;
        private int bestScore;

        private final Comparator<GridGraphNode> nodeComparator;

        public Pathfinder(GridGraphNode source, GridGraphNode target, BiConsumer<List<GridGraphNode>, Integer> onPathFound, int bestScore) {
            this.target = target;
            this.onPathFound = onPathFound;
            this.bestScore = bestScore;
            this.nodeComparator = Comparator.comparingLong(n -> {
                int heatLoss = cityGrid.get(n.col, n.row); // graph.heatLossAdjacentNodes.get(new Pair<>(current.getId(), n.getId()));
                int distanceToTarget = Math.abs(n.col - target.col) + Math.abs(n.row - target.row);
                // return distanceToTarget;
                // return (((cityGrid.getWidth() - n.col) + (cityGrid.getHeight() - n.row)) * 2L + ((n.col * n.row) & 1)) * 10L + heatLoss;
                // return (distanceToTarget * 4L + (((long) n.col * n.row) & 3)) * 10 + heatLoss;
                return 10L * distanceToTarget + heatLoss; // max heat loss is 9
                // return distanceToTarget + (long)heatLoss * (cityGrid.getWidth() + cityGrid.getHeight()); // only good for 1-path through 9-ground
            });
            path = new ArrayList<>();
            path.add(source);
            pathMarkers = new Grid2<>(cityGrid.getWidth(), cityGrid.getHeight(), false, "");
            pathMarkers.set(source.col, source.row, true);
        }

        private final List<GridGraphNode> path;
        private final Grid2<Boolean> pathMarkers;

        public int getBestScore() {
            return bestScore;
        }

        public void setBestScore(int bestScore) {
            this.bestScore = bestScore;
        }

        private long memoQueries, memoHits, memoFoundNodes, memoSuccesses;
        public void memoStatsReset() { memoQueries = memoHits = memoFoundNodes = memoSuccesses = 0; }
        public void memoStatsReport() { System.out.printf("memo stats: queries %d, hits %d (%.2f%%), nodes %d (%.2f%%), success %d (%.2f%%)%n", memoQueries, memoHits, 100.0*memoHits/memoQueries, memoFoundNodes, 100.0*memoFoundNodes/memoQueries, memoSuccesses, 100.0*memoSuccesses/memoQueries); }

        private void findPath(int score, int stepsRows, int stepsCols, Direction lastDir) {
            // if (path.size() % 100 == 0) {
            //     System.out.printf("path len %d, score %d, best %d%n".formatted(path.size(), score, bestScore));
            //     debugShowPath(path);
            // }
            GridGraphNode current = path.getLast();
            Integer memoKey = null;
            if (lastDir != Direction.UNKNOWN) {
                int lastStepsInTheSameDir = Math.max(stepsRows, stepsCols);
                memoKey = bestPathsForConstraintAndNodeMemoKey(lastDir, lastStepsInTheSameDir, current.getId());
                Pair<Integer, GridGraphNode> memoBest = bestPathsForConstraintAndNode.get(memoKey);
                memoQueries++;
                if (memoBest != null) {
                    // System.out.println("memo hit");
                    memoHits++;
                    GridGraphNode bestNode = memoBest.getValue1();
                    if (bestNode != null) {
                        memoFoundNodes++;
                        int bestPathScore = score + memoBest.getValue0();
                        // System.out.printf("memo: score %d + memo score %d = %d%n", score, memoBest.getValue0(), bestPathScore);
                        // debugShowPath(path);
                        if (bestScore > bestPathScore) {
                            memoSuccesses++;
                            if (path.getFirst().getId() == 0) {
                                System.out.printf("memo: path %d, score %d + memo score %d = %d < %d%n", path.size(), score, memoBest.getValue0(), bestPathScore, bestScore);
                            }
                            bestScore = bestPathScore;
                            if (onPathFound != null) {
                                onPathFound.accept(path, bestPathScore);
                            }
                        }
                        return;
                    }
                }
            }
            // System.out.printf("%d: %s->...%n".formatted(path.size(), current));
            final int currCol = current.col;
            final int currRow = current.row;
            final int firstCol = path.getFirst().col; // for staying below diagonal
            final int firstRow = path.getFirst().row;
            final int diagonalSum = firstCol + firstRow - 1;
            List<GridGraphNode> connections = new ArrayList<>(4);
            if (currCol > 0 && !pathMarkers.get(currCol - 1, currRow) && stepsRows < MAX_STEPS && diagonalSum <= currCol - 1 + currRow) {
                connections.add(graph.graph.getNode(cityGrid.getUniqueId(currCol - 1, currRow)));
            }
            if (currCol < cityGrid.getWidth() - 1 && !pathMarkers.get(currCol + 1, currRow) && stepsRows < MAX_STEPS && diagonalSum <= currCol + 1 + currRow) {
                connections.add(graph.graph.getNode(cityGrid.getUniqueId(currCol + 1, currRow)));
            }
            if (currRow > 0 && !pathMarkers.get(currCol, currRow - 1) && stepsCols < MAX_STEPS && diagonalSum <= currCol + currRow - 1) {
                connections.add(graph.graph.getNode(cityGrid.getUniqueId(currCol, currRow - 1)));
            }
            if (currRow < cityGrid.getHeight() - 1 && !pathMarkers.get(currCol, currRow + 1) && stepsCols < MAX_STEPS && diagonalSum <= currCol + currRow + 1) {
                connections.add(graph.graph.getNode(cityGrid.getUniqueId(currCol, currRow + 1)));
            }
            connections.sort(nodeComparator);
            GridGraphNode bestNode = null;
            for (GridGraphNode connection : connections) {
                // System.out.printf("%d: ->%s".formatted(path.size(), connection));
                int nextScore = score + cityGrid.get(connection.col, connection.row); // graph.heatLossAdjacentNodes.get(new Pair<>(current.getId(), connection.getId()));
                // System.out.printf(" score %d%n", nextScore);
                // System.out.printf("%d: ->%s score %d", path.size(), connection, nextScore);
                if (nextScore <= bestScore) { // less or equal to show equal score paths
                    // System.out.println();
                    path.addLast(connection);
                    pathMarkers.set(connection.col, connection.row, true);
                    if (connection.equals(target)) {
                        bestScore = nextScore;
                        if (onPathFound != null) {
                            onPathFound.accept(path, nextScore);
                        }
                        bestNode = connection;
                    } else {
                        // debugShowPath(path);
                        int nextStepsRows, nextStepsCols;
                        boolean isStepRows = currRow == connection.row;
                        if (isStepRows) {
                            nextStepsRows = stepsRows + 1;
                            nextStepsCols = 0;
                        } else {
                            nextStepsRows = 0;
                            nextStepsCols = stepsCols + 1;
                        }
                        Direction nextDir; // 0 down, 2 up, 3 right, 1 left
                        if (currRow == connection.row) {
                            nextDir = currCol < connection.col ? Direction.RIGHT : Direction.LEFT;
                        } else {
                            nextDir = currRow < connection.row ? Direction.DOWN : Direction.UP;
                        }
                        int savedBestScore = bestScore;
                        findPath(nextScore, nextStepsRows, nextStepsCols, nextDir);
                        if (bestScore < savedBestScore) {
                            bestNode = connection;
                        }
                    }
                    path.removeLast();
                    pathMarkers.set(connection.col, connection.row, false);
                } else {
                    // System.out.println(" pruned");
                }
            }
            // memoization
            // if (false)
            if (memoKey != null && bestNode != null) {
                Pair<Integer, GridGraphNode> memoBest = bestPathsForConstraintAndNode.get(memoKey);
                if (memoBest != null && memoBest.getValue1() != bestNode) {
                    throw new IllegalStateException("huh");
                }
                if (memoBest == null || memoBest.getValue0() > bestScore - score) {
                    bestPathsForConstraintAndNode.put(memoKey, new Pair<>(bestScore - score, bestNode));
                }
            }
        }

        public void pathMark(int col, int row, boolean state) {
            pathMarkers.set(col, row, state);
        }
    }

    private void debugShowPath(String title, List<GridGraphNode> path) {
        Grid2<Character> pathMap = new Grid2<>(cityGrid.getWidth(), cityGrid.getHeight(), '.', "");
        pathMap.set(path.getFirst().col, path.getFirst().row, '*');
        char[] routeSymbols = "<>^v".toCharArray();
        for (int ri = 1; ri < path.size(); ri++) {
            GridGraphNode node = path.get(ri), prevNode = path.get(ri - 1);
            pathMap.set(node.col, node.row, routeSymbols[node.row - prevNode.row == 0 ? ((1 + Math.clamp(node.col - prevNode.col, -1, 1)) / 2) : (2 + (1 + Math.clamp(node.row - prevNode.row, -1, 1)) / 2)]);
        }
        if (title != null && !title.trim().isEmpty()) {
            System.out.println(title);
        }
        System.out.println(pathMap);
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        long result = 0;
        return null;
    }

    private void parse() {
        var mapStrings = stream().collect(Collectors.toList());
        Function<String, Integer> parser = Integer::parseInt;
        if (this.getInputSuffix().contains("_blackhole_")) parser = CityGrid::parserBlackhole;
        cityGrid = new CityGrid(mapStrings, "", parser, Integer.valueOf(0)::getClass);
        System.out.printf("city grid %d x %d%n", cityGrid.getWidth(), cityGrid.getHeight());
    }

    public static class Day17Test {
        @Test
        void knownGoodInputs() {

        }

        @Test
        void solvePart1_small() {
            var day = new Day17("_sample");
            day.parsePart1();
            assertEquals(0L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day17("");
            day.parsePart1();
            assertEquals(0L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day17("_sample");
            day.parsePart2();
            assertEquals(0L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day17("");
            day.parsePart1();
            day.parsePart2();
            assertEquals(0L, day.solvePart2());
        }
    }
}
/*

COPY DESCRIPTION HERE

 */