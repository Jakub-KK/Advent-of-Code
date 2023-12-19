package dev.aoc.aoc2023;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day14 extends Day {
    public static void main(String[] args) {
        // _longcycle_2520 (source: https://old.reddit.com/r/adventofcode/comments/18i45eo/2023_day_14_part_2_worst_case_complexity/)
        // _longcycle_5 (cycle of length 5 extracted from _longcycle_2520)
        // _longcycle_85085_5x7x11x13x17 (based on _longcycle_2520, modified for compactness)
        // _longcycle_85085_5x7x11x13x17_filled (_longcycle_85085_5x7x11x13x17 filled with square stones '#' to speed up processing)
        // _longcycle_870870_2x3x5x7x11x13x29 (source: https://old.reddit.com/r/adventofcode/comments/18i45eo/2023_day_14_part_2_worst_case_complexity/kdfxrus/)
        // _longcycle_13082761331670030_43x41x37x31x29x23x19x17x13x11x7x5x3x2 (source: https://old.reddit.com/r/adventofcode/comments/18it12w/2023_day_14_part_2_custom_worst_case_testcase/)
        Day.run(() -> new Day14("")); // _sample
    }

    public Day14(String inputSuffix) {
        super(inputSuffix);
    }

    private static final HashFunction hashSHA256 = Hashing.sha256();

    private static final class StoneMap { // TODO extends Grid
        private final char[][] symbols;
        private final int width;
        private final int height;

        /** Array of ranges of non-square stones in the rows on the map (length is 2 * count of ranges). Speeds up round stones rolling. */
        private final int[][] nonSquareRangesForRows;
        /** Array of ranges of non-square stones in the columns on the map (length is 2 * count of ranges). Speeds up round stones rolling. */
        private final int[][] nonSquareRangesForCols;

        public StoneMap(List<String> lines) {
            this(
                    toArray(verifyEqualLengths(lines)),
                    lines.getFirst().length(),
                    lines.size(),
                    getSquareRangesForRows(toArray(lines), lines.getFirst().length(), lines.size()), // recalculated arguments (to keep "final"), only during parsing, not during rolling and usage
                    getSquareRangesForCols(toArray(lines), lines.getFirst().length(), lines.size())
            );
        }
        private static List<String> verifyEqualLengths(List<String> lines) {
            int height = lines.size();
            int width = lines.getFirst().length();
            IntStream.range(1, height).forEach(i -> {
                if (lines.get(i).length() != width) {
                    throw new IllegalArgumentException("line length mismatch");
                }
            });
            return lines;
        }

        private StoneMap(StoneMap that) {
            this(clone(that.symbols), that.width, that.height, that.nonSquareRangesForRows, that.nonSquareRangesForCols);
        }

        private StoneMap(char[][] symbols, int width, int height, int[][] nonSquareRangesForRows, int[][] nonSquareRangesForCols) {
            this.symbols = symbols;
            this.width = width;
            this.height = height;
            this.nonSquareRangesForRows = nonSquareRangesForRows;
            this.nonSquareRangesForCols = nonSquareRangesForCols;
        }
        private static int[][] getSquareRangesForRows(char[][] symbols, int width, int height) {
            return IntStream.range(0, height)
                    .mapToObj(row -> {
                        char[] symbolRow = symbols[row];
                        return getSquareRanges(
                                i -> symbolRow[i],
                                i -> i < width,
                                () -> 0,
                                i -> i + 1
                        );
                    })
                    .toArray(int[][]::new)
                    ;
        }
        private static int[][] getSquareRangesForCols(char[][] symbols, int width, int height) {
            return IntStream.range(0, width)
                    .mapToObj(col -> {
                        return getSquareRanges(
                                i -> symbols[i][col],
                                i -> i < height,
                                () -> 0,
                                i -> i + 1
                        );
                    })
                    .toArray(int[][]::new)
            ;
        }
        private static int[] getSquareRanges(
                Function<Integer, Character> symbolAt,
                Predicate<Integer> isNotEnd,
                Supplier<Integer> getStart,
                Function<Integer, Integer> advance
        ) {
            var ranges = new ArrayList<Integer>();
            int prevNonSquare = getStart.get();
            while (isNotEnd.test(prevNonSquare)) {
                while (isNotEnd.test(prevNonSquare) && symbolAt.apply(prevNonSquare) == '#') {
                    prevNonSquare = advance.apply(prevNonSquare);
                }
                if (isNotEnd.test(prevNonSquare)) {
                    int nextSquare = prevNonSquare;
                    while (isNotEnd.test(nextSquare) && symbolAt.apply(nextSquare) != '#') {
                        nextSquare = advance.apply(nextSquare);
                    }
                    ranges.add(prevNonSquare);
                    ranges.add(nextSquare);
                    prevNonSquare = nextSquare;
                }
            }
            return ranges.stream().mapToInt(i -> i).toArray();
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }

        public long calculateLoadNorth() {
            long load = 0;
            for (int row = 0; row < height; row++) {
                char[] rowLine = symbols[row];
                int countRound = 0;
                for (int col = 0; col < width; col++) {
                    if (rowLine[col] == 'O') {
                        countRound++;
                    }
                }
                load += (long)countRound * (height - row);
            }
            return load;
        }

        public long calculateLoadNorthAfter(long cycles) {
            // Map<String, Integer> memoryHashCodeToStep = new HashMap<>();
            Map<StoneMap, Integer> memoryHashCodeToStep = new HashMap<>();
            Map<Integer, Long> memoryStepToLoadNorth = new HashMap<>();
            // List<char[][]> steps = new ArrayList<>();
            int step = 0;
            StoneMap currentMap = this;
            // System.out.printf("step %d (load %d)%n%s%n---%n", step, calculateLoadNorth(), currentMap);
            while (!memoryHashCodeToStep.containsKey(currentMap)) {
                memoryHashCodeToStep.put(currentMap, step);
                memoryStepToLoadNorth.put(step, currentMap.calculateLoadNorth());
                // steps.add(currentMap);
                currentMap = new StoneMap(currentMap).rollFullCycle();
                step++;
                // System.out.printf("step %d (load %d)%n%s%n---%n", step, calculateLoadNorth(), currentMap);
            }
            int cycleCountBeforeLoopStart = memoryHashCodeToStep.get(currentMap);
            int cycleCountOfLoop = step - cycleCountBeforeLoopStart;
            System.out.printf("loop detected, intro %d, length %d%n", cycleCountBeforeLoopStart, cycleCountOfLoop);
            int stepAfterCycles = cycleCountBeforeLoopStart + (int)((cycles - cycleCountBeforeLoopStart) % cycleCountOfLoop);
            return memoryStepToLoadNorth.get(stepAfterCycles);
        }

        /** Returns this for chaining. */
        public StoneMap rollFullCycle() {
            rollNorth();
            // System.out.printf("cycle N%n%s%n-%n", this);
            rollWest();
            // System.out.printf("cycle W%n%s%n-%n", this);
            rollSouth();
            // System.out.printf("cycle S%n%s%n-%n", this);
            rollEast();
            // System.out.printf("cycle E%n%s%n-%n", this);
            return this;
        }
        /** Returns this for chaining. */
        private StoneMap rollNorth() {
            IntStream.range(0, width).forEach(x -> {
                roll(
                        i -> symbols[i][x],
                        (i, s) -> symbols[i][x] = s,
                        c -> 2 * c < nonSquareRangesForCols[x].length,
                        c -> nonSquareRangesForCols[x][2 * c],
                        c -> nonSquareRangesForCols[x][2 * c + 1]
                );
                // rollNonOptimized(
                //         i -> symbols[i][x],
                //         (i, s) -> symbols[i][x] = s,
                //         i -> i < height,
                //         () -> 0,
                //         i -> i + 1
                // );
            });
            return this;
        }
        /** Returns this for chaining. */
        private StoneMap rollSouth() {
            IntStream.range(0, width).forEach(x -> {
                roll(
                        i -> symbols[i][x],
                        (i, s) -> symbols[i][x] = s,
                        c -> 2 * c < nonSquareRangesForCols[x].length,
                        c -> nonSquareRangesForCols[x][2 * c + 1] - 1,
                        c -> nonSquareRangesForCols[x][2 * c] - 1
                );
                // rollNonOptimized(
                //         i -> symbols[i][x],
                //         (i, s) -> symbols[i][x] = s,
                //         i -> i >= 0,
                //         () -> height - 1,
                //         i -> i - 1
                // );
            });
            return this;
        }
        /** Returns this for chaining. */
        private StoneMap rollWest() {
            IntStream.range(0, height).forEach(y -> {
                char[] symbolRow = symbols[y];
                roll(
                        i -> symbolRow[i],
                        (i, s) -> symbolRow[i] = s,
                        c -> 2 * c < nonSquareRangesForRows[y].length,
                        c -> nonSquareRangesForRows[y][2 * c],
                        c -> nonSquareRangesForRows[y][2 * c + 1]
                );
                // rollNonOptimized(
                //         i -> symbolRow[i],
                //         (i, s) -> symbolRow[i] = s,
                //         i -> i < width,
                //         () -> 0,
                //         i -> i + 1
                // );
            });
            return this;
        }
        /** Returns this for chaining. */
        private StoneMap rollEast() {
            IntStream.range(0, height).forEach(y -> {
                char[] symbolRow = symbols[y];
                roll(
                        i -> symbolRow[i],
                        (i, s) -> symbolRow[i] = s,
                        c -> 2 * c < nonSquareRangesForRows[y].length,
                        c -> nonSquareRangesForRows[y][2 * c + 1] - 1,
                        c -> nonSquareRangesForRows[y][2 * c] - 1
                );
                // rollNonOptimized(
                //         i -> symbolRow[i],
                //         (i, s) -> symbolRow[i] = s,
                //         i -> i >= 0,
                //         () -> width - 1,
                //         i -> i - 1
                // );
            });
            return this;
        }

        /** Roll while always searching symbols for square (immovable) stones */
        private void rollNonOptimized(
                Function<Integer, Character> symbolAt,
                BiConsumer<Integer, Character> setSymbolAt,
                Predicate<Integer> isNotEnd,
                Supplier<Integer> getStart,
                Function<Integer, Integer> advance
        ) {
            int prevNonSquare = getStart.get();
            while (isNotEnd.test(prevNonSquare)) {
                while (isNotEnd.test(prevNonSquare) && symbolAt.apply(prevNonSquare) == '#') {
                    prevNonSquare = advance.apply(prevNonSquare);
                }
                if (isNotEnd.test(prevNonSquare)) {
                    int nextSquare = prevNonSquare;
                    int countRound = 0;
                    while (isNotEnd.test(nextSquare) && symbolAt.apply(nextSquare) != '#') {
                        if (symbolAt.apply(nextSquare) == 'O') {
                            countRound++;
                        }
                        nextSquare = advance.apply(nextSquare);
                    }
                    if (countRound > 0) {
                        // .O.O# -> OO..#
                        if (prevNonSquare < nextSquare) {
                            IntStream.range(prevNonSquare, prevNonSquare + countRound).forEach(i -> setSymbolAt.accept(i, 'O'));
                            IntStream.range(prevNonSquare + countRound, nextSquare).forEach(i -> setSymbolAt.accept(i, '.'));
                        } else {
                            IntStream.range(nextSquare + 1, prevNonSquare - countRound + 1).forEach(i -> setSymbolAt.accept(i, '.'));
                            IntStream.range(prevNonSquare - countRound + 1, prevNonSquare + 1).forEach(i -> setSymbolAt.accept(i, 'O'));
                        }
                    }
                    prevNonSquare = nextSquare;
                }
            }
        }
        /** Roll using precalculated arrays with indices of non-immovable ranges.  */
        private void roll(
                Function<Integer, Character> symbolAt,
                BiConsumer<Integer, Character> setSymbolAt,
                Predicate<Integer> isNotEnd,
                Function<Integer, Integer> getNextNonSquare,
                Function<Integer, Integer> getNextSquare
        ) {
            int rangeCounter = 0;
            while (isNotEnd.test(rangeCounter)) {
                int prevNonSquare = getNextNonSquare.apply(rangeCounter);
                int nextSquare = getNextSquare.apply(rangeCounter);
                int countRound = 0;
                if (prevNonSquare < nextSquare) { // do not optimize by using ternary operator - slows down
                    for (int pos = prevNonSquare; pos < nextSquare; pos++) {
                        if (symbolAt.apply(pos) == 'O') {
                            countRound++;
                        }
                    }
                } else {
                    for (int pos = nextSquare + 1; pos < prevNonSquare + 1; pos++) {
                        if (symbolAt.apply(pos) == 'O') {
                            countRound++;
                        }
                    }
                }
                if (countRound > 0) {
                    if (prevNonSquare < nextSquare) { // do not optimize by using ternary operator - slows down
                        IntStream.range(prevNonSquare, prevNonSquare + countRound).forEach(i -> setSymbolAt.accept(i, 'O'));
                        IntStream.range(prevNonSquare + countRound, nextSquare).forEach(i -> setSymbolAt.accept(i, '.'));
                    } else {
                        IntStream.range(nextSquare + 1, prevNonSquare - countRound + 1).forEach(i -> setSymbolAt.accept(i, '.'));
                        IntStream.range(prevNonSquare - countRound + 1, prevNonSquare + 1).forEach(i -> setSymbolAt.accept(i, 'O'));
                    }
                }
                rangeCounter++;
            }
        }

        private static char[][] toArray(List<String> lines) {
            char[][] result = new char[lines.size()][];
            IntStream.range(0, result.length).forEach(row -> {
                result[row] = new char[lines.getFirst().length()];
                String rowLine = lines.get(row);
                IntStream.range(0, result[row].length).forEach(col -> {
                    result[row][col] = rowLine.charAt(col);
                });
            });
            return result;
        }

        private List<String> toLines(char[][] mapArray) {
            return Arrays.stream(mapArray).map(chs -> CharBuffer.wrap(chs).toString()).toList();
        }

        private static char[][] clone(char[][] arr) {
            char[][] result = new char[arr.length][];
            IntStream.range(0, arr.length).forEach(i -> {
                result[i] = Arrays.copyOf(arr[i], arr[i].length);
            });
            return result;
        }

        public StoneMap transpose() {
            char[][] tmp = new char[width][];
            IntStream.range(0, width).forEach(row -> {
                tmp[row] = new char[height];
                IntStream.range(0, height).forEach(col -> {
                    tmp[row][col] = symbols[col][row];
                });
            });
            return new StoneMap(tmp, height, width, nonSquareRangesForRows, nonSquareRangesForCols);
        }

        public StoneMap rotateCW() {
            char[][] tmp = new char[width][];
            IntStream.range(0, width).forEach(row -> {
                tmp[row] = new char[height];
                IntStream.range(0, height).forEach(col -> {
                    tmp[row][col] = symbols[height - 1 - col][row];
                });
            });
            return new StoneMap(tmp, height, width, nonSquareRangesForRows, nonSquareRangesForCols);
        }

        @Override
        public String toString() {
            return String.join("%n".formatted(), toLines(symbols));
        }

        public String hashCodeString() {
            Hasher hasher = hashSHA256.newHasher();
            IntStream.range(0, height).forEach(row -> {
                hasher.putUnencodedChars(CharBuffer.wrap(symbols[row]));
            });
            HashCode hash = hasher.hash();
            return hash.toString();
            // return Arrays.hashCode(Arrays.stream(symbols).mapToInt(Arrays::hashCode).toArray()); // collides too often
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(Arrays.stream(symbols).mapToInt(Arrays::hashCode).toArray());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StoneMap stoneMap = (StoneMap)o;
            return Arrays.deepEquals(symbols, stoneMap.symbols);
        }
    }

    private StoneMap stoneMap;

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        // read map
        var mapStrings = stream().collect(Collectors.toList());
        stoneMap = new StoneMap(mapStrings);
        // System.out.println(stoneMap);
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        long result = stoneMap.rollNorth().calculateLoadNorth();
        // System.out.println(stoneMap);
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        // read map
        var mapStrings = stream().collect(Collectors.toList());
        stoneMap = new StoneMap(mapStrings);
        // System.out.println(stoneMap);
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        // if (true) return 0;
        long result = stoneMap.calculateLoadNorthAfter(1_000_000_000);
        return result;
    }

    public static class Day14Test {
        @Test
        void knownGoodInputs() {
            assertEquals(3487, new StoneMap(List.of(".O")).rollWest().hashCode());
        }

        @Test
        void solvePart1_sample() {
            var day = new Day14("_sample");
            day.parsePart1();
            assertEquals(136L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day14("");
            day.parsePart1();
            assertEquals(109638L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day14("_sample");
            day.parsePart1();
            day.parsePart2();
            assertEquals(64L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day14("");
            day.parsePart1();
            day.parsePart2();
            assertEquals(102657L, day.solvePart2());
        }
    }
}
/*

--- Day 14: Parabolic Reflector Dish ---

You reach the place where all of the mirrors were pointing: a massive parabolic reflector dish attached to the side of another large mountain.

The dish is made up of many small mirrors, but while the mirrors themselves are roughly in the shape of a parabolic reflector dish, each individual mirror seems to be pointing in slightly the wrong direction. If the dish is meant to focus light, all it's doing right now is sending it in a vague direction.

This system must be what provides the energy for the lava! If you focus the reflector dish, maybe you can go where it's pointing and use the light to fix the lava production.

Upon closer inspection, the individual mirrors each appear to be connected via an elaborate system of ropes and pulleys to a large metal platform below the dish. The platform is covered in large rocks of various shapes. Depending on their position, the weight of the rocks deforms the platform, and the shape of the platform controls which ropes move and ultimately the focus of the dish.

In short: if you move the rocks, you can focus the dish. The platform even has a control panel on the side that lets you tilt it in one of four directions! The rounded rocks (O) will roll when the platform is tilted, while the cube-shaped rocks (#) will stay in place. You note the positions of all of the empty spaces (.) and rocks (your puzzle input). For example:

O....#....
O.OO#....#
.....##...
OO.#O....O
.O.....O#.
O.#..O.#.#
..O..#O..O
.......O..
#....###..
#OO..#....

Start by tilting the lever so all of the rocks will slide north as far as they will go:

OOOO.#.O..
OO..#....#
OO..O##..O
O..#.OO...
........#.
..#....#.#
..O..#.O.O
..O.......
#....###..
#....#....

You notice that the support beams along the north side of the platform are damaged; to ensure the platform doesn't collapse, you should calculate the total load on the north support beams.

The amount of load caused by a single rounded rock (O) is equal to the number of rows from the rock to the south edge of the platform, including the row the rock is on. (Cube-shaped rocks (#) don't contribute to load.) So, the amount of load caused by each rock in each row is as follows:

OOOO.#.O.. 10
OO..#....#  9
OO..O##..O  8
O..#.OO...  7
........#.  6
..#....#.#  5
..O..#.O.O  4
..O.......  3
#....###..  2
#....#....  1

The total load is the sum of the load caused by all of the rounded rocks. In this example, the total load is 136.

Tilt the platform so that the rounded rocks all roll north. Afterward, what is the total load on the north support beams?

Your puzzle answer was 109638.

--- Part Two ---

The parabolic reflector dish deforms, but not in a way that focuses the beam. To do that, you'll need to move the rocks to the edges of the platform. Fortunately, a button on the side of the control panel labeled "spin cycle" attempts to do just that!

Each cycle tilts the platform four times so that the rounded rocks roll north, then west, then south, then east. After each tilt, the rounded rocks roll as far as they can before the platform tilts in the next direction. After one cycle, the platform will have finished rolling the rounded rocks in those four directions in that order.

Here's what happens in the example above after each of the first few cycles:

After 1 cycle:
.....#....
....#...O#
...OO##...
.OO#......
.....OOO#.
.O#...O#.#
....O#....
......OOOO
#...O###..
#..OO#....

After 2 cycles:
.....#....
....#...O#
.....##...
..O#......
.....OOO#.
.O#...O#.#
....O#...O
.......OOO
#..OO###..
#.OOO#...O

After 3 cycles:
.....#....
....#...O#
.....##...
..O#......
.....OOO#.
.O#...O#.#
....O#...O
.......OOO
#...O###.O
#.OOO#...O

This process should work if you leave it running long enough, but you're still worried about the north support beams. To make sure they'll survive for a while, you need to calculate the total load on the north support beams after 1000000000 cycles.

In the above example, after 1000000000 cycles, the total load on the north support beams is 64.

Run the spin cycle for 1000000000 cycles. Afterward, what is the total load on the north support beams?

Your puzzle answer was 102657.

 */