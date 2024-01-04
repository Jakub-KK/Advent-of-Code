package dev.aoc.aoc2023;

import dev.aoc.common.*;
import dev.aoc.common.graphsearch.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Benchmark(run = false, cycles = 10, inputSuffix = "_test_blackhole_loopy_202x202", partNumber = 1, solutionName = "A*")
public class Day17 extends Day {
    public Day17(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day17("_sample"));
        // _main_subset_20x20
        // _sample, _sample_subset_3x2
        // _sample_subset_9x2, _sample_subset_9x3
        // _sample_subset_10x10, _sample_subset_12x5, _sample_subset_13x6, _sample_subset_13x8, _sample_subset_12x11
        // _test_meandering_9x9, _test_meandering_loopy_24x24_2, _test_meandering_loopy_24x24_2, _test_meandering_loopy_30x30
        // _test_2way_12x12, _test_directionmatters
        // _test_blackhole_loopy_7x7_harder, _test_blackhole_loopy_8x8_easier, _test_blackhole_loopy_202x202
        // _test_rainbow_27x27, _test_rainbow_3x3
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
    }

    @SolutionSolver(partNumber = 1, solutionName = "JKK bottom-up")
    public Object solvePart1_JKK() {
        // the consensus on AoC reddit is to use Dijkstra's algorithm, for short and fast programs see https://old.reddit.com/r/adventofcode/comments/18k9ne5/2023_day_17_solutions/?sort=old
        // instead, for 3 days, the different solution was created with sweat and blood...
        // similar probably to dynamic programming, using brute-force and memoization
        // builds from the target node walking through grid diagonals towards start node
        return solve(SolverType.JKK_BOTTOMUP, 1, 3);
    }

    @SolutionSolver(partNumber = 1, solutionName = "A*")
    public Object solvePart1_AStar() {
        return solve(SolverType.ASTAR, 1, 3);
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2, solutionName = "JKK bottom-up")
    public Object solvePart2_JKK() {
        return solve(SolverType.JKK_BOTTOMUP, 4, 10);
    }

    @SolutionSolver(partNumber = 2, solutionName = "A*")
    public Object solvePart2_AStar() {
        return solve(SolverType.ASTAR, 4, 10);
    }

    private void parse() {
        var mapStrings = stream().collect(Collectors.toList());
        Function<String, Integer> parser = Integer::parseInt;
        if (this.getInputSuffix().contains("_blackhole_")) parser = CityGrid::parserBlackhole;
        cityGrid = new CityGrid(mapStrings, "", parser, Integer.class);
        System.out.printf("city grid %d x %d, hash %d%n", cityGrid.getWidth(), cityGrid.getHeight(), cityGrid.hashCode());
        // System.out.println(cityGrid);
    }

    private long solve(SolverType solverType, int runMinimum, int runMaximum) {
        CityGrid.Position positionStart = cityGrid.getPosition(0, 0);
        CityGrid.Position positionTarget = cityGrid.getPosition(cityGrid.getWidth() - 1, cityGrid.getHeight() - 1);
        return solverType.getSolver().solve(cityGrid, runMinimum, runMaximum, positionStart, positionTarget);
    }

    private CityGrid cityGrid;

    public static final int MAX_RUN_MAXIMUM = 10;

    private static class CityGrid extends Grid<Integer> {
        public CityGrid(List<String> lines, String elementDelimiter, Function<String, Integer> parser, Class<?> elementClass) {
            super(lines, elementDelimiter, parser, elementClass);
        }

        public CityGrid(int width, int height, Integer fillElement, String elementDelimiter) {
            super(width, height, fillElement, elementDelimiter);
        }

        @Override
        protected String toStringCell(Integer cellValue) {
            return cellValue == BLACKHOLE ? "." : super.toStringCell(cellValue);
        }

        /** Special large cell value for easier testing. Use '.' in input files. */
        public static int BLACKHOLE = 80087;

        public static Integer parserBlackhole(String s) {
            return s.equals(".") ? CityGrid.BLACKHOLE : Integer.parseInt(s);
        }

        public Position getPosition(int col, int row) {
            return positions.computeIfAbsent(getUniqueId(col, row), id -> new Position(col, row, id));
        }

        public Position getPosition(int id) {
            return positions.get(id);
        }

        private final Map<Integer, Position> positions = new HashMap<>();

        public static class Position implements GraphNode {
            public final int col;
            public final int row;
            public final int id;

            private Position(int col, int row, int id) {
                this.col = col;
                this.row = row;
                this.id = id;
            }

            public long getId() {
                return id;
            }

            @Override
            public boolean equalsTarget(GraphNode target) {
                if (this == target) return true;
                if (target == null || getClass() != target.getClass()) return false;
                Position targetPosition = (Position)target;
                return id == targetPosition.id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CityGrid.Position node = (CityGrid.Position) o;
                return id == node.id;
            }

            @Override
            public int hashCode() {
                return Integer.hashCode(id);
            }

            @Override
            public String toString() {
                return "(%d,%d)".formatted(col, row/*, id*/);
            }
        }
    }

    private enum SolverType {
        JKK_BOTTOMUP {
            @Override
            public ISolver getSolver() {
                return new SolverJKK();
            }
        },
        ASTAR {
            @Override
            public ISolver getSolver() {
                return new SolverAStar();
            }
        };
        public abstract ISolver getSolver();
    }

    private interface ISolver {
        long solve(CityGrid cityGrid, int runMinimum, int runMaximum, CityGrid.Position start, CityGrid.Position target);
    }

    private static abstract class SolverBase implements ISolver {
        protected CityGrid cityGrid;
        protected int runMinimum;
        protected int runMaximum;
        protected CityGrid.Position start;
        protected CityGrid.Position target;

        @Override
        public long solve(CityGrid cityGrid, int runMinimum, int runMaximum, CityGrid.Position start, CityGrid.Position target) {
            this.cityGrid = cityGrid;
            this.runMinimum = runMinimum;
            this.runMaximum = runMaximum;
            if (this.runMaximum > MAX_RUN_MAXIMUM) {
                throw new IllegalArgumentException("too large value %d of runMaximum, max is %d".formatted(runMaximum, MAX_RUN_MAXIMUM));
            }
            this.start = start;
            this.target = target;
            return solve();
        }

        protected abstract long solve();
    }

    /** (/u/morgoth1145 from reddit/r/adventofcode) A* over state space of (position, previous direction) and next move changing direction (one move is many steps on the grid)
     * source: https://old.reddit.com/r/adventofcode/comments/18k9ne5/2023_day_17_solutions/kdpwy80/
     * repo: https://github.com/morgoth1145/advent-of-code/blob/2023-python/2023/17/solution.py
     * Uses previously written A* route finder
     */
    private static class SolverAStar extends SolverBase {
        /** A* will search graph with nodes of city grid position and axis of previous move (vertical/horizontal).
         * Axis as part of state is required because in this graph we change axis in every move, as moves are multiple grid cells */
        private record StateNode(CityGrid.Position position, Axis axisPrevious) implements GraphNode {
            @Override
            public long getId() {
                return position.getId() * 2L + axisPrevious.ordinal();
            }
            public enum Axis { // axis of movement
                HORIZONTAL, VERTICAL;
                public Axis turn() {
                    return switch (this) {
                        case HORIZONTAL -> VERTICAL;
                        case VERTICAL -> HORIZONTAL;
                    };
                }
            }
            /** Target equality ignores axis */
            @Override
            public boolean equalsTarget(GraphNode target) {
                if (this == target) return true;
                if (target == null || getClass() != target.getClass()) return false;
                StateNode stateNode = (StateNode) target;
                return Objects.equals(position, stateNode.position);
            }
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                StateNode stateNode = (StateNode) o;
                return Objects.equals(position, stateNode.position) && Objects.equals(axisPrevious, stateNode.axisPrevious);
            }
            @Override
            public int hashCode() {
                return Objects.hash(position, axisPrevious);
            }
            @Override
            public String toString() {
                return "[%s,%s]".formatted(position, axisPrevious.toString().charAt(0));
            }
        }
        /** A* will search graph where every vertex is position + axis of previous move, edges are moves of multiple grid cells. */
        private record StateGraph(CityGrid cityGrid, int runMinimum, int runMaximum) implements Graph<StateNode> {
            @Override
            public StateNode getNode(long id) {
                throw new IllegalStateException("not implemented");
            }
            private static final Grid.Direction[] directionsHorizontal = new Grid.Direction[] { Grid.Direction.LEFT, Grid.Direction.RIGHT };
            private static final Grid.Direction[] directionsVertical = new Grid.Direction[] { Grid.Direction.UP, Grid.Direction.DOWN };
            @Override
            public Set<StateNode> getEdges(StateNode node) {
                HashSet<StateNode> result = new HashSet<>();
                StateNode.Axis newAxis = node.axisPrevious.turn();
                Grid.Direction[] newDirections = switch (newAxis) {
                    case HORIZONTAL -> directionsHorizontal;
                    case VERTICAL -> directionsVertical;
                };
                for (Grid.Direction newDirection : newDirections) {
                    int newCol = node.position.col + newDirection.dCol * (runMinimum - 1);
                    int newRow = node.position.row + newDirection.dRow * (runMinimum - 1);
                    for (int runSteps = runMinimum; runSteps <= runMaximum; runSteps++) {
                        newCol += newDirection.dCol;
                        newRow += newDirection.dRow;
                        if (!cityGrid.hasPos(newCol, newRow)) {
                            break;
                        }
                        result.add(new StateNode(cityGrid.getPosition(newCol, newRow), newAxis));
                    }
                }
                return result;
            }
        }
        /** Cost (score) between nodes is sum of city grid cell ("heat loss" from the story) */
        private record NextNodeScorer(CityGrid cityGrid) implements Scorer<StateNode> {
            @Override
            public long computeCost(StateNode from, StateNode to) {
                int col = from.position.col, row = from.position.row;
                int dCol = Integer.compare(to.position.col - col, 0);
                int dRow = Integer.compare(to.position.row - row, 0);
                if (dCol == 0 && dRow == 0) {
                    throw new IllegalArgumentException("the same nodes given to scorer: from %s, to %s".formatted(from, to));
                } else if (dCol != 0 & dRow != 0) {
                    throw new IllegalArgumentException("diagonal moves not supported: from %s, to %s".formatted(from, to));
                }
                long result = 0;
                while (col != to.position.col || row != to.position.row) {
                    col += dCol;
                    row += dRow;
                    result += cityGrid.get(col, row);
                }
                return result;
            }
        }
        /** Target node scorer guides route finder search to steer it towards target more effectively */
        private static class TargetNodeScorers {
            private enum Type {
                ZERO {
                    @Override
                    public Scorer<StateNode> getScorer(CityGrid cityGrid, CityGrid.Position target) {
                        return new ZeroScorer();
                    }
                },
                MANHATTAN {
                    @Override
                    public Scorer<StateNode> getScorer(CityGrid cityGrid, CityGrid.Position target) {
                        return new ManhattanDistanceScorer();
                    }
                },
                NAIVE_PATH_COST {
                    @Override
                    public Scorer<StateNode> getScorer(CityGrid cityGrid, CityGrid.Position target) {
                        return new NaivePathCostScorer(cityGrid, target);
                    }
                };
                public abstract Scorer<StateNode> getScorer(CityGrid cityGrid, CityGrid.Position target);
            }
            /** For testing: always returns 0. Can be used to assess cost of using target node scorer at all. */
            private static class ZeroScorer implements Scorer<StateNode> {
                @Override
                public long computeCost(StateNode from, StateNode to) {
                    return 0;
                }
            }
            /** Returns manhattan distance between nodes */
            private static class ManhattanDistanceScorer implements Scorer<StateNode> {
                @Override
                public long computeCost(StateNode from, StateNode to) {
                    return Math.abs(from.position.col - to.position.col) + Math.abs(from.position.row - to.position.row);
                }
            }
            /** Returns minimal cost from node to target node as if any path was permitted. Found by running Dijkstra on city grid. */
            private static class NaivePathCostScorer implements Scorer<StateNode> {
                private final Map<CityGrid.Position, Long> minScore;
                private final Grid<Long> minScoreGrid;
                public NaivePathCostScorer(CityGrid cityGrid, CityGrid.Position target) {
                    Instant searchStart = Instant.now();
                    // traverse graph from target node to calculate least scored path for every grid position (without puzzle constraints)
                    // this will be used as heuristic estimator for A* search guidance
                    RouteFinderDijkstra<CityGrid.Position> graphDijkstra = new RouteFinderDijkstra<>(
                            new GridGraph(cityGrid),
                            (from, to) -> {
                                if (Math.abs(from.col - to.col) + Math.abs(from.row - to.row) != 1) {
                                    throw new IllegalArgumentException("moves longer than 1 step not supported, from %s to %s".formatted(from, to));
                                }
                                return cityGrid.get(to.col, to.row);
                            }
                    );
                    minScore = graphDijkstra.search(target);
                    Instant searchFinish = Instant.now();
                    System.out.printf("### graph search to estimate path costs [elapsed: %s]%n", Duration.between(searchStart, searchFinish).toString());
                    minScoreGrid = new Grid<>(cityGrid.getWidth(), cityGrid.getHeight(), 0L, ",");
                    for (Map.Entry<CityGrid.Position, Long> minScoreEntry : minScore.entrySet()) {
                        CityGrid.Position pos = minScoreEntry.getKey();
                        minScoreGrid.set(pos.col, pos.row, minScoreEntry.getValue());
                    }
                }
                @Override
                public long computeCost(StateNode from, StateNode to) { // assert to.equals(target)
                    return minScoreGrid.get(from.position.col, from.position.row);
                    // return minScore.get(from.position);
                }
                private record GridGraph(CityGrid cityGrid) implements Graph<CityGrid.Position> {
                    @Override
                    public CityGrid.Position getNode(long id) {
                        throw new IllegalStateException("not implemented");
                    }
                    @Override
                    public Set<CityGrid.Position> getEdges(CityGrid.Position pos) {
                        HashSet<CityGrid.Position> result = new HashSet<>();
                        int col = pos.col, row = pos.row;
                        for (Grid.Direction direction : Grid.Direction.getAll()) {
                            int newCol = col + direction.dCol, newRow = row + direction.dRow;
                            if (cityGrid.hasPos(newCol, newRow)) {
                                result.add(cityGrid.getPosition(newCol, newRow));
                            }
                        }
                        return result;
                    }
                }
            }
        }
        @Override
        public long solve() {
            boolean useTargetNodeScorer = false; // testing shows that fastest is null (no target scoring), maybe used scorer (sum of cell costs) is unfit for purpose?
            TargetNodeScorers.Type targetNodeScorerType = TargetNodeScorers.Type.NAIVE_PATH_COST;
            RouteFinder<StateNode> routeFinder = new RouteFinderAStar<>(
                    new StateGraph(cityGrid, runMinimum, runMaximum),
                    new NextNodeScorer(cityGrid),
                    useTargetNodeScorer ? targetNodeScorerType.getScorer(cityGrid, target) : null
            );
            // if (true) return 0;
            StateNode start1 = new StateNode(start, StateNode.Axis.HORIZONTAL);
            StateNode start2 = new StateNode(start, StateNode.Axis.VERTICAL);
            StateNode end = new StateNode(target, StateNode.Axis.VERTICAL); // axis will be ignored when testing for target
            Pair<List<StateNode>, Long> route = routeFinder.findRoute(List.of(start1, start2), end);
            List<StateNode> path = route.getValue0();
            if (false) {
                System.out.printf("%d %s%n", route.getValue1(), String.join(",", path.stream().map(StateNode::toString).toList()));
                System.out.printf("path hash %d%n", Arrays.hashCode(path.toArray(new StateNode[0])));
                debugShowPath("winner", path);
            }
            return route.getValue1();
        }

        private void debugShowPath(String title, List<StateNode> path) {
            Grid<Character> pathMap = new Grid<>(cityGrid.getWidth(), cityGrid.getHeight(), '.', "");
            pathMap.set(path.getFirst().position.col, path.getFirst().position.row, '*');
            char[] routeSymbols = "<>^v".toCharArray();
            for (int ri = 1; ri < path.size(); ri++) {
                StateNode node = path.get(ri), prevNode = path.get(ri - 1);
                int dCol = Math.clamp(node.position.col - prevNode.position.col, -1, 1);
                int dRow = Math.clamp(node.position.row - prevNode.position.row, -1, 1);
                int col = prevNode.position.col, row = prevNode.position.row;
                while (col != node.position.col || row != node.position.row) {
                    col += dCol;
                    row += dRow;
                    pathMap.set(col, row, routeSymbols[row - prevNode.position.row == 0 ? ((1 + Math.clamp(col - prevNode.position.col, -1, 1)) / 2) : (2 + (1 + Math.clamp(row - prevNode.position.row, -1, 1)) / 2)]);
                }
            }
            if (title != null && !title.trim().isEmpty()) {
                System.out.println(title);
            }
            System.out.println(pathMap);
        }
    }

    private static class SolverJKK extends SolverBase {
        private enum Direction {
            UNKNOWN(-1), DOWN(0), LEFT(1), UP(2), RIGHT(3);
            public final int dir;
            Direction(int dir) {
                this.dir = dir;
            }
            static final Direction[] values = values();
            static Direction fromValue(int dirValue) {
                for (Direction dir : values) {
                    if (dir.dir == dirValue) {
                        return dir;
                    }
                }
                return Direction.UNKNOWN;
            }
        }

        private Map<Integer, Pair<Integer, CityGrid.Position>> bestPathsForConstraintAndNode;
        private int bestPathsForConstraintAndNodeMemoKey(Direction lastDir, int lastStepsInTheSameDir, int nodeId) {
            return lastDir.dir + 4 * (lastStepsInTheSameDir - 1 + runMaximum * nodeId);
        }

        private void memoAll() {
            if (cityGrid.getWidth() != cityGrid.getHeight()) {
                throw new IllegalArgumentException("non square");
            }
            int memoAllBestScore = Integer.MAX_VALUE;
            // if (this.getInputSuffix().equals("_test_meandering_loopy")) memoAllBestScore = 190;
            // else if (this.getInputSuffix().equals("_test_meandering_loopy_small0")) memoAllBestScore = 63;
            // else if (this.getInputSuffix().equals("_test_meandering_loopy_small1")) memoAllBestScore = 116;
            // if (false)
            for (int col = cityGrid.getWidth() - 1 - 1; col >= 0; col--) {
                int diagMax = cityGrid.getWidth() - 1 - col;
                for (int diag = 0; diag <= diagMax; diag++) {
                    // System.out.printf("memo all: col diagonal %d/%d, pos %d/%d >", cityGrid.getWidth() - (col + 1), cityGrid.getWidth(), diag + 1, diagMax + 1);
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
                    // System.out.printf("memo all: row diagonal %d/%d, pos %d/%d >", cityGrid.getWidth() - (row + 1), cityGrid.getWidth(), diag + 1, diagMax + 1);
                    int startCol = 0 + diag, startRow = row - diag;
                    // System.out.printf("%d,%d%n", startCol, startRow);
                    memoAll(startCol, startRow, memoAllBestScore);
                    // System.out.println();
                }
                // memoDebug();
            }
        }
        private void memoAll(int startCol, int startRow, int memoBestScore) {
            Pathfinder pathfinder = new Pathfinder(cityGrid.getPosition(startCol, startRow), target, null, memoBestScore);
            if (startCol != 0) {
                for (int steps = 1; steps <= runMaximum && startCol - steps >= 0; steps++) {
                    pathfinder.pathMark(startCol - steps, startRow, true);
                    pathfinder.setBestScore(memoBestScore);
                    pathfinder.findPath(0, steps, 0, Direction.RIGHT);
                }
                for (int steps = 1; steps <= runMaximum && startCol - steps >= 0; steps++) {
                    pathfinder.pathMark(startCol - steps, startRow, false);
                }
            }
            if (startRow != 0) {
                for (int steps = 1; steps <= runMaximum && startRow - steps >= 0; steps++) {
                    pathfinder.pathMark(startCol, startRow - steps, true);
                    pathfinder.setBestScore(memoBestScore);
                    pathfinder.findPath(0, 0, steps, Direction.DOWN);
                }
                for (int steps = 1; steps <= runMaximum && startRow - steps >= 0; steps++) {
                    pathfinder.pathMark(startCol, startRow - steps, false);
                }
            }
            // pathfinder.memoStatsReport();
            // ignore LEFT and UP directions - memoize only towards target
            // if (startCol != cityGrid.getWidth() - 1) {
            //     for (int steps = 1; steps <= MAX_STEPS && startCol + steps < cityGrid.getWidth(); steps++) {
            //         pathfinder.pathMark(startCol + steps, startRow, true);
            //         pathfinder.setBestScore(memoBestScore);
            //         pathfinder.findPath(0, steps, 0, Direction.LEFT);
            //     }
            //     for (int steps = 1; steps <= MAX_STEPS && startCol + steps < cityGrid.getWidth(); steps++) {
            //         pathfinder.pathMark(startCol + steps, startRow, false);
            //     }
            // }
            // if (startRow != cityGrid.getHeight() - 1) {
            //     for (int steps = 1; steps <= MAX_STEPS && startRow + steps < cityGrid.getHeight(); steps++) {
            //         pathfinder.pathMark(startCol, startRow + steps, true);
            //         pathfinder.setBestScore(memoBestScore);
            //         pathfinder.findPath(0, 0, steps, Direction.UP);
            //     }
            //     for (int steps = 1; steps <= MAX_STEPS && startRow + steps < cityGrid.getHeight(); steps++) {
            //         pathfinder.pathMark(startCol, startRow + steps, false);
            //     }
            // }
        }
        private void memoDebug() {
            Grid<List<Triplet<Direction, Integer, CityGrid.Position>>> memoCheck = new Grid<>(cityGrid.getWidth(), cityGrid.getHeight(), null, ArrayList.class, " ||| ");
            for (Map.Entry<Integer, Pair<Integer, CityGrid.Position>> entry : bestPathsForConstraintAndNode.entrySet()) {
                int key = entry.getKey();
                // memoKey = lastDir + 4 * (lastStepsInTheSameDir - 1 + MAX_STEPS * current.getId());
                int nodeId = key / 4 / runMaximum;
                Direction lastDir = Direction.fromValue(key % 4);
                int lastStepsInTheSameDir = (key / 4) % runMaximum + 1;
                CityGrid.Position node = cityGrid.getPosition(nodeId);
                CityGrid.Position memoNode = entry.getValue().getValue1();
                List<Triplet<Direction, Integer, CityGrid.Position>> memos;
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
                memos.sort(Comparator.comparingInt(n -> (n.getValue0().dir * runMaximum + (n.getValue1() - 1)) * cityGrid.getUniqueIdMax() + (int)n.getValue2().getId()));
            }
            return;
        }

        private class Pathfinder {
            private final CityGrid.Position target;
            private final BiConsumer<List<CityGrid.Position>, Integer> onPathFound;
            private int bestScore;

            private final Comparator<CityGrid.Position> nodeComparator;

            public Pathfinder(CityGrid.Position source, CityGrid.Position target, BiConsumer<List<CityGrid.Position>, Integer> onPathFound, int bestScore) {
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
                pathMarkers = new Grid<>(cityGrid.getWidth(), cityGrid.getHeight(), false, "");
                pathMarkers.set(source.col, source.row, true);
            }

            private final List<CityGrid.Position> path;
            private final Grid<Boolean> pathMarkers;

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
                CityGrid.Position current = path.getLast();
                Integer memoKey = null;
                if (lastDir != Direction.UNKNOWN) {
                    int lastStepsInTheSameDir = Math.max(stepsRows, stepsCols);
                    memoKey = bestPathsForConstraintAndNodeMemoKey(lastDir, lastStepsInTheSameDir, (int)current.getId());
                    Pair<Integer, CityGrid.Position> memoBest = bestPathsForConstraintAndNode.get(memoKey);
                    memoQueries++;
                    if (memoBest != null) {
                        // System.out.println("memo hit");
                        memoHits++;
                        CityGrid.Position bestNode = memoBest.getValue1();
                        if (bestNode != null) {
                            memoFoundNodes++;
                            int bestPathScore = score + memoBest.getValue0();
                            // System.out.printf("memo: score %d + memo score %d = %d%n", score, memoBest.getValue0(), bestPathScore);
                            // debugShowPath(path);
                            if (bestScore > bestPathScore) {
                                memoSuccesses++;
                                if (false && path.getFirst().getId() == 0) {
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
                final int diagonalSum = firstCol + firstRow - runMinimum; // -MIN_STEPS is needed to solve the puzzle, it allows path to go MIN_STEPS diagonal above currently memoized one, it won't work for long looping/winding paths
                List<CityGrid.Position> connections = new ArrayList<>(4);
                if (stepsCols == 0 && stepsRows == 0 || stepsCols >= runMinimum || stepsRows >= 1) {
                    if (currCol > 0 && !pathMarkers.get(currCol - 1, currRow) && stepsRows < runMaximum && diagonalSum <= currCol - 1 + currRow) {
                        connections.add(cityGrid.getPosition(currCol - 1, currRow));
                    }
                    if (currCol < cityGrid.getWidth() - 1 && !pathMarkers.get(currCol + 1, currRow) && stepsRows < runMaximum && diagonalSum <= currCol + 1 + currRow) {
                        connections.add(cityGrid.getPosition(currCol + 1, currRow));
                    }
                }
                if (stepsCols == 0 && stepsRows == 0 || stepsRows >= runMinimum || stepsCols >= 1) {
                    if (currRow > 0 && !pathMarkers.get(currCol, currRow - 1) && stepsCols < runMaximum && diagonalSum <= currCol + currRow - 1) {
                        connections.add(cityGrid.getPosition(currCol, currRow - 1));
                    }
                    if (currRow < cityGrid.getHeight() - 1 && !pathMarkers.get(currCol, currRow + 1) && stepsCols < runMaximum && diagonalSum <= currCol + currRow + 1) {
                        connections.add(cityGrid.getPosition(currCol, currRow + 1));
                    }
                }
                connections.sort(nodeComparator);
                CityGrid.Position bestNode = null;
                for (CityGrid.Position connection : connections) {
                    // System.out.printf("%d: ->%s".formatted(path.size(), connection));
                    int nextScore = score + cityGrid.get(connection.col, connection.row); // graph.heatLossAdjacentNodes.get(new Pair<>(current.getId(), connection.getId()));
                    // System.out.printf(" score %d%n", nextScore);
                    // System.out.printf("%d: ->%s score %d", path.size(), connection, nextScore);
                    if (nextScore <= bestScore) { // less or equal to show equal score paths
                        // System.out.println();
                        path.addLast(connection);
                        pathMarkers.set(connection.col, connection.row, true);
                        int nextStepsRows, nextStepsCols;
                        boolean isStepRows = currRow == connection.row;
                        if (isStepRows) {
                            nextStepsRows = stepsRows + 1;
                            nextStepsCols = 0;
                        } else {
                            nextStepsRows = 0;
                            nextStepsCols = stepsCols + 1;
                        }
                        Direction nextDir;
                        if (currRow == connection.row) {
                            nextDir = currCol < connection.col ? Direction.RIGHT : Direction.LEFT;
                        } else {
                            nextDir = currRow < connection.row ? Direction.DOWN : Direction.UP;
                        }
                        if (connection.equals(target)) {
                            if ((nextStepsCols == 0 && nextStepsRows >= runMinimum) ||
                                    (nextStepsRows == 0 && nextStepsCols >= runMinimum)) {
                                bestScore = nextScore;
                                if (onPathFound != null) {
                                    onPathFound.accept(path, nextScore);
                                }
                                bestNode = connection;
                            }
                        } else {
                            // debugShowPath(path);
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
                    Pair<Integer, CityGrid.Position> memoBest = bestPathsForConstraintAndNode.get(memoKey);
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

        @Override
        protected long solve() {
            bestPathsForConstraintAndNode = new HashMap<>();
            List<CityGrid.Position> winnerPath = new LinkedList<>();
            AtomicInteger counter = new AtomicInteger(0);
            BiConsumer<List<CityGrid.Position>, Integer> onPathFound = (path, score) -> {
                if (path.getFirst().getId() != 0) {
                    return;
                }
                if (path != winnerPath) {
                    winnerPath.clear();
                    winnerPath.addAll(path);
                    counter.incrementAndGet();
                }
                if (path == winnerPath) {
                    // System.out.printf("#%d: %d %s%n", counter.get(), score, String.join(",", path.stream().map(CityGrid.Position::toString).toList()));
                    // System.out.printf("path hash %d%n", Arrays.hashCode(path.toArray(new CityGrid.Position[0])));
                    // debugShowPath("path before reconstruction:", winnerPath);
                    if (winnerPath.getLast() != target) {
                        // reconstruct path finish from memo, check score, check memo best scores
                        int scoreCheck = 0;
                        int currentPathIdx = 0;
                        CityGrid.Position current = winnerPath.get(currentPathIdx);
                        Direction lastDir = Direction.UNKNOWN;
                        int lastStepsCols = 0, lastStepsRows = 0;
                        do {
                            CityGrid.Position next;
                            if (currentPathIdx < winnerPath.size() - 1) {
                                currentPathIdx++;
                                next = winnerPath.get(currentPathIdx);
                            } else {
                                if (lastDir == Direction.UNKNOWN) {
                                    throw new IllegalStateException();
                                }
                                int memoKey = bestPathsForConstraintAndNodeMemoKey(lastDir, Math.max(lastStepsCols, lastStepsRows), (int)current.getId());
                                Pair<Integer, CityGrid.Position> memoBest = bestPathsForConstraintAndNode.get(memoKey);
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
                            if (current.row == next.row) {
                                lastDir = current.col < next.col ? Direction.RIGHT : Direction.LEFT;
                            } else {
                                lastDir = current.row < next.row ? Direction.DOWN : Direction.UP;
                            }
                            current = next;
                        } while (current != target);
                        if (false) {
                            System.out.printf("#%d: %d %s%n", counter.get(), score, String.join(",", winnerPath.stream().map(CityGrid.Position::toString).toList()));
                            System.out.printf("path hash %d%n", Arrays.hashCode(winnerPath.toArray(new CityGrid.Position[0])));
                            debugShowPath("path reconstructed:", winnerPath);
                        }
                        if (score != scoreCheck) {
                            throw new IllegalStateException();
                        }
                    }
                }
            };
            int startingBestScore = Integer.MAX_VALUE;
            Pathfinder pathfinder = new Pathfinder(start, target, onPathFound, startingBestScore);

            // if (false)
            memoAll();

            pathfinder.findPath(0, 0, 0, Direction.UNKNOWN);
            // pathfinder.memoStatsReport();
            // System.out.print("winner ");
            onPathFound.accept(winnerPath, pathfinder.getBestScore());
            long result = pathfinder.getBestScore();
            return result;
        }

        private void debugShowPath(String title, List<CityGrid.Position> path) {
            Grid<Character> pathMap = new Grid<>(cityGrid.getWidth(), cityGrid.getHeight(), '.', "");
            pathMap.set(path.getFirst().col, path.getFirst().row, '*');
            char[] routeSymbols = "<>^v".toCharArray();
            for (int ri = 1; ri < path.size(); ri++) {
                CityGrid.Position node = path.get(ri), prevNode = path.get(ri - 1);
                pathMap.set(node.col, node.row, routeSymbols[node.row - prevNode.row == 0 ? ((1 + Math.clamp(node.col - prevNode.col, -1, 1)) / 2) : (2 + (1 + Math.clamp(node.row - prevNode.row, -1, 1)) / 2)]);
            }
            if (title != null && !title.trim().isEmpty()) {
                System.out.println(title);
            }
            System.out.println(pathMap);
        }
    }

    public static class Day17Test {
        @Nested
        public class Day17Test_AoCInputs {
            @Nested
            class Day17Test_AoCInputs_JKK {
                @Test
                void solvePart1_small() {
                    solvePart1(SolverType.JKK_BOTTOMUP, 102L, "_sample");
                }
                @Test
                void solvePart1_main() {
                    solvePart1(SolverType.JKK_BOTTOMUP, 851L, "");
                }
                @Test
                void solvePart2_small() {
                    solvePart2(SolverType.JKK_BOTTOMUP, 94L, "_sample");
                }
                @Test
                void solvePart2_main() {
                    solvePart2(SolverType.JKK_BOTTOMUP, 982L, "");
                }
            }

            @Nested
            class Day17Test_AoCInputs_AStar {
                @Test
                void solvePart1_small() {
                    solvePart1(SolverType.ASTAR, 102L, "_sample");
                }
                @Test
                void solvePart1_main() {
                    solvePart1(SolverType.ASTAR, 851L, "");
                }
                @Test
                void solvePart2_small() {
                    solvePart2(SolverType.ASTAR, 94L, "_sample");
                }
                @Test
                void solvePart2_main() {
                    solvePart2(SolverType.ASTAR, 982L, "");
                }
            }
        }
        @Nested
        public class Day17Test_Cases {
            @Nested
            class Day17Test_Cases_sample_subset_10x10 {
                @Test void test_sample_subset_10x10_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 94L, "_sample_subset_10x10"); }
                @Test void test_sample_subset_10x10_ASTAR() { solvePart1(SolverType.ASTAR, 94L, "_sample_subset_10x10"); }
            }
            @Nested
            class Day17Test_Cases_test_2way_12x12 {
                @Test void test_test_2way_12x12_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 94L, "_test_2way_12x12"); }
                @Test void test_test_2way_12x12_ASTAR() { solvePart1(SolverType.ASTAR, 94L, "_test_2way_12x12"); }
            }
            @Nested
            class Day17Test_Cases_test_directionmatters {
                @Test void test_test_directionmatters_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 9L, "_test_directionmatters"); }
                @Test void test_test_directionmatters_ASTAR() { solvePart1(SolverType.ASTAR, 9L, "_test_directionmatters"); }
            }
            @Nested
            class Day17Test_Cases_test_blackhole_loopy_7x7_harder {
                @Test void test_test_blackhole_loopy_7x7_harder_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 28L, "_test_blackhole_loopy_7x7_harder"); }
                @Test void test_test_blackhole_loopy_7x7_harder_ASTAR() { solvePart1(SolverType.ASTAR, 28L, "_test_blackhole_loopy_7x7_harder"); }
            }
            @Nested
            class Day17Test_Cases_test_blackhole_loopy_8x8_easier {
                @Test void test_test_blackhole_loopy_8x8_easier_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 34L, "_test_blackhole_loopy_8x8_easier"); }
                @Test void test_test_blackhole_loopy_8x8_easier_ASTAR() { solvePart1(SolverType.ASTAR, 34L, "_test_blackhole_loopy_8x8_easier"); }
            }
            @Nested
            class Day17Test_Cases_test_meandering_9x9 {
                @Test void test_test_meandering_9x9_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 54L, "_test_meandering_9x9"); }
                @Test void test_test_meandering_9x9_ASTAR() { solvePart1(SolverType.ASTAR, 54L, "_test_meandering_9x9"); }
            }
            @Nested
            class Day17Test_Cases_test_meandering_loopy_30x30 {
                @Test void test_test_meandering_loopy_30x30_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 190L, "_test_meandering_loopy_30x30"); }
                @Test void test_test_meandering_loopy_30x30_ASTAR() { solvePart1(SolverType.ASTAR, 190L, "_test_meandering_loopy_30x30"); }
            }
            @Nested
            class Day17Test_Cases_test_meandering_loopy_24x24_1 {
                @Test void test_test_meandering_loopy_24x24_1_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 0L, "_test_meandering_loopy_24x24_1"); }
                @Test void test_test_meandering_loopy_24x24_1_ASTAR() { solvePart1(SolverType.ASTAR, 0L, "_test_meandering_loopy_24x24_1"); }
            }
            @Nested
            class Day17Test_Cases_test_meandering_loopy_24x24_2 {
                @Test void test_test_meandering_loopy_24x24_2_JKK() { solvePart1(SolverType.JKK_BOTTOMUP, 116L, "_test_meandering_loopy_24x24_2"); }
                @Test void test_test_meandering_loopy_24x24_2_ASTAR() { solvePart1(SolverType.ASTAR, 116L, "_test_meandering_loopy_24x24_2"); }
            }
        }
        static void solvePart1(SolverType solverType, long expectedResult, String inputSuffix) {
            solve(solverType, expectedResult, inputSuffix, 1, 3);
        }
        static void solvePart2(SolverType solverType, long expectedResult, String inputSuffix) {
            solve(solverType, expectedResult, inputSuffix, 4, 10);
        }
        static void solve(SolverType solverType, long expectedResult, String inputSuffix, int runMinimum, int runMaximum) {
            var day = new Day17(inputSuffix);
            day.parse();
            assertEquals(expectedResult, day.solve(solverType, runMinimum, runMaximum));
        }
    }
    public static class Day17Test_SolverBenchmark {
        @Test
        void test_main() {
            bench(new SolverType[] { SolverType.JKK_BOTTOMUP, SolverType.ASTAR }, 851L, "", 1, 3);
            bench(new SolverType[] { SolverType.JKK_BOTTOMUP, SolverType.ASTAR }, 982L, "", 4, 10);
        }
        void bench(SolverType[] solverTypes, long expectedResult, String inputSuffix, int runMinimum, int runMaximum) {
            Day17 day17 = new Day17(inputSuffix);
            day17.parse();
            System.out.printf("### benchmark of \"%s\" with run min %d max %d%n", inputSuffix, runMinimum, runMaximum);
            for (SolverType solverType : solverTypes) {
                Instant start = Instant.now();
                test(solverType, expectedResult, day17, runMinimum, runMaximum);
                Instant finish = Instant.now();
                System.out.printf("### %-20s elapsed for solver \"%s\"%n", Duration.between(start, finish).toString(), solverType);
            }
        }
        void test(SolverType solverType, long expectedResult, Day17 day17, int runMinimum, int runMaximum) {
            assertEquals(expectedResult, day17.solve(solverType, runMinimum, runMaximum));
        }
    }
}
/*

--- Day 17: Clumsy Crucible ---

The lava starts flowing rapidly once the Lava Production Facility is operational. As you leave, the reindeer offers you a parachute, allowing you to quickly reach Gear Island.

As you descend, your bird's-eye view of Gear Island reveals why you had trouble finding anyone on your way up: half of Gear Island is empty, but the half below you is a giant factory city!

You land near the gradually-filling pool of lava at the base of your new lavafall. Lavaducts will eventually carry the lava throughout the city, but to make use of it immediately, Elves are loading it into large crucibles on wheels.

The crucibles are top-heavy and pushed by hand. Unfortunately, the crucibles become very difficult to steer at high speeds, and so it can be hard to go in a straight line for very long.

To get Desert Island the machine parts it needs as soon as possible, you'll need to find the best way to get the crucible from the lava pool to the machine parts factory. To do this, you need to minimize heat loss while choosing a route that doesn't require the crucible to go in a straight line for too long.

Fortunately, the Elves here have a map (your puzzle input) that uses traffic patterns, ambient temperature, and hundreds of other parameters to calculate exactly how much heat loss can be expected for a crucible entering any particular city block.

For example:

2413432311323
3215453535623
3255245654254
3446585845452
4546657867536
1438598798454
4457876987766
3637877979653
4654967986887
4564679986453
1224686865563
2546548887735
4322674655533

Each city block is marked by a single digit that represents the amount of heat loss if the crucible enters that block. The starting point, the lava pool, is the top-left city block; the destination, the machine parts factory, is the bottom-right city block. (Because you already start in the top-left block, you don't incur that block's heat loss unless you leave that block and then return to it.)

Because it is difficult to keep the top-heavy crucible going in a straight line for very long, it can move at most three blocks in a single direction before it must turn 90 degrees left or right. The crucible also can't reverse direction; after entering each city block, it may only turn left, continue straight, or turn right.

One way to minimize heat loss is this path:

2>>34^>>>1323
32v>>>35v5623
32552456v>>54
3446585845v52
4546657867v>6
14385987984v4
44578769877v6
36378779796v>
465496798688v
456467998645v
12246868655<v
25465488877v5
43226746555v>

This path never moves more than three consecutive blocks in the same direction and incurs a heat loss of only 102.

Directing the crucible from the lava pool to the machine parts factory, but not moving more than three consecutive blocks in the same direction, what is the least heat loss it can incur?

Your puzzle answer was 851.

--- Part Two ---

The crucibles of lava simply aren't large enough to provide an adequate supply of lava to the machine parts factory. Instead, the Elves are going to upgrade to ultra crucibles.

Ultra crucibles are even more difficult to steer than normal crucibles. Not only do they have trouble going in a straight line, but they also have trouble turning!

Once an ultra crucible starts moving in a direction, it needs to move a minimum of four blocks in that direction before it can turn (or even before it can stop at the end). However, it will eventually start to get wobbly: an ultra crucible can move a maximum of ten consecutive blocks without turning.

In the above example, an ultra crucible could follow this path to minimize heat loss:

2>>>>>>>>1323
32154535v5623
32552456v4254
34465858v5452
45466578v>>>>
143859879845v
445787698776v
363787797965v
465496798688v
456467998645v
122468686556v
254654888773v
432267465553v

In the above example, an ultra crucible would incur the minimum possible heat loss of 94.

Here's another example:

111111111111
999999999991
999999999991
999999999991
999999999991

Sadly, an ultra crucible would need to take an unfortunate path like this one:

1>>>>>>>1111
9999999v9991
9999999v9991
9999999v9991
9999999v>>>>

This route causes the ultra crucible to incur the minimum possible heat loss of 71.

Directing the ultra crucible from the lava pool to the machine parts factory, what is the least heat loss it can incur?

Your puzzle answer was 982.

 */