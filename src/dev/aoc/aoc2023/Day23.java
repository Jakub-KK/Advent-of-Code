package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.Grid;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import dev.aoc.common.graphsearch.*;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day23 extends Day {
    public Day23(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day23("")); // _sample, _test_1, _test_2
    }

    private static class ForestGrid extends Grid<Character> {
        public final Pair<Integer, Integer> startCell, endCell;
        public final int maxSteps;

        public ForestGrid(List<String> lines, String elementDelimiter, Function<String, Character> parser) {
            super(lines, elementDelimiter, parser, Character.class);
            // check for border of forest '#' with entry and exit paths '.'
            int borderCount = count((col, row) -> ((col == 0 || col == getWidth() - 1) || (row == 0 || row == getHeight() - 1)) && is(col, row, '#'));
            if (borderCount != 2 * getWidth() + 2 * getHeight() - 4 - 2) {
                throw new IllegalArgumentException("input border must be forest '#' with only two paths '.' for start and end");
            }
            // init start, end cells
            startCell = new Pair<>(1, 0);
            if (!is(startCell.getValue0(), startCell.getValue1(), '.')) {
                throw new IllegalArgumentException("start cell at (%d,%d) must be marked as path '.'".formatted(startCell.getValue0(), startCell.getValue1()));
            }
            endCell = new Pair<>(getWidth() - 2, getHeight() - 1);
            if (!is(endCell.getValue0(), endCell.getValue1(), '.')) {
                throw new IllegalArgumentException("end cell at (%d,%d) must be marked as path '.'".formatted(endCell.getValue0(), endCell.getValue1()));
            }
            // calculate absolute max steps from start to end (visiting every grid cell)
            maxSteps = getUniqueIdMax();
        }
    }

    private ForestGrid forestGrid;

    private enum Direction {
        UNKNOWN(-1), DOWN(1), LEFT(2), UP(3), RIGHT(0);

        public final int dir;
        Direction(int dir) {
            this.dir = dir;
        }
        private static final Direction[] values = values();
        public static Direction fromValue(int dirValue) {
            for (Direction dir : values) {
                if (dir.dir == dirValue) {
                    return dir;
                }
            }
            throw new IllegalArgumentException("unknown direction value %d".formatted(dirValue));
            // return Direction.UNKNOWN;
        }
        public static Direction fromLetter(char letter) {
            return switch (letter) {
                case 'D' -> DOWN;
                case 'L' -> LEFT;
                case 'U' -> UP;
                case 'R' -> RIGHT;
                default -> UNKNOWN;
            };
        }
        public Grid.Direction toGridDirection() {
            return switch (this) {
                case RIGHT -> Grid.Direction.RIGHT;
                case DOWN -> Grid.Direction.DOWN;
                case LEFT -> Grid.Direction.LEFT;
                case UP -> Grid.Direction.UP;
                default -> Grid.Direction.UNKNOWN;
            };
        }
        public static Direction[] getAll() {
            return new Direction[] { Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT };
        }
        public Direction reverse() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
                case UNKNOWN -> UNKNOWN;
            };
        }
    }

    private class PathNode implements GraphNode {
        public final int col, row;

        public PathNode(int col, int row) {
            this.col = col;
            this.row = row;
        }

        @Override
        public long getId() {
            return forestGrid.getUniqueId(col, row);
        }

        @Override
        public boolean equalsTarget(GraphNode target) {
            return equals(target);
        }

        public boolean equals(PathNode that) {
            return col == that.col && row == that.row;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathNode pathNode = (PathNode) o;
            return col == pathNode.col && row == pathNode.row;
        }

        @Override
        public int hashCode() {
            return Objects.hash(col, row);
        }

        @Override
        public String toString() {
            return "[%d,%d]".formatted(col, row);
        }
    }
    private PathNode pathNodeFromId(long idCoords) {
        // TODO: caching?
        int col = (int) (idCoords % forestGrid.getWidth());
        int row = (int) (idCoords / forestGrid.getWidth());
        return new PathNode(col, row);
    }
    private abstract class ForestGraph implements Graph<PathNode> {
        public final PathNode start, end;
        public ForestGraph() {
            start = new PathNode(forestGrid.startCell.getValue0(), forestGrid.startCell.getValue1());
            end = new PathNode(forestGrid.endCell.getValue0(), forestGrid.endCell.getValue1());
        }

        @Override
        public PathNode getNode(long id) {
            return pathNodeFromId(id);
        }

        @Override
        public abstract Set<PathNode> getEdges(PathNode node);
    }
    private class ForestWithoutSlopesGraph extends ForestGraph {
        @Override
        public Set<PathNode> getEdges(PathNode node) {
            HashSet<PathNode> results = new HashSet<>(4);
            Arrays.stream(Direction.getAll()).forEach(dir -> {
                Grid.Direction gridDir = dir.toGridDirection();
                int thatCol = node.col + gridDir.dCol, thatRow = node.row + gridDir.dRow;
                if (!forestGrid.hasPos(thatCol, thatRow)) {
                    return;
                }
                char cell = forestGrid.get(thatCol, thatRow);
                if (cell == '#') {
                    return;
                }
                results.add(new PathNode(thatCol, thatRow));
            });
            return results;
        }
    }
    private class NextPathNodeScorer implements Scorer<PathNode> {
        @Override
        public long computeCost(PathNode from, PathNode to) {
            if (Math.abs(from.col - to.col) + Math.abs(from.row - to.row) != 1) {
                throw new IllegalArgumentException("non-adjacent nodes given to scorer, from %s, to %s".formatted(from, to));
            }
            return 1;
        }
    }
    private class TargetEstimatePathNodeScorer implements Scorer<PathNode> {
        @Override
        public long computeCost(PathNode from, PathNode to) {
            return -(Math.abs(from.col - to.col) + Math.abs(from.row - to.row)); // reverse to try first connections more distanced from target in hope of longer paths
        }
    }
    private class CrossingsGraph implements Graph<PathNode> {
        public final PathNode start, end;
        public final Set<PathNode> nodes;
        public final Map<Pair<PathNode, PathNode>, Integer> edgeWeights;
        private final Map<PathNode, Set<PathNode>> edges;

        public CrossingsGraph(Set<PathNode> nodes, Map<Pair<PathNode, PathNode>, Integer> edgeWeights) {
            start = new PathNode(forestGrid.startCell.getValue0(), forestGrid.startCell.getValue1());
            if (!nodes.contains(start)) {
                throw new IllegalArgumentException("start node missing");
            }
            end = new PathNode(forestGrid.endCell.getValue0(), forestGrid.endCell.getValue1());
            if (!nodes.contains(end)) {
                throw new IllegalArgumentException("end node missing");
            }
            this.nodes = nodes;
            this.edgeWeights = edgeWeights;
            edges = new HashMap<>();
            for (Pair<PathNode, PathNode> edges : edgeWeights.keySet()) {
                Set<PathNode> nodeA = this.edges.computeIfAbsent(edges.getValue0(), key -> new HashSet<>());
                nodeA.add(edges.getValue1());
                Set<PathNode> nodeB = this.edges.computeIfAbsent(edges.getValue1(), key -> new HashSet<>());
                nodeB.add(edges.getValue0());
            }
            if (!edges.containsKey(start) || edges.get(start).isEmpty()) {
                throw new IllegalArgumentException("start node not connected to graph");
            }
            if (!edges.containsKey(end) || edges.get(end).isEmpty()) {
                throw new IllegalArgumentException("end node not connected to graph");
            }
            // System.out.printf("crossings graph nodes %s%n".formatted(String.join(", ", nodes.stream().map(PathNode::toString).toList())));
        }

        @Override
        public PathNode getNode(long id) {
            PathNode node = pathNodeFromId(id);
            if (!nodes.contains(node)) {
                throw new IllegalArgumentException("invalid node for this graph");
            }
            return node;
        }

        @Override
        public Set<PathNode> getEdges(PathNode node) {
            return edges.get(node);
        }
    }
    private class NextCrossingsPathNodeScorer implements Scorer<PathNode> {
        private final CrossingsGraph graph;
        public NextCrossingsPathNodeScorer(CrossingsGraph graph) {
            this.graph = graph;
        }
        @Override
        public long computeCost(PathNode from, PathNode to) {
            boolean isOrdered = from.getId() < to.getId();
            return graph.edgeWeights.get(new Pair<>(isOrdered ? from : to, isOrdered ? to : from));
        }
    }
    private class TargetEstimateCrossingsPathNodeScorer implements Scorer<PathNode> {
        @Override
        public long computeCost(PathNode from, PathNode to) {
            return 0; // ignore target estimation
        }
    }

    private class PathDirectedNode extends PathNode {
        public final Direction direction;

        public PathDirectedNode(int col, int row, Direction direction) {
            super(col, row);
            this.direction = direction;
        }

        @Override
        public long getId() {
            return super.getId() * 4 + direction.dir;
        }

        public boolean equals(PathDirectedNode that) {
            return super.equals(that) && direction == that.direction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathDirectedNode pathDirectedNode = (PathDirectedNode) o;
            return super.equals(pathDirectedNode) && direction == pathDirectedNode.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), direction);
        }

        @Override
        public String toString() {
            return "[%d,%d|%s]".formatted(col, row, direction);
        }
    }
    private PathDirectedNode pathDirectedNodeFromId(long idCoordsAndDir) {
        // TODO: caching?
        Direction direction = Direction.fromValue((int) (idCoordsAndDir % 4));
        long idCoords = idCoordsAndDir / 4;
        int col = (int) (idCoords % forestGrid.getWidth());
        int row = (int) (idCoords / forestGrid.getWidth());
        return new PathDirectedNode(col, row, direction);
    }
    private abstract class ForestDirectedGraph implements Graph<PathDirectedNode> {
        public final PathDirectedNode start, end;
        public ForestDirectedGraph() {
            start = new PathDirectedNode(forestGrid.startCell.getValue0(), forestGrid.startCell.getValue1(), Direction.DOWN);
            end = new PathDirectedNode(forestGrid.endCell.getValue0(), forestGrid.endCell.getValue1(), Direction.DOWN);
        }

        @Override
        public PathDirectedNode getNode(long id) {
            return pathDirectedNodeFromId(id);
        }

        @Override
        public abstract Set<PathDirectedNode> getEdges(PathDirectedNode node);
    }
    private class ForestWithSlopesGraph extends ForestDirectedGraph {
        @Override
        public Set<PathDirectedNode> getEdges(PathDirectedNode node) {
            HashSet<PathDirectedNode> results = new HashSet<>(4);
            Direction nodeDir = node.direction;
            Direction noTurnBackDir = nodeDir.reverse();
            Arrays.stream(Direction.getAll()).forEach(dir -> {
                if (dir == noTurnBackDir) {
                    return;
                }
                Grid.Direction gridDir = dir.toGridDirection();
                int thatCol = node.col + gridDir.dCol, thatRow = node.row + gridDir.dRow;
                if (!forestGrid.hasPos(thatCol, thatRow)) {
                    return;
                }
                char cell = forestGrid.get(thatCol, thatRow);
                if (cell == '#') {
                    return;
                }
                char disallowedSlope = switch (dir) {
                    case RIGHT -> '<';
                    case DOWN -> '^';
                    case LEFT -> '>';
                    case UP -> 'v';
                    case UNKNOWN -> '.';
                };
                if (cell == disallowedSlope) {
                    return;
                }
                // char allowedSlope = switch (dir) {
                //     case RIGHT -> '>';
                //     case DOWN -> 'v';
                //     case LEFT -> '<';
                //     case UP -> '^';
                //     case UNKNOWN -> '.';
                // };
                results.add(new PathDirectedNode(thatCol, thatRow, dir));
            });
            return results;
        }
    }
    private class NextPathDirectedNodeScorer implements Scorer<PathDirectedNode> {
        @Override
        public long computeCost(PathDirectedNode from, PathDirectedNode to) {
            if (Math.abs(from.col - to.col) + Math.abs(from.row - to.row) != 1) {
                throw new IllegalArgumentException("non-adjacent nodes given to scorer, from %s, to %s".formatted(from, to));
            }
            return 1;
        }
    }
    private class TargetEstimatePathDirectedNodeScorer implements Scorer<PathDirectedNode> {
        @Override
        public long computeCost(PathDirectedNode from, PathDirectedNode to) {
            return (Math.abs(from.col - to.col) + Math.abs(from.row - to.row)); // reverse to try first connections more distanced from target in hope of longer paths
        }
    }

    private void parse() {
        var mapStrings = stream().collect(Collectors.toList());
        parse(mapStrings);
    }
    private void parse(List<String> mapStrings) {
        forestGrid = new ForestGrid(mapStrings, "", s -> s.charAt(0));
        System.out.printf("forest grid %d x %d, hash %d%n", forestGrid.getWidth(), forestGrid.getHeight(), forestGrid.hashCode());
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // if (true) return null;
        ForestDirectedGraph forestGraph = new ForestWithSlopesGraph();
        final long[] maxScore = { Long.MIN_VALUE };
        RouteFinderDFS<PathDirectedNode> routeFinder = new RouteFinderDFS<>(forestGraph, new NextPathDirectedNodeScorer(), new TargetEstimatePathDirectedNodeScorer()) {
            @Override
            protected FoundRouteDecision foundRoute(List<PathDirectedNode> route, long score) {
                if (maxScore[0] < score) {
                    maxScore[0] = score;
                    // System.out.printf("found better route, score %d%n", score);
                    return FoundRouteDecision.REMEMBER;
                } else {
                    return FoundRouteDecision.IGNORE;
                }
            }
        };
        Pair<List<PathDirectedNode>, Long> routeWithScore = routeFinder.findRoute(forestGraph.start, forestGraph.end);
        long result = routeWithScore.getValue1();
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        // input is long stretches of corridors with small number of crossings (marked by slopes, which we ignore in part 2)
        // find all crossings, use search from part 1 to iterate through all paths and create graph with nodes in crossings (plus start/end) and edges with weight of longest corridor between crossings
        ForestGraph forestWithoutSlopesGraph = new ForestWithoutSlopesGraph();
        Grid<Integer> forestConnDegree = forestGrid.map((coords, symbol) -> symbol == '#' ? 0 : forestWithoutSlopesGraph.getEdges(new PathDirectedNode(coords.getValue0(), coords.getValue1(), Direction.UNKNOWN)).size(), Integer.valueOf(0).getClass());
        Set<PathNode> crossNodes = new HashSet<>();
        crossNodes.add(forestWithoutSlopesGraph.start);
        crossNodes.add(forestWithoutSlopesGraph.end);
        forestConnDegree.forEach((coords, degree) -> {
            if (degree > 2) {
                PathNode node = new PathNode(coords.getValue0(), coords.getValue1());
                crossNodes.add(node);
            }
        });
        Map<Pair<PathNode, PathNode>, Integer> crossNodeDistances = new HashMap<>();
        ForestDirectedGraph forestWithSlopesGraph = new ForestWithSlopesGraph();
        // Grid<Character> routeHitMap = forestGrid.getClone();
        RouteFinderDFS<PathDirectedNode> routeFinder = new RouteFinderDFS<>(forestWithSlopesGraph, new NextPathDirectedNodeScorer(), new TargetEstimatePathDirectedNodeScorer()) {
            @Override
            protected FoundRouteDecision foundRoute(List<PathDirectedNode> routeDirected, long score) {
                List<PathNode> route = routeDirected.stream().map(pdn -> new PathNode(pdn.col, pdn.row)).toList();
                int firstIdx = 0;
                PathNode firstNode = route.get(firstIdx);
                for (int secondIdx = firstIdx + 1; secondIdx < route.size(); secondIdx++) {
                    PathNode secondNode = route.get(secondIdx);
                    // routeHitMap.set(secondNode.col, secondNode.row, 'O');
                    if (!crossNodes.contains(secondNode)) {
                        continue;
                    }
                    boolean isOrdered = firstNode.getId() < secondNode.getId();
                    Pair<PathNode, PathNode> crossPair = new Pair<>(isOrdered ? firstNode : secondNode, isOrdered ? secondNode : firstNode);
                    int crossDistance = secondIdx - firstIdx;
                    crossNodeDistances.compute(crossPair, (key, previousDistance) -> previousDistance == null || previousDistance < crossDistance ? crossDistance : previousDistance);
                    firstIdx = secondIdx;
                    firstNode = secondNode;
                }
                return FoundRouteDecision.IGNORE; // doesn't matter
            }
        };
        routeFinder.findRoute(forestWithSlopesGraph.start, forestWithSlopesGraph.end);
        // System.out.println(routeHitMap);
        CrossingsGraph crossingsGraph = new CrossingsGraph(crossNodes, crossNodeDistances);
        // optimize search: exploit the knowledge that to end node leads only one path from last crossing
        Set<PathNode> endNodeEdges = crossingsGraph.getEdges(crossingsGraph.end);
        if (endNodeEdges.size() != 1) {
            throw new IllegalStateException("multiple ways to end point not supported");
        }
        PathNode penultimateToEndNode = endNodeEdges.stream().toList().get(0);
        final long[] maxScore = { Long.MIN_VALUE };
        RouteFinderDFS<PathNode> crossingsRouteFinder = new RouteFinderDFS<>(crossingsGraph, new NextCrossingsPathNodeScorer(crossingsGraph)) {
            @Override
            protected FoundRouteDecision foundRoute(List<PathNode> route, long score) {
                if (maxScore[0] < score) {
                    maxScore[0] = score;
                    // System.out.printf("found better route, score %d%n", score);
                    return FoundRouteDecision.REMEMBER;
                } else {
                    return FoundRouteDecision.IGNORE;
                }
            }
        };
        Pair<List<PathNode>, Long> routeWithScore = crossingsRouteFinder.findRoute(crossingsGraph.start, penultimateToEndNode);
        long result = routeWithScore.getValue1();
        // add length of stretch from last crossing to end node
        result += crossNodeDistances.get(new Pair<>(penultimateToEndNode, crossingsGraph.end));
        return result;
    }

    public static class Day23Test {
        @Test
        void solvePart1_sample() {
            var day = new Day23("_sample");
            day.parsePart1();
            assertEquals(94L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day23("");
            day.parsePart1();
            assertEquals(2282L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day23("_sample");
            day.parsePart2();
            assertEquals(154L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day23("");
            day.parsePart2();
            assertEquals(6646L, day.solvePart2());
        }
    }
    public static class Day23Test_Benchmark {
        @Test
        void test_main() {
            benchmark("", 6646L);
        }
        void benchmark(String inputSuffix, Object expectedResult) {
            Day.benchmark(5, expectedResult, inputSuffix,
                    List.of("dummy"),
                    (solverType) -> {
                        Day23 day17 = new Day23(inputSuffix);
                        day17.parse();
                        return day17;
                    },
                    (day, solverType) -> day.solvePart2(),
                    ""
            );
        }
    }
}
/*

--- Day 23: A Long Walk ---

The Elves resume water filtering operations! Clean water starts flowing over the edge of Island Island.

They offer to help you go over the edge of Island Island, too! Just hold on tight to one end of this impossibly long rope and they'll lower you down a safe distance from the massive waterfall you just created.

As you finally reach Snow Island, you see that the water isn't really reaching the ground: it's being absorbed by the air itself. It looks like you'll finally have a little downtime while the moisture builds up to snow-producing levels. Snow Island is pretty scenic, even without any snow; why not take a walk?

There's a map of nearby hiking trails (your puzzle input) that indicates paths (.), forest (#), and steep slopes (^, >, v, and <).

For example:

#.#####################
#.......#########...###
#######.#########.#.###
###.....#.>.>.###.#.###
###v#####.#v#.###.#.###
###.>...#.#.#.....#...#
###v###.#.#.#########.#
###...#.#.#.......#...#
#####.#.#.#######.#.###
#.....#.#.#.......#...#
#.#####.#.#.#########v#
#.#...#...#...###...>.#
#.#.#v#######v###.###v#
#...#.>.#...>.>.#.###.#
#####v#.#.###v#.#.###.#
#.....#...#...#.#.#...#
#.#########.###.#.#.###
#...###...#...#...#.###
###.###.#.###v#####v###
#...#...#.#.>.>.#.>.###
#.###.###.#.###.#.#v###
#.....###...###...#...#
#####################.#

You're currently on the single path tile in the top row; your goal is to reach the single path tile in the bottom row. Because of all the mist from the waterfall, the slopes are probably quite icy; if you step onto a slope tile, your next step must be downhill (in the direction the arrow is pointing). To make sure you have the most scenic hike possible, never step onto the same tile twice. What is the longest hike you can take?

In the example above, the longest hike you can take is marked with O, and your starting position is marked S:

#S#####################
#OOOOOOO#########...###
#######O#########.#.###
###OOOOO#OOO>.###.#.###
###O#####O#O#.###.#.###
###OOOOO#O#O#.....#...#
###v###O#O#O#########.#
###...#O#O#OOOOOOO#...#
#####.#O#O#######O#.###
#.....#O#O#OOOOOOO#...#
#.#####O#O#O#########v#
#.#...#OOO#OOO###OOOOO#
#.#.#v#######O###O###O#
#...#.>.#...>OOO#O###O#
#####v#.#.###v#O#O###O#
#.....#...#...#O#O#OOO#
#.#########.###O#O#O###
#...###...#...#OOO#O###
###.###.#.###v#####O###
#...#...#.#.>.>.#.>O###
#.###.###.#.###.#.#O###
#.....###...###...#OOO#
#####################O#

This hike contains 94 steps. (The other possible hikes you could have taken were 90, 86, 82, 82, and 74 steps long.)

Find the longest hike you can take through the hiking trails listed on your map. How many steps long is the longest hike?

Your puzzle answer was 2282.
--- Part Two ---

As you reach the trailhead, you realize that the ground isn't as slippery as you expected; you'll have no problem climbing up the steep slopes.

Now, treat all slopes as if they were normal paths (.). You still want to make sure you have the most scenic hike possible, so continue to ensure that you never step onto the same tile twice. What is the longest hike you can take?

In the example above, this increases the longest hike to 154 steps:

#S#####################
#OOOOOOO#########OOO###
#######O#########O#O###
###OOOOO#.>OOO###O#O###
###O#####.#O#O###O#O###
###O>...#.#O#OOOOO#OOO#
###O###.#.#O#########O#
###OOO#.#.#OOOOOOO#OOO#
#####O#.#.#######O#O###
#OOOOO#.#.#OOOOOOO#OOO#
#O#####.#.#O#########O#
#O#OOO#...#OOO###...>O#
#O#O#O#######O###.###O#
#OOO#O>.#...>O>.#.###O#
#####O#.#.###O#.#.###O#
#OOOOO#...#OOO#.#.#OOO#
#O#########O###.#.#O###
#OOO###OOO#OOO#...#O###
###O###O#O###O#####O###
#OOO#OOO#O#OOO>.#.>O###
#O###O###O#O###.#.#O###
#OOOOO###OOO###...#OOO#
#####################O#

Find the longest hike you can take through the surprisingly dry hiking trails listed on your map. How many steps long is the longest hike?

Your puzzle answer was 6646.

 */