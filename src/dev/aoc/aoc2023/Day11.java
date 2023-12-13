package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class Day11 extends Day {
    public static void main(String[] args) {
        new Day11("_small").run(); // _small, _large1, _large2, _large3
    }

    public Day11(String inputSuffix) {
        super(inputSuffix);
        solverType = SolverType.FAST_1;
    }

    protected record Map(List<String> map, int width, int height) {
        public char getSymbol(int x, int y) {
            return map.get(y).charAt(x);
        }
    }

    protected Map map;

    protected final SolverType solverType;

    @Override
    protected void parsePart1() {
        // read map
        var mapStrings = stream().collect(Collectors.toList());
        int width = mapStrings.get(0).length();
        int height = mapStrings.size();
        map = new Map(mapStrings, width, height);
    }

    @Override
    protected Object solvePart1() {
        final long expansionFactor = 2;
        ISolver solver = solverType.getSolver();
        long result = solver.solve(map, expansionFactor);
        return result;
    }

    @Override
    protected void parsePart2() {
    }

    @Override
    protected Object solvePart2() {
        final long expansionFactor = 1000000;
        ISolver solver = solverType.getSolver();
        long result = solver.solve(map, expansionFactor);
        return result;
    }

    protected enum SolverType {
        NAIVE {
            @Override
            public ISolver getSolver() {
                return new SolverNaive();
            }
        },
        OPTIMIZED {
            @Override
            public ISolver getSolver() {
                return new SolverOptimized();
            }
        },
        FAST_1 {
            @Override
            public ISolver getSolver() {
                return new SolverFast1();
            }
        };

        public abstract ISolver getSolver();
    }

    protected interface ISolver {
        long solve(Map map, long expansionFactor);
    }

    protected static class SolverFast1 implements ISolver {
        public long solve(Map map, long expansionFactor) {
            // analyze map
            int[] projectionX = new int[map.width], boundsX = new int[]{Integer.MAX_VALUE, 0};
            int[] projectionY = new int[map.height], boundsY = new int[]{Integer.MAX_VALUE, 0};
            AtomicInteger count = new AtomicInteger(0);
            IntStream.range(0, map.height).forEach(y -> {
                String mapLine = map.map.get(y);
                IntStream.range(0, map.width).forEach(x -> {
                    char symbol = mapLine.charAt(x);
                    if (symbol == '#') {
                        count.incrementAndGet();
                        projectionX[x]++;
                        projectionY[y]++;
                        if (boundsX[1] < x) {
                            boundsX[1] = x;
                        } else if (boundsX[0] > x) {
                            boundsX[0] = x;
                        }
                    }
                });
                if (projectionY[y] > 0) {
                    boundsY[1] = y;
                    if (boundsY[0] > y) {
                        boundsY[0] = y;
                    }
                }
            });
            long sumX = calculateSumOfExpandedDistancesAlongAxis(expansionFactor, projectionX, boundsX[0], boundsX[1], count.get());
            long sumY = calculateSumOfExpandedDistancesAlongAxis(expansionFactor, projectionY, boundsY[0], boundsY[1], count.get());
            return sumX + sumY;
        }

        /**
         * Trick to calculate distances along an axis pairwise but in one sweep - O(n)
         * Conceptually moves galaxies to first position and accounts for differences in distance calculation later
         * see: http://clb.confined.space/aoc2023/#day11code
         */
        protected long calculateSumOfExpandedDistancesAlongAxis(long expansionFactor, int[] projection, int first, int last, int count) {
            long sum = 0; // result - sum of pairwise distances with expansion factor applied
            long cumDist = 1; // expanded distance from x=first to x=i
            long galsSum = projection[first]; // number of galaxies passed
            for (int i = first + 1; i <= last; i++, cumDist++) {
                if (projection[i] > 0) { // if some galaxies are in this position in the axis...
                    sum += (2 * galsSum - count + projection[i]) * projection[i] * cumDist; // calculate distance to all galaxies seen before, apply correction to account for accumulating all galaxies in first position
                    galsSum += projection[i]; // sum galaxies in first position
                } else {
                    cumDist += expansionFactor - 1; // apply expansion factor
                }
            }
            return sum;
        }
    }

    protected static class SolverOptimized implements ISolver {
        public long solve(Map map, long expansionFactor) {
            var mapAnalysis = analyzeMap(map);
            var axisDistances = calculateAxisDistances(expansionFactor, mapAnalysis);
            long result = calculateSumOfExpandedDistances(mapAnalysis.galaxies, axisDistances);
            return result;
        }

        protected record Galaxy(int x, int y) {}

        protected record MapAnalysis(boolean[] emptyRows, boolean[] emptyCols, List<Galaxy> galaxies) {}

        protected record AxisDistances(long[] distanceRows, long[] distanceCols) {}

        protected long calculateSumOfExpandedDistances(List<Galaxy> galaxies, AxisDistances axisDistances) {
            return galaxies.stream()
                    .limit(galaxies.size() - 1)
                    .flatMapToLong(g1 ->
                            galaxies.stream()
                                    .skip(galaxies.indexOf(g1) + 1)
                                    .mapToLong(g2 -> {
                                        long distanceCols = Math.abs(axisDistances.distanceCols[g2.x] - axisDistances.distanceCols[g1.x]);
                                        long distanceRows = Math.abs(axisDistances.distanceRows[g2.y] - axisDistances.distanceRows[g1.y]);
                                        long distance = distanceCols + distanceRows;
                                        // System.out.printf("(%d,%d) - (%d,%d) = %d%n", g1.x, g1.y, g2.x, g2.y, distance);
                                        return distance;
                                    })
                    )
                    .sum()
                    ;
        }

        protected MapAnalysis analyzeMap(Map map) {
            boolean[] emptyCols = new boolean[map.width];
            Arrays.fill(emptyCols, true);
            boolean[] emptyRows = new boolean[map.height];
            Arrays.fill(emptyRows, true);
            var galaxies = new ArrayList<Galaxy>();
            IntStream.range(0, map.height).forEach(y -> {
                String mapLine = map.map.get(y);
                IntStream.range(0, map.width).forEach(x -> {
                    char symbol = mapLine.charAt(x);
                    if (symbol == '#') {
                        emptyRows[y] = emptyCols[x] = false;
                        galaxies.add(new Galaxy(x, y));
                    }
                });
            });
            return new MapAnalysis(emptyRows, emptyCols, galaxies);
        }

        protected AxisDistances calculateAxisDistances(long expansionFactor, MapAnalysis mapAnalysis) {
            return new AxisDistances(
                    calculateAxisDistances(expansionFactor, mapAnalysis.emptyRows),
                    calculateAxisDistances(expansionFactor, mapAnalysis.emptyCols)
            );
        }

        /**
         * Returns array for calculating distances between axis points, takes expansion into account.
         */
        protected long[] calculateAxisDistances(long expansionFactor, boolean[] empty) {
            long[] distancesAccumulated = IntStream.range(0, empty.length)
                    .mapToLong(i -> empty[i] ? expansionFactor : 1)
                    .collect(
                            () -> new ArrayList<Long>(),
                            (ds, d) -> ds.add(d + (ds.isEmpty() ? 0 : ds.getLast())),
                            (ds1, ds2) -> {
                                throw new IllegalArgumentException("only sequential stream processing allowed");
                            }
                    )
                    .stream()
                    .mapToLong(d -> d)
                    .toArray();
            return distancesAccumulated;
        }
    }

    /** Slow implementation, wastes a lot of time traversing the map looking for galaxies and repeating expansion calculations, O(n^5) complexity where n is map side */
    protected static class SolverNaive implements ISolver {
        public long solve(Map map, long expansionFactor) {
            List<Boolean> emptyRows = IntStream.range(0, map.height).mapToObj(y -> map.map.get(y).replace('.', ' ').trim().isEmpty()).toList();
            List<Boolean> emptyCols = IntStream.range(0, map.width).mapToObj(x -> IntStream.range(0, map.height).mapToObj(y -> map.getSymbol(x, y) == '.').allMatch(b -> b)).toList();
            AtomicLong sumDistances = new AtomicLong(0);
            AtomicInteger pairs = new AtomicInteger(0);
            IntStream.range(0, map.height).forEach(y1 -> {
                IntStream.range(0, map.width).forEach(x1 -> {
                    if (map.getSymbol(x1, y1) == '#') {
                        IntStream.range(y1, map.height).forEach(y2 -> {
                            IntStream.range(y2 == y1 ? x1 + 1 : 0, map.width).forEach(x2 -> {
                                if (map.getSymbol(x2, y2) == '#') {
                                    long distance = Math.abs(x2 - x1) + Math.abs(y2 - y1);
                                    long expansionWidth = (expansionFactor - 1) * emptyCols.stream().skip(Math.min(x1 + 1, x2 + 1)).limit(Math.max(0, Math.abs(x2 - x1) - 1)).filter(b -> b).count();
                                    long expansionHeight = (expansionFactor - 1) * emptyRows.stream().skip(Math.min(y1 + 1, y2 + 1)).limit(Math.max(0, Math.abs(y2 - y1) - 1)).filter(b -> b).count();
                                    long expandedDistance = distance + expansionWidth + expansionHeight;
                                    // System.out.printf("%02d: (%d,%d) - (%d,%d) = %d (%d, %d)%n", pairs.get(), x1, y1, x2, y2, expandedDistance, expansionWidth, expansionHeight);
                                    pairs.incrementAndGet();
                                    sumDistances.addAndGet(expandedDistance);
                                }
                            });
                        });
                    }
                });
            });
            long result = sumDistances.get();
            return result;
        }
    }


    public static class Day11Test {
        SolverType[] solvers = new SolverType[] { SolverType.NAIVE, SolverType.OPTIMIZED, SolverType.FAST_1 };

        @Test
        void solvePart1_small() {
            var day = new Day11("_small");
            day.parsePart1();
            for (SolverType type : solvers) {
                assertEquals(374L, type.getSolver().solve(day.map, 2));
            }
        }

        @Test
        void solvePart1_main() {
            var day = new Day11("");
            day.parsePart1();
            for (SolverType type : solvers) {
                assertEquals(9418609L, type.getSolver().solve(day.map, 2));
            }
        }

        @Test
        void solvePart2_small() {
            var day = new Day11("_small");
            day.parsePart1();
            // day11.solvePart2(); // not needed in Day11
            day.parsePart2();
            assertAll(
                    () -> { for (SolverType type : solvers) assertEquals(1030L, type.getSolver().solve(day.map,10)); },
                    () -> { for (SolverType type : solvers) assertEquals(8410L, type.getSolver().solve(day.map,100)); }
            );
        }

        @Test
        void solvePart2_main() {
            var day = new Day11("");
            day.parsePart1();
            // day11.solvePart2(); // not needed in Day11
            day.parsePart2();
            for (SolverType type : solvers) {
                assertEquals(593821230983L, type.getSolver().solve(day.map, 1000000));
            }
        }
    }

    public static class Day11Test_LargeSlow {
        SolverType[] solvers = new SolverType[] { SolverType.OPTIMIZED, SolverType.FAST_1 };

        String testInputSuffix = "_large3";

        @Test
        void solvePart1_large_slow() {
            var day = new Day11(testInputSuffix);
            day.parsePart1();
            for (SolverType type : solvers) {
                assertEquals(35720904960L, type.getSolver().solve(day.map, 2));
            }
        }

        @Test
        void solvePart2_large_slow() {
            var day = new Day11(testInputSuffix);
            day.parsePart1();
            // day.solvePart2(); // not needed in Day11
            day.parsePart2();
            for (SolverType type : solvers) {
                assertEquals(11906980226968320L, type.getSolver().solve(day.map, 1000000));
            }
        }
    }
}
/*

--- Day 11: Cosmic Expansion ---

You continue following signs for "Hot Springs" and eventually come across an observatory. The Elf within turns out to be a researcher studying cosmic expansion using the giant telescope here.

He doesn't know anything about the missing machine parts; he's only visiting for this research project. However, he confirms that the hot springs are the next-closest area likely to have people; he'll even take you straight there once he's done with today's observation analysis.

Maybe you can help him with the analysis to speed things up?

The researcher has collected a bunch of data and compiled the data into a single giant image (your puzzle input). The image includes empty space (.) and galaxies (#). For example:

...#......
.......#..
#.........
..........
......#...
.#........
.........#
..........
.......#..
#...#.....

The researcher is trying to figure out the sum of the lengths of the shortest path between every pair of galaxies. However, there's a catch: the universe expanded in the time it took the light from those galaxies to reach the observatory.

Due to something involving gravitational effects, only some space expands. In fact, the result is that any rows or columns that contain no galaxies should all actually be twice as big.

In the above example, three columns and two rows contain no galaxies:

   v  v  v
 ...#......
 .......#..
 #.........
>..........<
 ......#...
 .#........
 .........#
>..........<
 .......#..
 #...#.....
   ^  ^  ^

These rows and columns need to be twice as big; the result of cosmic expansion therefore looks like this:

....#........
.........#...
#............
.............
.............
........#....
.#...........
............#
.............
.............
.........#...
#....#.......

Equipped with this expanded universe, the shortest path between every pair of galaxies can be found. It can help to assign every galaxy a unique number:

....1........
.........2...
3............
.............
.............
........4....
.5...........
............6
.............
.............
.........7...
8....9.......

In these 9 galaxies, there are 36 pairs. Only count each pair once; order within the pair doesn't matter. For each pair, find any shortest path between the two galaxies using only steps that move up, down, left, or right exactly one . or # at a time. (The shortest path between two galaxies is allowed to pass through another galaxy.)

For example, here is one of the shortest paths between galaxies 5 and 9:

....1........
.........2...
3............
.............
.............
........4....
.5...........
.##.........6
..##.........
...##........
....##...7...
8....9.......

This path has length 9 because it takes a minimum of nine steps to get from galaxy 5 to galaxy 9 (the eight locations marked # plus the step onto galaxy 9 itself). Here are some other example shortest path lengths:

    Between galaxy 1 and galaxy 7: 15
    Between galaxy 3 and galaxy 6: 17
    Between galaxy 8 and galaxy 9: 5

In this example, after expanding the universe, the sum of the shortest path between all 36 pairs of galaxies is 374.

Expand the universe, then find the length of the shortest path between every pair of galaxies. What is the sum of these lengths?

Your puzzle answer was 9418609.

--- Part Two ---

The galaxies are much older (and thus much farther apart) than the researcher initially estimated.

Now, instead of the expansion you did before, make each empty row or column one million times larger. That is, each empty row should be replaced with 1000000 empty rows, and each empty column should be replaced with 1000000 empty columns.

(In the example above, if each empty row or column were merely 10 times larger, the sum of the shortest paths between every pair of galaxies would be 1030. If each empty row or column were merely 100 times larger, the sum of the shortest paths between every pair of galaxies would be 8410. However, your universe will need to expand far beyond these values.)

Starting with the same initial image, expand the universe according to these new rules, then find the length of the shortest path between every pair of galaxies. What is the sum of these lengths?

Your puzzle answer was 593821230983.

 */