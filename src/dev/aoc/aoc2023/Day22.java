package dev.aoc.aoc2023;

import com.google.common.collect.Streams;
import dev.aoc.common.*;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day22 extends Day {
    public Day22(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day22("")); // _sample
    }

    /** ID of the brick is used for hashCode and equals, so keep it unique for different bricks and persist when mutating brick */
    private record Brick(Triplet<Integer, Integer, Integer> start, Triplet<Integer, Integer, Integer> end, Integer id) {
        public Brick fall(int height) {
            return new Brick(new Triplet<>(getStartX(), getStartY(), getStartZ() - height), new Triplet<>(getEndX(), getEndY(), getEndZ() - height), id);
        }

        public boolean intersectsWith(int x, int y) {
            return getStartX() <= x && x <= getEndX() && getStartY() <= y && y <= getEndY();
        }

        public int getStartX() {
            return start.getValue0();
        }
        public int getStartY() {
            return start.getValue1();
        }
        public int getStartZ() {
            return start.getValue2();
        }
        public int getEndX() {
            return end.getValue0();
        }
        public int getEndY() {
            return end.getValue1();
        }
        public int getEndZ() {
            return end.getValue2();
        }
        public int getRangeX() {
            return getEndX() - getStartX() + 1;
        }
        public int getRangeY() {
            return getEndY() - getStartY() + 1;
        }
        public int getRangeZ() {
            return getEndZ() - getStartZ() + 1;
        }
        public boolean isStick() {
            if (getStartZ() != getEndZ()) {
                if (getStartX() != getEndX() || getStartY() != getEndY()) {
                    return false;
                }
            }
            return true;
        }
        public boolean isOriented() {
            return getStartX() <= getEndX() &&
                    getStartY() <= getEndY() &&
                    getStartZ() <= getEndZ();
        }
        public boolean isSingle() {
            return getRangeX() == 1 && getRangeY() == 1 && getRangeZ() == 1;
        }
        public boolean isVertical() {
            return getStartX() == getEndX() &&
                    getStartY() == getEndY() &&
                    getStartZ() < getEndZ();
        }
        public boolean isHorizontal() {
            return (getStartX() < getEndX() && getStartY() == getEndY()) ||
                    (getStartX() == getEndX() && getStartY() < getEndY()) &&
                    getStartZ() == getEndZ();
        }
        public boolean isFloor() {
            return getStartZ() == 0;
        }
        public static Brick parse(String s) {
            String[] parts = s.split("~");
            return new Brick(parsePoint3D(parts[0]), parsePoint3D(parts[1]), brickId++);
        }
        private static int brickId = 1;

        private static Triplet<Integer, Integer, Integer> parsePoint3D(String s) {
            String[] parts = s.split(",");
            int[] intParts = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            return new Triplet<>(intParts[0], intParts[1], intParts[2]);
        }

        public void addToMinMax(MinMaxBounds3D minMax) {
            minMax.accX(getStartX());
            minMax.accX(getEndX());
            minMax.accY(getStartY());
            minMax.accY(getEndY());
            minMax.accZ(getStartZ());
            minMax.accZ(getEndZ());
        }

        @Override
        public String toString() {
            return "%d,%d,%d~%d,%d,%d(%s,%s)".formatted(getStartX(), getStartY(), getStartZ(), getEndX(), getEndY(), getEndZ(), isSingle() ? '.' : isVertical() ? '|' : isHorizontal() ? '-' : '?', id);
        }

        public boolean equals(Brick that) {
            return id.equals(that.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Brick brick = (Brick) o;
            return Objects.equals(id, brick.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static class BrickJenga {
        private final List<Brick> brickList;
        private final MinMaxBounds3D minMax;

        public BrickJenga(Stream<String> inputStream) {
            List<Brick> inputBrickList = inputStream.map(Brick::parse).toList();
            // accept only stick-like bricks
            List<Brick> nonStickBricks = inputBrickList.stream().filter(brick -> !brick.isStick()).toList();
            if (!nonStickBricks.isEmpty()) {
                throw new IllegalArgumentException("brick is non-stick, %s".formatted(String.join(", ", nonStickBricks.stream().map(Brick::toString).toList())));
            }
            // accept only properly oriented bricks
            List<Brick> nonOrientedBricks = inputBrickList.stream().filter(brick -> !brick.isOriented()).toList();
            if (!nonOrientedBricks.isEmpty()) {
                throw new IllegalArgumentException("brick is non-oriented, %s".formatted(String.join(", ", nonOrientedBricks.stream().map(Brick::toString).toList())));
            }
            // reject input with any brick intersecting with level 0 (floor)
            List<Brick> floorIntersectingBricks = inputBrickList.stream().filter(brick -> brick.getStartZ() <= 0).toList();
            if (!floorIntersectingBricks.isEmpty()) {
                throw new IllegalArgumentException("brick is floor intersecting, %s".formatted(String.join(", ", floorIntersectingBricks.stream().map(Brick::toString).toList())));
            }
            // find min max of volume containing bricks
            minMax = new MinMaxBounds3D();
            calculateMinMax(minMax, inputBrickList);
            // System.out.println(minMax);
            // create special floor brick
            Brick floor = new Brick(new Triplet<>(minMax.getMinX(), minMax.getMinY(), 0), new Triplet<>(minMax.getMaxX(), minMax.getMaxY(), 0), 0);
            Brick[] floorBrick = { floor };
            // sort brick list, reject brick list if two brick found equal
            brickList = new ArrayList<>(Streams.concat(Arrays.stream(floorBrick), inputBrickList.stream()).toList());
        }
        public BrickJenga(BrickJenga that) {
            this.brickList = new ArrayList<>(that.brickList);
            // find min max of volume containing bricks
            this.minMax = new MinMaxBounds3D();
            calculateMinMax(minMax, this.brickList);
        }

        private static void calculateMinMax(MinMaxBounds3D minMax, List<Brick> brickList) {
            brickList.forEach(brick -> {
                if (brick.isFloor()) {
                    return;
                }
                brick.addToMinMax(minMax);
            });
        }

        /** Sort by start Z then by end Z, then by other coordinates. Disallow equal bricks. */
        private static int brickComparator(Brick a, Brick b) {
            int cmp;
            if ((cmp = Integer.compare(a.getStartZ(), b.getStartZ())) != 0) return cmp;
            if ((cmp = Integer.compare(a.getEndZ(), b.getEndZ())) != 0) return cmp;
            if ((cmp = Integer.compare(a.getStartX(), b.getStartX())) != 0) return cmp;
            if ((cmp = Integer.compare(a.getEndX(), b.getEndX())) != 0) return cmp;
            if ((cmp = Integer.compare(a.getStartY(), b.getStartY())) != 0) return cmp;
            if ((cmp = Integer.compare(a.getEndY(), b.getEndY())) != 0) return cmp;
            throw new IllegalArgumentException("found two equal bricks, %s".formatted(a));
        }

        public void settle() {
            settle(null);
        }
        public void settle(Set<Brick> bricksFallen) {
            Map<Brick, Brick> settled = new HashMap<>(brickList.size());
            settled.put(brickList.get(0), brickList.get(0)); // mark floor as settled
            boolean anyFallen;
            do {
                // ArrayList<Object> brickListAfterFalling = new ArrayList<>(brickList.size());
                // System.out.println("settling");
                brickList.sort(BrickJenga::brickComparator);
                // System.out.println(this);
                anyFallen = false;
                for (int bi = 1; bi < brickList.size() && !anyFallen; bi++) { // TODO: optimization start from first not settled brick
                    Brick brick = brickList.get(bi);
                    if (settled.containsKey(brick)) {
                        continue; // ignore settled
                    }
                    int zOfNearestBrickBelow = Integer.MIN_VALUE;
                    Brick nearestBrickBelow = null;
                    int zCurrent = brick.getStartZ();
                    for (int bbi = 0; bbi < brickList.size(); bbi++) {
                        if (bi == bbi) {
                            continue;
                        }
                        Brick brickBelow = brickList.get(bbi);
                        if (brickBelow.getStartZ() >= zCurrent) {
                            break; // stop search if got to brick further up on sorted list
                        }
                        boolean brickBelowHit = false;
                        for (int x = brick.getStartX(); x <= brick.getEndX() && !brickBelowHit; x++) {
                            for (int y = brick.getStartY(); y <= brick.getEndY() && !brickBelowHit; y++) {
                                if (!brickBelow.intersectsWith(x, y)) {
                                    continue;
                                }
                                if (zOfNearestBrickBelow < brickBelow.getEndZ()) {
                                    zOfNearestBrickBelow = brickBelow.getEndZ();
                                    nearestBrickBelow = brickBelow;
                                    brickBelowHit = true;
                                }
                            }
                        }
                    }
                    int fallHeight = zCurrent - zOfNearestBrickBelow - 1;
                    if (fallHeight > 0) {
                        // System.out.printf("brick %s falls %d units%n", brick, fallHeight);
                        brick = brick.fall(fallHeight);
                        brickList.set(bi, brick);
                        if (bricksFallen != null) {
                            bricksFallen.add(brick);
                        }
                        anyFallen = true;
                    }
                    // brick is supported
                    if (settled.containsKey(nearestBrickBelow)) {
                        // System.out.printf("brick %s settled on %s%n", brick, nearestBrickBelow);
                        settled.put(brick, brick); // if supported by settled, mark as settled
                    } else {
                        // System.out.printf("brick %s supported on %s%n", brick, nearestBrickBelow); // never reached - bricks below are fully settled
                    }
                }
            } while (anyFallen);
            minMax.reset();
            calculateMinMax(minMax, brickList);
        }

        private record SupportedSupports(Map<Brick, Set<Brick>> supported, Map<Brick, Set<Brick>> supports) {}
        private SupportedSupports getSupportedSupports() {
            Map<Brick, Set<Brick>> supported = new HashMap<>(brickList.size()); // brick K is supported by bricks in V
            for (int bi = 1; bi < brickList.size(); bi++) {
                Brick brick = brickList.get(bi);
                int zCurrent = brick.getStartZ();
                for (int bbi = 0; bbi < brickList.size(); bbi++) {
                    if (bi == bbi) {
                        continue;
                    }
                    Brick brickBelow = brickList.get(bbi);
                    if (brickBelow.getStartZ() >= zCurrent) {
                        break; // stop search if got to brick further up on sorted list
                    }
                    for (int x = brick.getStartX(); x <= brick.getEndX(); x++) {
                        for (int y = brick.getStartY(); y <= brick.getEndY(); y++) {
                            if (!brickBelow.intersectsWith(x, y)) {
                                continue;
                            }
                            int distance = brick.getStartZ() - brickBelow.getEndZ() - 1;
                            if (distance == 0) {
                                supported.computeIfAbsent(brick, key -> new HashSet<>()).add(brickBelow);
                            }
                        }
                    }
                }
            }
            Map<Brick, Set<Brick>> supports = new HashMap<>(brickList.size()); // brick K supports bricks in V
            for (Brick brick : supported.keySet()) {
                Set<Brick> supportedBy = supported.get(brick);
                for (Brick supportedByBrick : supportedBy) {
                    supports.computeIfAbsent(supportedByBrick, key -> new HashSet<>()).add(brick);
                }
            }
            return new SupportedSupports(supported, supports);
        }

        /** Count by analysis, very effective, O(n) */
        public int countSafeToDisintegrate() {
            SupportedSupports ss = getSupportedSupports();
            return (int)brickList.stream().filter(brick -> // count number of bricks that...
                    !ss.supports.containsKey(brick) || // ...do not support anything
                            ss.supports.get(brick).stream().noneMatch(supportedBrick -> ss.supported.get(supportedBrick).size() == 1) // ...or no bricks supported are supported solely by that brick
                    )
                    .count();
        }

        /** Count by simulation (for each brick remove it and settle), ineffective, O(n^3)
         * TODO: create effective solution (
         */
        public int countSumOfBricksFallingAfterEachBrickDisintegration() {
            SupportedSupports ss = getSupportedSupports();
            return brickList.stream().mapToInt(brick -> {
                if (brick.isFloor()) {
                    return 0;
                }
                if (ss.supports.containsKey(brick) && ss.supports.get(brick).stream().noneMatch(supportedBrick -> ss.supported.get(supportedBrick).size() == 1)) {
                    return 0;
                }
                return countBricksFallingAfterBrickDisintegration(brick);
            }).sum();
        }

        private int countBricksFallingAfterBrickDisintegration(Brick brickDisintegrated) {
            BrickJenga jenga = new BrickJenga(this);
            jenga.removeBrick(brickDisintegrated);
            HashSet<Brick> bricksFallen = new HashSet<>(brickList.size());
            jenga.settle(bricksFallen);
            return bricksFallen.size();
        }

        private void removeBrick(Brick brick) {
            brickList.remove(brick);
        }

        public String toStringSections() {
            Grid<Character> result = new Grid<>(minMax.getXRange() + 3 + minMax.getYRange(), minMax.getZRange() + 1, '.', "");
            for (Brick brick : brickList) {
                for (int x = brick.getStartX(); x <= brick.getEndX(); x++) {
                    for (int z = brick.getStartZ(); z <= brick.getEndZ(); z++) {
                        result.set(x, minMax.getZRange() - z, z == 0 ? '-' : '#');
                    }
                }
                for (int y = brick.getStartY(); y <= brick.getEndY(); y++) {
                    for (int z = brick.getStartZ(); z <= brick.getEndZ(); z++) {
                        result.set(minMax.getXRange() + 3 + y, minMax.getZRange() - z, z == 0 ? '-' : '#');
                    }
                }
            }
            return result.toString();
        }

        @Override
        public String toString() {
            return String.join("\r\n", brickList.stream().map(Brick::toString).toList());
        }
    }

    private BrickJenga brickJenga;

    private void parse() {
        brickJenga = new BrickJenga(stream());
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // if (true) return null;
        // System.out.println(brickJenga);
        // System.out.println(brickJenga.toStringSections());
        brickJenga.settle();
        // System.out.println(brickJenga.toStringSections());
        long result = brickJenga.countSafeToDisintegrate();
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        brickJenga.settle();
        long result = brickJenga.countSumOfBricksFallingAfterEachBrickDisintegration();
        return result;
    }


    public static class Day22Test {
        @Test
        void solvePart1_small() {
            var day = new Day22("_sample");
            day.parsePart1();
            assertEquals(5L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day22("");
            day.parsePart1();
            assertEquals(439L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day22("_sample");
            day.parsePart2();
            assertEquals(7L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day22("");
            day.parsePart2();
            assertEquals(43056L, day.solvePart2());
        }
    }
}
/*

--- Day 22: Sand Slabs ---

Enough sand has fallen; it can finally filter water for Snow Island.

Well, almost.

The sand has been falling as large compacted bricks of sand, piling up to form an impressive stack here near the edge of Island Island. In order to make use of the sand to filter water, some of the bricks will need to be broken apart - nay, disintegrated - back into freely flowing sand.

The stack is tall enough that you'll have to be careful about choosing which bricks to disintegrate; if you disintegrate the wrong brick, large portions of the stack could topple, which sounds pretty dangerous.

The Elves responsible for water filtering operations took a snapshot of the bricks while they were still falling (your puzzle input) which should let you work out which bricks are safe to disintegrate. For example:

1,0,1~1,2,1
0,0,2~2,0,2
0,2,3~2,2,3
0,0,4~0,2,4
2,0,5~2,2,5
0,1,6~2,1,6
1,1,8~1,1,9

Each line of text in the snapshot represents the position of a single brick at the time the snapshot was taken. The position is given as two x,y,z coordinates - one for each end of the brick - separated by a tilde (~). Each brick is made up of a single straight line of cubes, and the Elves were even careful to choose a time for the snapshot that had all of the free-falling bricks at integer positions above the ground, so the whole snapshot is aligned to a three-dimensional cube grid.

A line like 2,2,2~2,2,2 means that both ends of the brick are at the same coordinate - in other words, that the brick is a single cube.

Lines like 0,0,10~1,0,10 or 0,0,10~0,1,10 both represent bricks that are two cubes in volume, both oriented horizontally. The first brick extends in the x direction, while the second brick extends in the y direction.

A line like 0,0,1~0,0,10 represents a ten-cube brick which is oriented vertically. One end of the brick is the cube located at 0,0,1, while the other end of the brick is located directly above it at 0,0,10.

The ground is at z=0 and is perfectly flat; the lowest z value a brick can have is therefore 1. So, 5,5,1~5,6,1 and 0,2,1~0,2,5 are both resting on the ground, but 3,3,2~3,3,3 was above the ground at the time of the snapshot.

Because the snapshot was taken while the bricks were still falling, some bricks will still be in the air; you'll need to start by figuring out where they will end up. Bricks are magically stabilized, so they never rotate, even in weird situations like where a long horizontal brick is only supported on one end. Two bricks cannot occupy the same position, so a falling brick will come to rest upon the first other brick it encounters.

Here is the same example again, this time with each brick given a letter so it can be marked in diagrams:

1,0,1~1,2,1   <- A
0,0,2~2,0,2   <- B
0,2,3~2,2,3   <- C
0,0,4~0,2,4   <- D
2,0,5~2,2,5   <- E
0,1,6~2,1,6   <- F
1,1,8~1,1,9   <- G

At the time of the snapshot, from the side so the x axis goes left to right, these bricks are arranged like this:

 x
012
.G. 9
.G. 8
... 7
FFF 6
..E 5 z
D.. 4
CCC 3
BBB 2
.A. 1
--- 0

Rotating the perspective 90 degrees so the y axis now goes left to right, the same bricks are arranged like this:

 y
012
.G. 9
.G. 8
... 7
.F. 6
EEE 5 z
DDD 4
..C 3
B.. 2
AAA 1
--- 0

Once all of the bricks fall downward as far as they can go, the stack looks like this, where ? means bricks are hidden behind other bricks at that location:

 x
012
.G. 6
.G. 5
FFF 4
D.E 3 z
??? 2
.A. 1
--- 0

Again from the side:

 y
012
.G. 6
.G. 5
.F. 4
??? 3 z
B.C 2
AAA 1
--- 0

Now that all of the bricks have settled, it becomes easier to tell which bricks are supporting which other bricks:

    Brick A is the only brick supporting bricks B and C.
    Brick B is one of two bricks supporting brick D and brick E.
    Brick C is the other brick supporting brick D and brick E.
    Brick D supports brick F.
    Brick E also supports brick F.
    Brick F supports brick G.
    Brick G isn't supporting any bricks.

Your first task is to figure out which bricks are safe to disintegrate. A brick can be safely disintegrated if, after removing it, no other bricks would fall further directly downward. Don't actually disintegrate any bricks - just determine what would happen if, for each brick, only that brick were disintegrated. Bricks can be disintegrated even if they're completely surrounded by other bricks; you can squeeze between bricks if you need to.

In this example, the bricks can be disintegrated as follows:

    Brick A cannot be disintegrated safely; if it were disintegrated, bricks B and C would both fall.
    Brick B can be disintegrated; the bricks above it (D and E) would still be supported by brick C.
    Brick C can be disintegrated; the bricks above it (D and E) would still be supported by brick B.
    Brick D can be disintegrated; the brick above it (F) would still be supported by brick E.
    Brick E can be disintegrated; the brick above it (F) would still be supported by brick D.
    Brick F cannot be disintegrated; the brick above it (G) would fall.
    Brick G can be disintegrated; it does not support any other bricks.

So, in this example, 5 bricks can be safely disintegrated.

Figure how the blocks will settle based on the snapshot. Once they've settled, consider disintegrating a single brick; how many bricks could be safely chosen as the one to get disintegrated?

Your puzzle answer was 439.

--- Part Two ---

Disintegrating bricks one at a time isn't going to be fast enough. While it might sound dangerous, what you really need is a chain reaction.

You'll need to figure out the best brick to disintegrate. For each brick, determine how many other bricks would fall if that brick were disintegrated.

Using the same example as above:

    Disintegrating brick A would cause all 6 other bricks to fall.
    Disintegrating brick F would cause only 1 other brick, G, to fall.

Disintegrating any other brick would cause no other bricks to fall. So, in this example, the sum of the number of other bricks that would fall as a result of disintegrating each brick is 7.

For each brick, determine how many other bricks would fall if that brick were disintegrated. What is the sum of the number of other bricks that would fall?

Your puzzle answer was 43056.

 */