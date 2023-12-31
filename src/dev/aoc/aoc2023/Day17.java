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

        private static final Map<Integer, Position> positions = new HashMap<>();

        public static class Position {
            public final int col;
            public final int row;
            public final int id;

            private Position(int col, int row, int id) {
                this.col = col;
                this.row = row;
                this.id = id;
            }

            public int getId() {
                return id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Day17.CityGrid.Position node = (Day17.CityGrid.Position) o;
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
    }

    private CityGrid cityGrid;

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // the consensus on AoC reddit is to use Dijkstra's algorithm, for short and fast programs see https://old.reddit.com/r/adventofcode/comments/18k9ne5/2023_day_17_solutions/?sort=old
        // instead, for 3 days, the different solution was created...
        // probably dynamic programming, using brute-force and memoization
        // builds from the target node walking through grid diagonals towards start node
        Solver solver = new Solver(1, 3, cityGrid.getPosition(0, 0), cityGrid.getPosition(cityGrid.getWidth() - 1, cityGrid.getHeight() - 1));
        return solver.solve();
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        Solver solver = new Solver(4, 10, cityGrid.getPosition(0, 0), cityGrid.getPosition(cityGrid.getWidth() - 1, cityGrid.getHeight() - 1));
        return solver.solve();
    }

    private void parse() {
        var mapStrings = stream().collect(Collectors.toList());
        Function<String, Integer> parser = Integer::parseInt;
        if (this.getInputSuffix().contains("_blackhole_")) parser = CityGrid::parserBlackhole;
        cityGrid = new CityGrid(mapStrings, "", parser, Integer.valueOf(0).getClass());
        System.out.printf("city grid %d x %d, hash %d%n", cityGrid.getWidth(), cityGrid.getHeight(), cityGrid.hashCode());
        // System.out.println(cityGrid);
    }

    private class Solver {
        private final int MIN_STEPS;
        private final int MAX_STEPS;
        private final CityGrid.Position start;
        private final CityGrid.Position target;

        public Solver(int MIN_STEPS, int MAX_STEPS, CityGrid.Position start, CityGrid.Position target) {
            this.MIN_STEPS = MIN_STEPS;
            this.MAX_STEPS = MAX_STEPS;
            this.start = start;
            this.target = target;
        }

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
            return lastDir.dir + 4 * (lastStepsInTheSameDir - 1 + MAX_STEPS * nodeId);
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
                int nodeId = key / 4 / MAX_STEPS;
                Direction lastDir = Direction.fromValue(key % 4);
                int lastStepsInTheSameDir = (key / 4) % MAX_STEPS + 1;
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
                memos.sort(Comparator.comparingInt(n -> (n.getValue0().dir * MAX_STEPS + (n.getValue1() - 1)) * cityGrid.getUniqueIdMax() + n.getValue2().getId()));
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
                    memoKey = bestPathsForConstraintAndNodeMemoKey(lastDir, lastStepsInTheSameDir, current.getId());
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
                final int diagonalSum = firstCol + firstRow - MIN_STEPS; // -MIN_STEPS is needed to solve the puzzle, it allows path to go MIN_STEPS diagonal above currently memoized one, it won't work for long looping/winding paths
                List<CityGrid.Position> connections = new ArrayList<>(4);
                if (stepsCols == 0 && stepsRows == 0 || stepsCols >= MIN_STEPS || stepsRows >= 1) {
                    if (currCol > 0 && !pathMarkers.get(currCol - 1, currRow) && stepsRows < MAX_STEPS && diagonalSum <= currCol - 1 + currRow) {
                        connections.add(cityGrid.getPosition(currCol - 1, currRow));
                    }
                    if (currCol < cityGrid.getWidth() - 1 && !pathMarkers.get(currCol + 1, currRow) && stepsRows < MAX_STEPS && diagonalSum <= currCol + 1 + currRow) {
                        connections.add(cityGrid.getPosition(currCol + 1, currRow));
                    }
                }
                if (stepsCols == 0 && stepsRows == 0 || stepsRows >= MIN_STEPS || stepsCols >= 1) {
                    if (currRow > 0 && !pathMarkers.get(currCol, currRow - 1) && stepsCols < MAX_STEPS && diagonalSum <= currCol + currRow - 1) {
                        connections.add(cityGrid.getPosition(currCol, currRow - 1));
                    }
                    if (currRow < cityGrid.getHeight() - 1 && !pathMarkers.get(currCol, currRow + 1) && stepsCols < MAX_STEPS && diagonalSum <= currCol + currRow + 1) {
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
                            if ((nextStepsCols == 0 && nextStepsRows >= MIN_STEPS) ||
                                    (nextStepsRows == 0 && nextStepsCols >= MIN_STEPS)) {
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
        
        public long solve() {
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
                if (true || path == winnerPath) {
                    System.out.printf("#%d: %d %s%n", counter.get(), score, String.join(",", path.stream().map(CityGrid.Position::toString).toList()));
                    System.out.printf("path hash %d%n", Arrays.hashCode(path.toArray(new CityGrid.Position[0])));
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
                                int memoKey = bestPathsForConstraintAndNodeMemoKey(lastDir, Math.max(lastStepsCols, lastStepsRows), current.getId());
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
                        debugShowPath("path reconstructed:", winnerPath);
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
            pathfinder.memoStatsReport();
            System.out.print("winner ");
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
        @Test
        void solvePart1_small() {
            var day = new Day17("_sample");
            day.parsePart1();
            assertEquals(102L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day17("");
            day.parsePart1();
            assertEquals(851L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day17("_sample");
            day.parsePart2();
            assertEquals(94L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day17("");
            day.parsePart2();
            assertEquals(982L, day.solvePart2());
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