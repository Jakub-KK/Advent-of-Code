package dev.aoc.aoc2023;

import dev.aoc.common.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day16 extends Day {
    public Day16(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day16("_sample")); // _sample, _loop_1, _loop_2, _large1
    }

    private Grid<Character> objectGrid; // '.' empty, '|' and '-' splitter, '/' and '\' mirrors

    public record BeamStart(int posX, int posY, Grid.Direction dir) {}

    /** Light beam path, '|' and '-' light direction, '+' light crossing, '*' mirrors (hit) */
    private static abstract class LightGrid extends Grid<Character> {
        protected final Grid<Character> objectGrid;
        protected final Set<Character> isMirror = Set.of('/', '\\');
        protected final Set<Character> canBeamVertical = Set.of('.', '*', '-'); // set of chars that mark position as possible to travel vertically
        protected final Set<Character> canBeamHorizontal = Set.of('.', '*', '|'); // set of chars that mark position as possible to travel horizontally

        public LightGrid(Grid<Character> objectGrid) {
            super(objectGrid.getWidth(), objectGrid.getHeight(), '.', objectGrid.getElementDelimiter());
            this.objectGrid = objectGrid;
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
                // propagate: find a position where the light beam hits an object or exits grid
                int posX = head.posX, posY = head.posY;
                int dX = head.dir.dCol, dY = head.dir.dRow;
                while (objectGrid.hasColumn(posX) && objectGrid.hasRow(posY) && (
                        (objectGrid.is(posX, posY,'.') ||
                                (dX == 0 && objectGrid.is(posX, posY,'|')) || // pass through splitter parallel to light beam
                                (dY == 0 && objectGrid.is(posX, posY,'-'))
                        )
                )) {
                    // use light grid to remember beam positions and direction
                    markPath(posX, posY, dX != 0);
                    posX += dX;
                    posY += dY;
                }
                // if beam ended outside of grid, forget about it
                if (!objectGrid.hasPos(posX, posY)) {
                    escaped(posX, posY, dX, dY);
                    continue;
                }
                // consider collisions of beam with objects, spawn new beams
                propagate(posX, posY, dX, dY, lightHeads);
                // remember beam position and direction
                markPath(posX, posY, dX != 0);
            }
            return this.count(c -> c != '.');
        }
        private void markPath(int posX, int posY, boolean isBeamHorizontal) {
            if (objectGrid.isInSet(posX, posY, isMirror)) {
                this.set(posX, posY, '*'); // special mark for mirrors to allow reentry (from the other side)
            } else if (isBeamHorizontal ? this.is(posX, posY, '|') : this.is(posX, posY, '-')) {
                this.set(posX, posY, '+'); // mark light crossings
            } else {
                this.set(posX, posY, isBeamHorizontal ? '-' : '|');
            }
        }
        protected abstract void propagate(int posX, int posY, int dX, int dY, List<BeamStart> lightHeads);

        /** TODO: when light beam escapes the grid it is possible that when treating every exit point
         * as entry point (beaming in reverse direction) we cannot attain better score.
         * See http://clb.confined.space/aoc2023/#day16code
         * 1. Add calls to "escaped" to "propagate" function (missing right now)
         * 2. Check the conjecture on test cases, maybe randomly generated object grids */
        protected void escaped(int posX, int posY, int dX, int dY) {}
    }

    /** Sophisticated light propagation, general solution */
    private static class LightGridPropagate extends LightGrid {
        public LightGridPropagate(Grid<Character> objectGrid) {
            super(objectGrid);
        }
        protected void propagate(int posX, int posY, int dX, int dY, List<BeamStart> lightHeads) {
            // beam hit an object which splits or mirrors, spawn new beams depending on object and beam direction
            // do not reenter positions that were visited before if the same beam direction (taking crossing into account)
            // always reenter mirrors (entering from the other side special case)
            char objectSymbol = objectGrid.get(posX, posY);
            if ((objectSymbol == '|' || (objectSymbol == '/' && dX > 0) || (objectSymbol == '\\' && dX < 0)) && posY > 0 && this.isInSet(posX, posY - 1, canBeamVertical)) {
                lightHeads.add(new BeamStart(posX, posY - 1, Grid.Direction.UP)); // add light beam traveling up
            }
            if ((objectSymbol == '|' || (objectSymbol == '/' && dX < 0) || (objectSymbol == '\\' && dX > 0)) && posY < objectGrid.getHeight() - 1 && this.isInSet(posX, posY + 1, canBeamVertical)) {
                lightHeads.add(new BeamStart(posX, posY + 1, Grid.Direction.DOWN)); // add light beam traveling down
            }
            if ((objectSymbol == '-' || (objectSymbol == '/' && dY > 0) || (objectSymbol == '\\' && dY < 0)) && posX > 0 && this.isInSet(posX - 1, posY, canBeamHorizontal)) {
                lightHeads.add(new BeamStart(posX - 1, posY, Grid.Direction.LEFT)); // add light beam traveling left
            }
            if ((objectSymbol == '-' || (objectSymbol == '/' && dY < 0) || (objectSymbol == '\\' && dY > 0)) && posX < objectGrid.getWidth() - 1 && this.isInSet(posX + 1, posY, canBeamHorizontal)) {
                lightHeads.add(new BeamStart(posX + 1, posY, Grid.Direction.RIGHT)); // add light beam traveling right
            }
        }
    }

    /** Simpler light propagation, it needs only one bit of information to not use splitter again after first usage.
     * source: http://clb.confined.space/aoc2023/#day16code
     * WARNING: non-general solution, it fails to evade mirror loops (see _loop_1 test case), curiously sufficient for solving actual puzzle solution */
    private static class LightGridPropagateNonGeneralButSufficient extends LightGrid {
        public LightGridPropagateNonGeneralButSufficient(Grid<Character> objectGrid) {
            super(objectGrid);
        }
        protected void propagate(int posX, int posY, int dX, int dY, List<BeamStart> lightHeads) {
            // beam hit an object which splits or mirrors, spawn new beams depending on object and beam direction
            // do not reenter positions that were visited before if the same beam direction (taking crossing into account)
            // always reenter mirrors (entering from the other side special case)
            char objectSymbol = objectGrid.get(posX, posY);
            if (posY > 0 && ((objectSymbol == '|' && this.is(posX, posY, '.')) || (objectSymbol == '/' && dX > 0) || (objectSymbol == '\\' && dX < 0))) {
                lightHeads.add(new BeamStart(posX, posY - 1, Grid.Direction.UP)); // add light beam traveling up
            }
            if (posY < objectGrid.getHeight() - 1 && ((objectSymbol == '|' && this.is(posX, posY, '.')) || (objectSymbol == '/' && dX < 0) || (objectSymbol == '\\' && dX > 0))) {
                lightHeads.add(new BeamStart(posX, posY + 1, Grid.Direction.DOWN)); // add light beam traveling down
            }
            if (posX > 0 && ((objectSymbol == '-' && this.is(posX, posY, '.')) || (objectSymbol == '/' && dY > 0) || (objectSymbol == '\\' && dY < 0))) {
                lightHeads.add(new BeamStart(posX - 1, posY, Grid.Direction.LEFT)); // add light beam traveling left
            }
            if (posX < objectGrid.getWidth() - 1 && ((objectSymbol == '-' && this.is(posX, posY, '.')) || (objectSymbol == '/' && dY < 0) || (objectSymbol == '\\' && dY > 0))) {
                lightHeads.add(new BeamStart(posX + 1, posY, Grid.Direction.RIGHT)); // add light beam traveling right
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
        LightGrid lightGrid = new LightGridPropagate(objectGrid);
        long result = lightGrid.countEnergized(new BeamStart(0, 0, Grid.Direction.RIGHT));
        // long result = lightGrid.countEnergized(new BeamStart(9, 2, Grid.Direction.LEFT));
        // System.out.println(lightGrid);
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        LightGrid lightGrid = new LightGridPropagate(objectGrid);
        long[] results = new long[4];
        results[0] = IntStream.range(0, objectGrid.getWidth()).mapToLong(col -> lightGrid.countEnergized(new BeamStart(col, 0, Grid.Direction.DOWN))).max().getAsLong();
        results[1] = IntStream.range(0, objectGrid.getWidth()).mapToLong(col -> lightGrid.countEnergized(new BeamStart(col, objectGrid.getHeight() - 1, Grid.Direction.UP))).max().getAsLong();
        results[2] = IntStream.range(0, objectGrid.getHeight()).mapToLong(row -> lightGrid.countEnergized(new BeamStart(0, row, Grid.Direction.RIGHT))).max().getAsLong();
        results[3] = IntStream.range(0, objectGrid.getHeight()).mapToLong(row -> lightGrid.countEnergized(new BeamStart(objectGrid.getHeight() - 1, row, Grid.Direction.LEFT))).max().getAsLong();
        long result = Arrays.stream(results).max().getAsLong();
        return result;
    }

    private void parse() {
        objectGrid = new Grid<>(stream().toList(), "", s -> s.charAt(0), Character.class);
        int countEmpty = objectGrid.count(c -> c == '.');
        int countSplitterVertical = objectGrid.count(c -> c == '|');
        int countSplitterHorizontal = objectGrid.count(c -> c == '-');
        int countMirror1 = objectGrid.count(c -> c == '/');
        int countMirror2 = objectGrid.count(c -> c == '\\');
        int all = objectGrid.getWidth() * objectGrid.getHeight();
        System.out.printf("mirror grid %d x %d: %.2f%% empty, %.2f%% mirrors (%.2f%% /, %.2f%% \\), %.2f%% splitters (%.2f%% |, %.2f%% -) %n", objectGrid.getWidth(), objectGrid.getHeight(), 100.0*countEmpty/all, 100.0*(countMirror1+countMirror2)/all, 100.0*countMirror1/all, 100.0*countMirror2/all, 100.0*(countSplitterHorizontal+countSplitterVertical)/all, 100.0*countSplitterHorizontal/all, 100.0*countSplitterVertical/all);
        // System.out.println(objectGrid);
    }

    private void createTest(String testSuffix, int side, double probObject) {
        createTestFile(testSuffix, writer -> {
            var rng = new Random();
            Grid<Character> test = new Grid<>(side, side, '.', "");
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
        void solvePart1_sample() {
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
        void solvePart2_sample() {
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