package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.GridOfChars;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day16 extends Day {
    public Day16(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day16("")); // _sample, _simple1, _large1
    }

    private GridOfChars mirrorGrid; // '.' empty, '|' and '-' splitter, '/' and '\' mirrors

    public enum Direction {
        UP(0, -1), NORTH(0, -1),
        DOWN(0, 1), SOUTH(0, 1),
        LEFT(-1, 0), WEST(-1, 0),
        RIGHT(1, 0), EAST(1, 0);
        public final int dX;
        public final int dY;
        Direction(int dX, int dY) {
            this.dX = dX;
            this.dY = dY;
        }
    }

    public record BeamStart(int posX, int posY, Direction dir) {}

    /** Light beam path, '|' and '-' light direction, '+' light crossing, '*' mirrors */
    private static class LightGrid extends GridOfChars {
        private final GridOfChars mirrorGrid;

        public LightGrid(GridOfChars mirrorGrid) {
            super(mirrorGrid.getWidth(), mirrorGrid.getHeight(), '.');
            this.mirrorGrid = mirrorGrid;
        }

        // int maxLightHeadsCount = 0;

        public int countEnergized(BeamStart start) {
            if (count(c -> c == '.') != getWidth() * getHeight()) { // if reused then clear light grid
                fill('.');
            }
            List<BeamStart> lightHeads = new LinkedList<>(); // light beams starting positions and directions
            lightHeads.add(start);
            while (!lightHeads.isEmpty()) {
                // System.out.println(lightGrid);
                // System.out.println();
                // if (maxLightHeadsCount < lightHeads.size()) {
                //     maxLightHeadsCount = lightHeads.size();
                //     System.out.printf("max light heads %d%n", maxLightHeadsCount);
                // }
                BeamStart head = lightHeads.removeFirst();
                // propagate: find a position where the light beam hits a symbol or exits grid
                int posX = head.posX, posY = head.posY;
                int dX = head.dir.dX, dY = head.dir.dY;
                while (mirrorGrid.hasColumn(posX) && mirrorGrid.hasRow(posY) && (
                        (mirrorGrid.is(posX, posY,'.') ||
                                (dX == 0 && mirrorGrid.is(posX, posY,'|')) || // pass through splitter parallel to light beam
                                (dY == 0 && mirrorGrid.is(posX, posY,'-'))
                        )
                )) {
                    // use light grid to remember beam positions and direction
                    markBeamPath(posX, posY, dX != 0);
                    posX += dX;
                    posY += dY;
                }
                // if beam ended outside of grid, forget about it
                if (!mirrorGrid.hasColumn(posX) || !mirrorGrid.hasRow(posY)) {
                    continue;
                }
                // remember...
                markBeamPath(posX, posY, dX != 0);
                // beam hit an object which splits or mirrors, spawn new beams depending on object and beam direction
                // do not reenter positions that were visited before if the same beam direction (taking crossing into account)
                // always reenter mirrors (entering from the other side special case)
                char objectSymbol = mirrorGrid.get(posX, posY);
                if ((objectSymbol == '|' || (objectSymbol == '/' && dX > 0) || (objectSymbol == '\\' && dX < 0)) && posY > 0 && this.isInSet(posX, posY - 1, ".*-")) {
                    lightHeads.add(new BeamStart(posX, posY - 1, Direction.UP)); // add light beam traveling up
                }
                if ((objectSymbol == '|' || (objectSymbol == '/' && dX < 0) || (objectSymbol == '\\' && dX > 0)) && posY < mirrorGrid.getHeight() - 1 && this.isInSet(posX, posY + 1, ".*-")) {
                    lightHeads.add(new BeamStart(posX, posY + 1, Direction.DOWN)); // add light beam traveling down
                }
                if ((objectSymbol == '-' || (objectSymbol == '/' && dY > 0) || (objectSymbol == '\\' && dY < 0)) && posX > 0 && this.isInSet(posX - 1, posY, ".*|")) {
                    lightHeads.add(new BeamStart(posX - 1, posY, Direction.LEFT)); // add light beam traveling left
                }
                if ((objectSymbol == '-' || (objectSymbol == '/' && dY < 0) || (objectSymbol == '\\' && dY > 0)) && posX < mirrorGrid.getWidth() - 1 && this.isInSet(posX + 1, posY, ".*|")) {
                    lightHeads.add(new BeamStart(posX + 1, posY, Direction.RIGHT)); // add light beam traveling right
                }
            }
            return this.count(c -> c != '.');
        }
        private void markBeamPath(int posX, int posY, boolean isBeamHorizontal) {
            if (mirrorGrid.isInSet(posX, posY, "/\\")) {
                this.set(posX, posY, '*'); // special mark for mirrors to allow reentry (from the other side)
            } else if (this.isInSet(posX, posY, "|-") && this.isNot(posX, posY, isBeamHorizontal ? '-' : '|')) {
                this.set(posX, posY, '+'); // mark light crossings
            } else {
                this.set(posX, posY, isBeamHorizontal ? '-' : '|');
            }
        }

    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        // createTest("_large1", 500, 0.105);
        parse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        LightGrid lightGrid = new LightGrid(mirrorGrid);
        long result = lightGrid.countEnergized(new BeamStart(0, 0, Direction.RIGHT));
        // System.out.println(lightGrid);
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        LightGrid lightGrid = new LightGrid(mirrorGrid);
        long[] results = new long[4];
        results[0] = IntStream.range(0, mirrorGrid.getWidth()).mapToLong(col -> lightGrid.countEnergized(new BeamStart(col, 0, Direction.DOWN))).max().getAsLong();
        results[1] = IntStream.range(0, mirrorGrid.getWidth()).mapToLong(col -> lightGrid.countEnergized(new BeamStart(col, mirrorGrid.getHeight() - 1, Direction.UP))).max().getAsLong();
        results[2] = IntStream.range(0, mirrorGrid.getHeight()).mapToLong(row -> lightGrid.countEnergized(new BeamStart(0, row, Direction.RIGHT))).max().getAsLong();
        results[3] = IntStream.range(0, mirrorGrid.getHeight()).mapToLong(row -> lightGrid.countEnergized(new BeamStart(mirrorGrid.getHeight() - 1, row, Direction.LEFT))).max().getAsLong();
        long result = Arrays.stream(results).max().getAsLong();
        return result;
    }

    private void parse() {
        var mapStrings = stream().collect(Collectors.toList());
        mirrorGrid = new GridOfChars(mapStrings);
        int countEmpty = mirrorGrid.count(c -> c == '.');
        int countSplitterVertical = mirrorGrid.count(c -> c == '|');
        int countSplitterHorizontal = mirrorGrid.count(c -> c == '-');
        int countMirror1 = mirrorGrid.count(c -> c == '/');
        int countMirror2 = mirrorGrid.count(c -> c == '\\');
        int all = mirrorGrid.getWidth() * mirrorGrid.getHeight();
        System.out.printf("mirror grid %d x %d: %.2f%% empty, %.2f%% mirrors (%.2f%% /, %.2f%% \\), %.2f%% splitters (%.2f%% |, %.2f%% -) %n", mirrorGrid.getWidth(), mirrorGrid.getHeight(), 100.0*countEmpty/all, 100.0*(countMirror1+countMirror2)/all, 100.0*countMirror1/all, 100.0*countMirror2/all, 100.0*(countSplitterHorizontal+countSplitterVertical)/all, 100.0*countSplitterHorizontal/all, 100.0*countSplitterVertical/all);
        // System.out.println(mirrorGrid);
    }

    private void createTest(String testSuffix, int side, double probObject) {
        createTestFile(testSuffix, writer -> {
            var rng = new Random();
            GridOfChars test = new GridOfChars(side, side, '.');
            String objects = "/\\|-";
            test.map((row, col) -> {
                if (rng.nextDouble() < probObject) {
                    return objects.charAt(rng.nextInt(objects.length()));
                } else {
                    return '.';
                }
            });
            try {
                writer.append(test.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static class Day16Test {
        @Test
        void solvePart1_small() {
            var day = new Day16("_sample");
            day.parsePart1();
            assertEquals(46L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day16("");
            day.parsePart1();
            assertEquals(8901L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day16("_sample");
            day.parsePart2();
            assertEquals(51L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day16("");
            day.parsePart2();
            assertEquals(9064L, day.solvePart2());
        }
    }
}
/*

--- Day 16: The Floor Will Be Lava ---

With the beam of light completely focused somewhere, the reindeer leads you deeper still into the Lava Production Facility. At some point, you realize that the steel facility walls have been replaced with cave, and the doorways are just cave, and the floor is cave, and you're pretty sure this is actually just a giant cave.

Finally, as you approach what must be the heart of the mountain, you see a bright light in a cavern up ahead. There, you discover that the beam of light you so carefully focused is emerging from the cavern wall closest to the facility and pouring all of its energy into a contraption on the opposite side.

Upon closer inspection, the contraption appears to be a flat, two-dimensional square grid containing empty space (.), mirrors (/ and \), and splitters (| and -).

The contraption is aligned so that most of the beam bounces around the grid, but each tile on the grid converts some of the beam's light into heat to melt the rock in the cavern.

You note the layout of the contraption (your puzzle input). For example:

.|...\....
|.-.\.....
.....|-...
........|.
..........
.........\
..../.\\..
.-.-/..|..
.|....-|.\
..//.|....

The beam enters in the top-left corner from the left and heading to the right. Then, its behavior depends on what it encounters as it moves:

    If the beam encounters empty space (.), it continues in the same direction.
    If the beam encounters a mirror (/ or \), the beam is reflected 90 degrees depending on the angle of the mirror. For instance, a rightward-moving beam that encounters a / mirror would continue upward in the mirror's column, while a rightward-moving beam that encounters a \ mirror would continue downward from the mirror's column.
    If the beam encounters the pointy end of a splitter (| or -), the beam passes through the splitter as if the splitter were empty space. For instance, a rightward-moving beam that encounters a - splitter would continue in the same direction.
    If the beam encounters the flat side of a splitter (| or -), the beam is split into two beams going in each of the two directions the splitter's pointy ends are pointing. For instance, a rightward-moving beam that encounters a | splitter would split into two beams: one that continues upward from the splitter's column and one that continues downward from the splitter's column.

Beams do not interact with other beams; a tile can have many beams passing through it at the same time. A tile is energized if that tile has at least one beam pass through it, reflect in it, or split in it.

In the above example, here is how the beam of light bounces around the contraption:

>|<<<\....
|v-.\^....
.v...|->>>
.v...v^.|.
.v...v^...
.v...v^..\
.v../2\\..
<->-/vv|..
.|<<<2-|.\
.v//.|.v..

Beams are only shown on empty tiles; arrows indicate the direction of the beams. If a tile contains beams moving in multiple directions, the number of distinct directions is shown instead. Here is the same diagram but instead only showing whether a tile is energized (#) or not (.):

######....
.#...#....
.#...#####
.#...##...
.#...##...
.#...##...
.#..####..
########..
.#######..
.#...#.#..

Ultimately, in this example, 46 tiles become energized.

The light isn't energizing enough tiles to produce lava; to debug the contraption, you need to start by analyzing the current situation. With the beam starting in the top-left heading right, how many tiles end up being energized?

Your puzzle answer was 8901.

--- Part Two ---

As you try to work out what might be wrong, the reindeer tugs on your shirt and leads you to a nearby control panel. There, a collection of buttons lets you align the contraption so that the beam enters from any edge tile and heading away from that edge. (You can choose either of two directions for the beam if it starts on a corner; for instance, if the beam starts in the bottom-right corner, it can start heading either left or upward.)

So, the beam could start on any tile in the top row (heading downward), any tile in the bottom row (heading upward), any tile in the leftmost column (heading right), or any tile in the rightmost column (heading left). To produce lava, you need to find the configuration that energizes as many tiles as possible.

In the above example, this can be achieved by starting the beam in the fourth tile from the left in the top row:

.|<2<\....
|v-v\^....
.v.v.|->>>
.v.v.v^.|.
.v.v.v^...
.v.v.v^..\
.v.v/2\\..
<-2-/vv|..
.|<<<2-|.\
.v//.|.v..

Using this configuration, 51 tiles are energized:

.#####....
.#.#.#....
.#.#.#####
.#.#.##...
.#.#.##...
.#.#.##...
.#.#####..
########..
.#######..
.#...#.#..

Find the initial beam configuration that energizes the largest number of tiles; how many tiles are energized in that configuration?

Your puzzle answer was 9064.

 */