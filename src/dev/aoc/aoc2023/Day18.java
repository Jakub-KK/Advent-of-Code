package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.Grid;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day18 extends Day {

    public Day18(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day18("_sample")); // _sample
    }

    private enum Direction {
        UNKNOWN(-1), DOWN(1), LEFT(2), UP(3), RIGHT(0);

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
        static Direction fromLetter(char letter) {
            return switch (letter) {
                case 'D' -> DOWN;
                case 'L' -> LEFT;
                case 'U' -> UP;
                case 'R' -> RIGHT;
                default -> UNKNOWN;
            };
        }
    }

    private record DigPlanItem(Direction dir, int length, String rgbStr) {}

    private List<DigPlanItem> digPlanList;

    private final List<Pair<Long, Long>> points = new ArrayList<>();

    private long sumEdgeLength = 0;

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        digPlanList = stream().map(s -> {
            String[] parts = s.split(" ");
            return new DigPlanItem(Direction.fromLetter(parts[0].charAt(0)), Integer.parseInt(parts[1]), parts[2]);
        }).toList();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        convertDigPlanToPoints();
        long area = getAreaFromPointsOfSimplePolygon();
        // formula discovery: https://old.reddit.com/r/adventofcode/comments/18l8mao/2023_day_18_intuition_for_why_spoiler_alone/kdwsdmp/
        // actual formula source: Pick's Theorem, explanation https://old.reddit.com/r/adventofcode/comments/18l8mao/2023_day_18_intuition_for_why_spoiler_alone/
        long result = sumEdgeLength + area - (sumEdgeLength / 2 - 1);
        return result;
    }

    /** Shoelace formula (see https://en.wikipedia.org/wiki/Shoelace_formula) */
    private long getAreaFromPointsOfSimplePolygon() {
        long doubleArea = 0;
        for (int pi = 0; pi < points.size() - 1; pi++) {
            Pair<Long, Long> pc = points.get(pi);
            Pair<Long, Long> pn = points.get(pi + 1);
            doubleArea += pc.getValue0() * pn.getValue1() - pc.getValue1() * pn.getValue0();
        }
        return doubleArea / 2;
    }

    private void convertDigPlanToPoints() {
        // long minCol = Long.MAX_VALUE, maxCol = Long.MIN_VALUE;
        // long minRow = Long.MAX_VALUE, maxRow = Long.MIN_VALUE;
        long currentCol = 0, currentRow = 0;
        points.add(new Pair<>(currentCol, currentRow));
        for (DigPlanItem item : digPlanList) {
            sumEdgeLength += item.length;
            long nextCol = currentCol, nextRow = currentRow;
            switch (item.dir) {
                case RIGHT: {
                    nextCol = currentCol + item.length;
                    break;
                }
                case LEFT: {
                    nextCol = currentCol - item.length;
                    break;
                }
                case DOWN: {
                    nextRow = currentRow + item.length;
                    break;
                }
                case UP: {
                    nextRow = currentRow - item.length;
                    break;
                }
            }
            points.add(new Pair<>(nextCol, nextRow));
            // if (minCol > nextCol) {
            //     minCol = nextCol;
            // } else if (maxCol < nextCol) {
            //     maxCol = nextCol;
            // }
            // if (minRow > nextRow) {
            //     minRow = nextRow;
            // } else if (maxRow < nextRow) {
            //     maxRow = nextRow;
            // }
            currentCol = nextCol;
            currentRow = nextRow;
        }
        // long width = maxCol - minCol + 1, height = maxRow - minRow + 1;
        if (!points.getFirst().equals(points.getLast())) {
            throw new IllegalArgumentException("dig plan invalid, not a loop");
        }
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        digPlanList = stream().map(s -> {
            String[] parts = s.split(" ");
            String lenStr = parts[2].substring(2, 7);
            String dirStr = parts[2].substring(7, 8);
            return new DigPlanItem(Direction.fromValue(Integer.parseInt(dirStr)), Integer.parseInt(lenStr, 16), parts[2]);
        }).toList();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        convertDigPlanToPoints();
        long area = getAreaFromPointsOfSimplePolygon();
        long result = sumEdgeLength + area - (sumEdgeLength / 2 - 1);
        return result;
    }

    private static class LavaLagoon extends Grid<LavaLagoon.Cell> {
        public LavaLagoon(int width, int height, LavaLagoon.Cell fillElement, String elementDelimiter) {
            super(width, height, fillElement, elementDelimiter);
        }

        public static class Cell {
            public final String rgbStr;

            public Cell(String rgbStr) {
                this.rgbStr = rgbStr;
            }

            public boolean isExterior() {
                return rgbStr == null;
            }

            public boolean isWall() {
                return rgbStr != null && !rgbStr.isEmpty();
            }

            public boolean isInterior() {
                return rgbStr != null && rgbStr.isEmpty();
            }

            @Override
            public String toString() {
                return rgbStr == null ? "." : "#";
            }
        }
    }
    private long solvePart1_Naive() {
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        int currentCol = 0, currentRow = 0;
        for (DigPlanItem item : digPlanList) {
            switch (item.dir) {
                case RIGHT: {
                    currentCol += item.length;
                    break;
                }
                case LEFT: {
                    currentCol -= item.length;
                    break;
                }
                case DOWN: {
                    currentRow += item.length;
                    break;
                }
                case UP: {
                    currentRow -= item.length;
                    break;
                }
            }
            if (minCol > currentCol) {
                minCol = currentCol;
            } else if (maxCol < currentCol) {
                maxCol = currentCol;
            }
            if (minRow > currentRow) {
                minRow = currentRow;
            } else if (maxRow < currentRow) {
                maxRow = currentRow;
            }
        }
        int width = maxCol - minCol + 1, height = maxRow - minRow + 1;
        LavaLagoon lavaLagoon = new LavaLagoon(width, height, new LavaLagoon.Cell(null), "");
        currentCol = currentRow = 0;
        int sumWalls = 0;
        for (DigPlanItem item : digPlanList) {
            switch (item.dir) {
                case RIGHT: {
                    for (int col = currentCol - minCol; col < currentCol - minCol + item.length; col++) {
                        lavaLagoon.set(col, currentRow - minRow, new LavaLagoon.Cell(item.rgbStr));
                    }
                    currentCol += item.length;
                    sumWalls += item.length;
                    break;
                }
                case LEFT: {
                    for (int col = currentCol - minCol; col >= currentCol - minCol - item.length; col--) {
                        lavaLagoon.set(col, currentRow - minRow, new LavaLagoon.Cell(item.rgbStr));
                    }
                    currentCol -= item.length;
                    sumWalls += item.length;
                    break;
                }
                case DOWN: {
                    for (int row = currentRow - minRow; row < currentRow - minRow + item.length; row++) {
                        lavaLagoon.set(currentCol - minCol, row, new LavaLagoon.Cell(item.rgbStr));
                    }
                    currentRow += item.length;
                    sumWalls += item.length;
                    break;
                }
                case UP: {
                    for (int row = currentRow - minRow; row >= currentRow - minRow - item.length; row--) {
                        lavaLagoon.set(currentCol - minCol, row, new LavaLagoon.Cell(item.rgbStr));
                    }
                    currentRow -= item.length;
                    sumWalls += item.length;
                    break;
                }
            }
        }
        System.out.printf("walls %d%n", sumWalls);
        int sumInterior = 0;
        // System.out.println(lavaLagoon);
        for (int row = 0; row < lavaLagoon.getHeight(); row++) {
            for (int col = 0; col < lavaLagoon.getWidth(); col++) {
                if (lavaLagoon.get(col, row).isWall()) {
                    continue;
                }
                int cr = row, cc = col;
                int edges = 0;
                while (cr > 0 && cc > 0) {
                    cr--;
                    cc--;
                    if (lavaLagoon.get(cc, cr).isWall()) { // wall hit
                        if (cc - 1 >= 0 && cc + 1 < lavaLagoon.getWidth() && lavaLagoon.get(cc - 1, cr).isWall() && lavaLagoon.get(cc + 1, cr).isWall() ||
                                cr - 1 >= 0 && cr + 1 < lavaLagoon.getHeight() && lavaLagoon.get(cc, cr - 1).isWall() && lavaLagoon.get(cc, cr + 1).isWall()) {
                            // straight wall
                            edges++;
                        } else if (cc - 1 >= 0 && cr - 1 >= 0 && lavaLagoon.get(cc - 1, cr).isWall() && lavaLagoon.get(cc, cr - 1).isWall() ||
                                cr + 1 < lavaLagoon.getHeight() && cc + 1 < lavaLagoon.getWidth() && lavaLagoon.get(cc, cr + 1).isWall() && lavaLagoon.get(cc + 1, cr).isWall()) {
                            // crossing corner
                            edges++;
                        }
                    }
                }
                boolean isInterior = (edges & 1) == 1;
                if (isInterior) {
                    sumInterior++;
                    lavaLagoon.set(col, row, new LavaLagoon.Cell("")); // change to interior
                }
                // System.out.println(lavaLagoon);
            }
        }
        System.out.printf("interior %d%n", sumInterior);
        System.out.printf("all %d%n", sumWalls + sumInterior);
        System.out.println(lavaLagoon);
        return sumWalls + sumInterior;
    }

    public static class Day18Test {
        @Test
        void knownGoodInputs() {

        }

        @Test
        void solvePart1_small() {
            var day = new Day18("_sample");
            day.parsePart1();
            assertEquals(62L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day18("");
            day.parsePart1();
            assertEquals(36807L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day18("_sample");
            day.parsePart2();
            assertEquals(952408144115L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day18("");
            day.parsePart2();
            assertEquals(48797603984357L, day.solvePart2());
        }
    }
}
/*

--- Day 18: Lavaduct Lagoon ---

Thanks to your efforts, the machine parts factory is one of the first factories up and running since the lavafall came back. However, to catch up with the large backlog of parts requests, the factory will also need a large supply of lava for a while; the Elves have already started creating a large lagoon nearby for this purpose.

However, they aren't sure the lagoon will be big enough; they've asked you to take a look at the dig plan (your puzzle input). For example:

R 6 (#70c710)
D 5 (#0dc571)
L 2 (#5713f0)
D 2 (#d2c081)
R 2 (#59c680)
D 2 (#411b91)
L 5 (#8ceee2)
U 2 (#caa173)
L 1 (#1b58a2)
U 2 (#caa171)
R 2 (#7807d2)
U 3 (#a77fa3)
L 2 (#015232)
U 2 (#7a21e3)

The digger starts in a 1 meter cube hole in the ground. They then dig the specified number of meters up (U), down (D), left (L), or right (R), clearing full 1 meter cubes as they go. The directions are given as seen from above, so if "up" were north, then "right" would be east, and so on. Each trench is also listed with the color that the edge of the trench should be painted as an RGB hexadecimal color code.

When viewed from above, the above example dig plan would result in the following loop of trench (#) having been dug out from otherwise ground-level terrain (.):

#######
#.....#
###...#
..#...#
..#...#
###.###
#...#..
##..###
.#....#
.######

At this point, the trench could contain 38 cubic meters of lava. However, this is just the edge of the lagoon; the next step is to dig out the interior so that it is one meter deep as well:

#######
#######
#######
..#####
..#####
#######
#####..
#######
.######
.######

Now, the lagoon can contain a much more respectable 62 cubic meters of lava. While the interior is dug out, the edges are also painted according to the color codes in the dig plan.

The Elves are concerned the lagoon won't be large enough; if they follow their dig plan, how many cubic meters of lava could it hold?

Your puzzle answer was 36807.

--- Part Two ---

The Elves were right to be concerned; the planned lagoon would be much too small.

After a few minutes, someone realizes what happened; someone swapped the color and instruction parameters when producing the dig plan. They don't have time to fix the bug; one of them asks if you can extract the correct instructions from the hexadecimal codes.

Each hexadecimal code is six hexadecimal digits long. The first five hexadecimal digits encode the distance in meters as a five-digit hexadecimal number. The last hexadecimal digit encodes the direction to dig: 0 means R, 1 means D, 2 means L, and 3 means U.

So, in the above example, the hexadecimal codes can be converted into the true instructions:

    #70c710 = R 461937
    #0dc571 = D 56407
    #5713f0 = R 356671
    #d2c081 = D 863240
    #59c680 = R 367720
    #411b91 = D 266681
    #8ceee2 = L 577262
    #caa173 = U 829975
    #1b58a2 = L 112010
    #caa171 = D 829975
    #7807d2 = L 491645
    #a77fa3 = U 686074
    #015232 = L 5411
    #7a21e3 = U 500254

Digging out this loop and its interior produces a lagoon that can hold an impressive 952408144115 cubic meters of lava.

Convert the hexadecimal color codes into the correct instructions; if the Elves follow this new dig plan, how many cubic meters of lava could the lagoon hold?

Your puzzle answer was 48797603984357.

 */