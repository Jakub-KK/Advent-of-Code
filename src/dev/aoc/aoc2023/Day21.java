package dev.aoc.aoc2023;

import dev.aoc.common.*;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day21 extends Day {
    public Day21(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day21("_sample_1x1maps"));
        // _sample_1x1maps, _sample_3x3maps
        // _sample_empty_1x1maps - fully symmetric
        // _sample_twohead_1x1maps - maximally asymmetric
        // _test_1x1maps
        // _test_mixed_1x1maps (creates stable with mixed patterns)
    }

    private static class GardenGrid extends Grid<Character> {
        public GardenGrid(List<String> lines, String elementDelimiter, Function<String, Character> parser) {
            super(lines, elementDelimiter, parser, Character.valueOf(' ').getClass());
            // no tests were done for input grids of even dimensions
            if ((getWidth() & 1) == 0 || (getHeight() & 1) == 0) {
                throw new IllegalArgumentException("input dimension not odd");
            }
            // check for empty border around center
            int borderCount = count((col, row) -> ((col == 0 || col == getWidth() - 1) || (row == 0 || row == getHeight() - 1)) && is(col, row, '.'));
            if (borderCount != 2 * getWidth() + 2 * getHeight() - 4) {
                throw new IllegalArgumentException("input border must be empty");
            }
            startCol = startRow = -1;
            for (int row = 0; row < getHeight(); row++) {
                for (int col = 0; col < getWidth(); col++) {
                    char cell = get(col, row);
                    if (cell == 'S') {
                        startCol = col;
                        startRow = row;
                    }
                }
            }
            if (startCol < 0) {
                throw new IllegalArgumentException("missing starting point 'S");
            }
            FloodUnit flooded = flood();
            allReachableNonStoneEven = getPlotCountReachableInStepsNonOptimized(flooded.maxSteps * 2, flooded);
            allReachableNonStoneOdd = getPlotCountReachableInStepsNonOptimized(flooded.maxSteps * 2 + 1, flooded);
        }
        private GardenGrid(GardenGrid that, int width, int height, char fillElement) {
            super(width, height, fillElement, that.elementClass, that.getElementDelimiter());
        }

        private int startCol;
        private int startRow;

        private int allReachableNonStoneEven;
        private int allReachableNonStoneOdd;

        /** Stability: subtract parallel sides, if constant, the flood pattern will continue in outward direction from origin */
        public record FloodUnit(Grid<Integer> floodedGrid, int minSteps, int maxSteps,
                                boolean isStable,
                                int stableOffset, int stableCounter) {
            public FloodUnit floodStable() {
                return floodStable(1);
            }
            public FloodUnit floodStable(int counterDelta) {
                if (!isStable) {
                    throw new IllegalStateException("not stable");
                }
                return new FloodUnit(floodedGrid, minSteps + counterDelta * stableOffset, maxSteps + counterDelta * stableOffset, isStable, stableOffset, stableCounter + counterDelta);
            }
            public FloodUnit floodStableHalf() {
                if (!isStable) {
                    throw new IllegalStateException("not stable");
                }
                return new FloodUnit(floodedGrid, minSteps + stableOffset / 2, maxSteps + stableOffset / 2, isStable, stableOffset / 2, 2 * stableCounter + 1);
            }
            public int get(int col, int row) {
                return stableTransform(floodedGrid.get(col, row));
            }
            public int stableTransform(int val) {
                return val != CELL_NOT_VISITED ? val + stableCounter * stableOffset : CELL_NOT_VISITED;
            }
            /** True if for given steps this flood grid part is possible to achieve (may be fully covered) */
            public boolean isAchievableIn(int steps) {
                return minSteps <= steps;
            }
            /** True if for given steps this flood is fully covered (implies that it's achievable) */
            public boolean isFullyCoveredBy(int steps) {
                return maxSteps <= steps;
            }
            public boolean equalsStable(FloodUnit that) {
                if (minSteps != that.minSteps || maxSteps != that.maxSteps || floodedGrid.getWidth() != that.floodedGrid.getWidth() || floodedGrid.getHeight() != that.floodedGrid.getHeight()) {
                    return false;
                }
                int countDifferent = floodedGrid.count((col, row) -> floodedGrid.get(col, row) != that.floodedGrid.get(col, row));
                return countDifferent > 0;
            }

            @Override
            public String toString() {
                return "<%s[%d,%d]|%s>".formatted(isStable ? "%d*%d|".formatted(stableOffset, stableCounter) : "", minSteps, maxSteps, floodedGrid.toString());
            }
        }

        private record PathHead(int steps, int col, int row) {
            public static int comparator(PathHead a, PathHead b) {
                return Integer.compare(a.steps, b.steps);
            }
        }

        private static final int CELL_NOT_VISITED = Integer.MIN_VALUE;

        private FloodUnit flood(List<PathHead> pathHeads, FloodUnit parent) {
            Grid<Integer> floodedGrid = new Grid<>(getWidth(), getHeight(), CELL_NOT_VISITED, ",");
            int minSteps = Integer.MAX_VALUE, maxSteps = Integer.MIN_VALUE;
            Direction[] dirs = Direction.getAll();
            // List<PathHead> front = new LinkedList<>(pathHeads);
            // front.sort(PathHead::comparator);
            PriorityQueue<PathHead> front = new PriorityQueue<>(PathHead::comparator);
            front.addAll(pathHeads);
            for (PathHead pathHead : front) {
                floodedGrid.set(pathHead.col, pathHead.row, pathHead.steps);
                minSteps = Math.min(minSteps, pathHead.steps);
                maxSteps = Math.max(maxSteps, pathHead.steps);
            }
            while (!front.isEmpty()) {
                PathHead pos = front.poll();//.removeFirst();
                minSteps = Math.min(minSteps, pos.steps);
                maxSteps = Math.max(maxSteps, pos.steps);
                for (Direction dir : dirs) {
                    int newPosCol = pos.col + dir.dCol, newPosRow = pos.row + dir.dRow;
                    if (!hasPos(newPosCol, newPosRow)) {
                        continue; // out of bounds
                    }
                    if (is(newPosCol, newPosRow, '#')) {
                        continue; // stone (obstacle)
                    }
                    int prevSteps = floodedGrid.get(newPosCol, newPosRow);
                    int stepsAtNewPos = pos.steps + 1;
                    if (prevSteps != CELL_NOT_VISITED) {
                        if (prevSteps > stepsAtNewPos) {
                            throw new IllegalStateException("shorter path detected, invariant violated");
                        }
                        continue; // visited before
                    }
                    floodedGrid.set(newPosCol, newPosRow, stepsAtNewPos);
                    front.add(new PathHead(stepsAtNewPos, newPosCol, newPosRow));
                }
            }
            return floodWithStability(floodedGrid, minSteps, maxSteps, parent);
        }
        private FloodUnit floodWithStability(Grid<Integer> floodedGrid, int minSteps, int maxSteps, FloodUnit parent) {
            if (parent != null) {
                Grid<Integer> difference = floodDifference(parent.floodedGrid, floodedGrid);
                int val = difference.get(0, 0);
                boolean isStable = difference.count(v -> v == val) == difference.count(v -> v > 0);
                // boolean isStable = true;
                // int colLastIdx = getWidth() - 1, rowLastIdx = getHeight() - 1;
                // int diffVal, diffValNext;
                // // int[] diffVectorHorizontal = new int[Math.max(getWidth(), getHeight())];
                // // Arrays.fill(diffVectorHorizontal, Integer.MIN_VALUE);
                // diffVal = difference.get(0, 0) - difference.get(0, rowLastIdx);
                // // diffVectorHorizontal[0] = diffVal;
                // for (int col = 1; col < getWidth(); col++) {
                //     diffValNext = difference.get(col, 0) - difference.get(col, rowLastIdx);
                //     // diffVectorHorizontal[col] = diffValNext;
                //     if (diffVal != diffValNext) {
                //         isStable = false;
                //         break;
                //     }
                // }
                // // int[] diffVectorVertical = new int[Math.max(getWidth(), getHeight())];
                // // Arrays.fill(diffVectorVertical, Integer.MIN_VALUE);
                // diffVal = difference.get(0, 0) - difference.get(colLastIdx, 0);
                // // diffVectorVertical[0] = diffVal;
                // for (int row = 1; row < getHeight(); row++) {
                //     diffValNext = difference.get(0, row) - difference.get(colLastIdx, row);
                //     // diffVectorVertical[row] = diffValNext;
                //     if (diffVal != diffValNext) {
                //         isStable = false;
                //         break;
                //     }
                // }
                if (isStable) {
                    int offset = difference.get(0, 0); // offset is constant and because of the map border presence invariant we know that 0,0 contains value
                    return new FloodUnit(floodedGrid, minSteps, maxSteps, true, offset, 0);
                }
            }
            return new FloodUnit(floodedGrid, minSteps, maxSteps, false, 0, 0);
        }

        public FloodUnit flood() {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            pathHeads.add(new PathHead(0, startCol, startRow));
            return flood(pathHeads, null);
        }

        public FloodUnit floodAdjacentMapRight(FloodUnit parentLeft) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            for (int row = 0; row < getHeight(); row++) {
                pathHeads.add(new PathHead(parentLeft.get(getWidth() - 1, row) + 1, 0, row));
            }
            return flood(pathHeads, parentLeft);
        }
        public FloodUnit floodAdjacentMapLeft(FloodUnit parentRight) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            for (int row = 0; row < getHeight(); row++) {
                pathHeads.add(new PathHead(parentRight.get(0, row) + 1, getWidth() - 1, row));
            }
            return flood(pathHeads, parentRight);
        }
        public FloodUnit floodAdjacentMapDown(FloodUnit parentUp) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            for (int col = 0; col < getWidth(); col++) {
                pathHeads.add(new PathHead(parentUp.get(col, getHeight() - 1) + 1, col, 0));
            }
            return flood(pathHeads, parentUp);
        }
        public FloodUnit floodAdjacentMapUp(FloodUnit parentDown) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            for (int col = 0; col < getWidth(); col++) {
                pathHeads.add(new PathHead(parentDown.get(col, 0) + 1, col, getHeight() - 1));
            }
            return flood(pathHeads, parentDown);
        }
        public FloodUnit floodAdjacentMapRightDown(FloodUnit parentUp, FloodUnit parentLeft, FloodUnit parentDiagonal) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            int tmpL = parentLeft.get(getWidth() - 1, 0);
            int tmpU = parentUp.get(0, getHeight() - 1);
            if (tmpL != tmpU) {
                throw new IllegalStateException("guard");
            }
            pathHeads.add(new PathHead(Math.min(tmpL, tmpU) + 1, 0, 0));
            for (int row = 1; row < getHeight(); row++) {
                pathHeads.add(new PathHead(parentLeft.get(getWidth() - 1, row) + 1, 0, row));
            }
            for (int col = 1; col < getWidth(); col++) {
                pathHeads.add(new PathHead(parentUp.get(col, getHeight() - 1) + 1, col, 0));
            }
            return flood(pathHeads, parentDiagonal);
        }
        public FloodUnit floodAdjacentMapLeftDown(FloodUnit parentUp, FloodUnit parentRight, FloodUnit parentDiagonal) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            int tmpR = parentRight.get(0, 0);
            int tmpU = parentUp.get(getWidth() - 1, getHeight() - 1);
            if (tmpR != tmpU) {
                throw new IllegalStateException("guard");
            }
            pathHeads.add(new PathHead(Math.min(tmpR, tmpU) + 1, getWidth() - 1, 0));
            for (int row = 1; row < getHeight(); row++) {
                pathHeads.add(new PathHead(parentRight.get(0, row) + 1, getWidth() - 1, row));
            }
            for (int col = 0; col < getWidth() - 1; col++) {
                pathHeads.add(new PathHead(parentUp.get(col, getHeight() - 1) + 1, col, 0));
            }
            return flood(pathHeads, parentDiagonal);
        }
        public FloodUnit floodAdjacentMapRightUp(FloodUnit parentDown, FloodUnit parentLeft, FloodUnit parentDiagonal) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            int tmpL = parentLeft.get(getWidth() - 1, getHeight() - 1);
            int tmpD = parentDown.get(0, 0);
            if (tmpL != tmpD) {
                throw new IllegalStateException("guard");
            }
            pathHeads.add(new PathHead(Math.min(tmpL, tmpD) + 1, 0, getHeight() - 1));
            for (int row = 0; row < getHeight() - 1; row++) {
                pathHeads.add(new PathHead(parentLeft.get(getWidth() - 1, row) + 1, 0, row));
            }
            for (int col = 1; col < getWidth(); col++) {
                pathHeads.add(new PathHead(parentDown.get(col, 0) + 1, col, getHeight() - 1));
            }
            return flood(pathHeads, parentDiagonal);
        }
        public FloodUnit floodAdjacentMapLeftUp(FloodUnit parentDown, FloodUnit parentRight, FloodUnit parentDiagonal) {
            ArrayList<PathHead> pathHeads = new ArrayList<>();
            int tmpR = parentRight.get(0, getHeight() - 1);
            int tmpU = parentDown.get(getWidth() - 1, 0);
            if (tmpR != tmpU) {
                throw new IllegalStateException("guard");
            }
            pathHeads.add(new PathHead(Math.min(tmpR, tmpU) + 1, getWidth() - 1, getHeight() - 1));
            for (int row = 0; row < getHeight() - 1; row++) {
                pathHeads.add(new PathHead(parentRight.get(0, row) + 1, getWidth() - 1, row));
            }
            for (int col = 0; col < getWidth() - 1; col++) {
                pathHeads.add(new PathHead(parentDown.get(col, 0) + 1, col, getHeight() - 1));
            }
            return flood(pathHeads, parentDiagonal);
        }

        public Grid<Integer> floodDifference(Grid<Integer> base, Grid<Integer> next) {
            Grid<Integer> result = next.getTemplate(null);
            result.map((col, row) -> {
                if (next.get(col, row) < 0 == base.get(col, row) < 0) {
                    return next.get(col, row) - base.get(col, row);
                } else {
                    throw new IllegalArgumentException("flooded grid mismatch");
                }
            });
            return result;
        }

        public Grid<Integer> floodDifference(FloodUnit base, FloodUnit next) {
            Grid<Integer> result = next.floodedGrid.getTemplate(null);
            result.map((col, row) -> {
                if (next.get(col, row) < 0 == base.get(col, row) < 0) {
                    return next.get(col, row) - base.get(col, row);
                } else {
                    throw new IllegalArgumentException("flooded grid mismatch");
                }
            });
            return result;
        }

        public Grid<Character> createInputFromFloodedGrid(Grid<Integer> floodedGrid) {
            Grid<Character> result = new Grid<>(getWidth(), getHeight(), '.', "");
            for (int row = 0; row < getHeight(); row++) {
                for (int col = 0; col < getWidth(); col++) {
                    if (is(col, row, '#')) {
                        result.set(col, row, '#'); // for stones use ':' as it's less visually cluttered
                    } else {
                        int steps = floodedGrid.get(col, row);
                        result.set(col, row, steps == 0 ? 'S' : '.');
                    }
                }
            }
            return result;
        }
        public Grid<Character> createInputFromFloodedGrid(FloodUnit flooded) {
            Grid<Character> result = new Grid<>(getWidth(), getHeight(), '.', "");
            for (int row = 0; row < getHeight(); row++) {
                for (int col = 0; col < getWidth(); col++) {
                    if (is(col, row, '#')) {
                        result.set(col, row, '#'); // for stones use ':' as it's less visually cluttered
                    } else {
                        int steps = flooded.get(col, row);
                        result.set(col, row, steps == 0 ? 'S' : '.');
                    }
                }
            }
            return result;
        }
        public Grid<Character> debugFloodedGrid(Grid<Integer> floodedGrid, int maxSteps) {
            Grid<Character> result = new Grid<>(getWidth(), getHeight(), '.', "");
            for (int row = 0; row < getHeight(); row++) {
                for (int col = 0; col < getWidth(); col++) {
                    if (is(col, row, '#')) {
                        result.set(col, row, '#'); // for stones use ':' as it's less visually cluttered
                    } else {
                        int steps = floodedGrid.get(col, row);
                        result.set(col, row, steps == CELL_NOT_VISITED ? '.' : maxSteps < steps ? '.' : debugChars.charAt(steps >= 0 ? (steps % debugChars.length()) : (debugChars.length() + steps % debugChars.length())));
                    }
                }
            }
            return result;
        }
        public Grid<Character> debugFloodedGrid(FloodUnit flooded, int maxSteps) {
            Grid<Character> result = new Grid<>(getWidth(), getHeight(), '.', "");
            for (int row = 0; row < getHeight(); row++) {
                for (int col = 0; col < getWidth(); col++) {
                    if (is(col, row, '#')) {
                        result.set(col, row, '#'); // for stones use ':' as it's less visually cluttered
                    } else {
                        int steps = flooded.get(col, row);
                        result.set(col, row, steps == CELL_NOT_VISITED ? '.' : maxSteps < steps ? '.' : debugChars.charAt(steps >= 0 ? (steps % debugChars.length()) : (debugChars.length() + steps % debugChars.length())));
                    }
                }
            }
            return result;
        }
        private static final String debugChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        public int getPlotCountReachableInSteps(int maxSteps, FloodUnit flooded) {
            int resultO = getPlotCountReachableInStepsOptimized(maxSteps, flooded);
            // int resultNO = getPlotCountReachableInStepsNonOptimized(maxSteps, flooded);
            // if (resultO != resultNO) {
            //     // throw new IllegalStateException("mismatch");
            //     System.out.printf("plot counter mismatch for: max steps %d, flood (0,0) %d%n", maxSteps & 1, flooded.floodedGrid.get(0, 0) & 1);
            //     return resultNO;
            // }
            return resultO;
        }
        private int getPlotCountReachableInStepsOptimized(int maxSteps, FloodUnit flooded) {
            if (!flooded.isAchievableIn(maxSteps)) {
                return 0;
            } else if (flooded.isFullyCoveredBy(maxSteps)) {
                // depending on parity of max steps and parity of given flood, return all even or odd reachable non-stone positions
                return ((maxSteps & 1) == 0) == ((flooded.get(0, 0) & 1) == 0) ? allReachableNonStoneEven : allReachableNonStoneOdd;
            }
            return getPlotCountReachableInStepsNonOptimized(maxSteps, flooded);
        }
        private int getPlotCountReachableInStepsNonOptimized(int maxSteps, FloodUnit flooded) {
            // count grid positions that:
            // - are visited
            // - number of steps to visit them has the same parity as required number of maxSteps (takes into account paths that trace backwards)
            // - number of steps to visit them is less then required number of maxSteps
            return flooded.floodedGrid.count(s -> {
                int ss = flooded.stableTransform(s);
                return ss >= 0 && (ss & 1) == (maxSteps & 1) && ss <= maxSteps;
            });
        }

        public long getPlotCountReachableInSteps(int steps) {
            long countReachable = 0;
            Direction[] dirs = Direction.getAll();
            List<PathHead> front = new LinkedList<>();
            int maxFrontSize = Integer.MIN_VALUE;
            int currentStep = 0;
            front.add(new PathHead(currentStep, startCol, startRow));
            Map<Long, Long> stepMapH = new HashMap<>();
            while (!front.isEmpty()) {
                if (maxFrontSize < front.size()) {
                    maxFrontSize = front.size();
                    if (maxFrontSize % 1000000 == 0) {
                        // System.out.printf("max front size %d%n", maxFrontSize);
                    }
                }
                PathHead pos = front.removeFirst();
                if (pos.steps != currentStep) {
                    currentStep = pos.steps;
                    // System.out.printf("current step %d, previous front count %d%n", currentStep, stepMapH.size());
                    stepMapH.clear();
                }
                for (Direction dir : dirs) {
                    int newPosCol = pos.col + dir.dCol, newPosRow = pos.row + dir.dRow;
                    if (!hasPos(newPosCol, newPosRow)) {
                        continue;
                    }
                    int wrappedNewPosCol = newPosCol < 0 ? (getWidth() - 1 - ((-newPosCol - 1) % getWidth())) : (newPosCol % getWidth());
                    int wrappedNewPosRow = newPosRow < 0 ? (getHeight() - 1 - ((-newPosRow - 1) % getHeight())) : (newPosRow % getHeight());
                    if (is(wrappedNewPosCol, wrappedNewPosRow, '#')) {
                        continue;
                    }
                    int stepsAtNewPos = pos.steps + 1;
                    Long hashKey = (steps + newPosRow) * (2L * steps + 1) + (steps + newPosCol); // create key as index in 1D array, there can be at most 'steps' in any direction
                    if (stepMapH.containsKey(hashKey)) {
                        continue;
                    }
                    stepMapH.put(hashKey, hashKey);
                    if (stepsAtNewPos < steps) {
                        front.addLast(new PathHead(stepsAtNewPos, newPosCol, newPosRow));
                    } else {
                        countReachable++;
                    }
                }
            }
            return countReachable;
        }

        /** Create new Grid with half the size (actually 3 + 0.5 * (W|H - 3)). Average contents. */
        public GardenGrid getHalf() {
            GardenGrid halfGardenGrid = new GardenGrid(this, 3 + (getWidth() - 3) / 2, 3 + (getHeight() - 3) / 2, '.');
            int middleColIdx = (halfGardenGrid.getWidth() - 1) / 2, middleRowIdx = (halfGardenGrid.getHeight() - 1) / 2;
            for (int row = 1; row < halfGardenGrid.getHeight() - 1; row++) {
                for (int col = 1; col < halfGardenGrid.getWidth() - 1; col++) {
                    int prevCol = (col <= middleColIdx ? 1 : 2) + 2 * (col - 1), prevRow = (row <= middleRowIdx ? 1 : 2) + 2 * (row - 1);
                    int stoneCount = IntStream.range(0, 2).map(dc -> IntStream.range(0, 2).map(dr -> get(prevCol, prevRow) == '#' ? 1 : 0).sum()).sum();
                    if (stoneCount > 2) {
                        halfGardenGrid.set(col, row, '#');
                    }
                }
            }
            halfGardenGrid.set(middleColIdx, middleRowIdx, 'S');
            return halfGardenGrid;
        }
    }

    private GardenGrid gardenGrid;

    private void parse() {
        var mapStrings = stream().collect(Collectors.toList());
        parse(mapStrings);
    }
    private void parse(List<String> mapStrings) {
        gardenGrid = new GardenGrid(mapStrings, "", s -> s.charAt(0));
        System.out.printf("garden grid %d x %d, hash %d%n", gardenGrid.getWidth(), gardenGrid.getHeight(), gardenGrid.hashCode());
        System.out.printf("garden grid: reachable: even %d, odd %d%n", gardenGrid.allReachableNonStoneEven, gardenGrid.allReachableNonStoneOdd);
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // if (true) return null;
        int maxSteps = getInputSuffix().isEmpty() ? 64 : 6;
        // long resultNaive = gardenGrid.getPlotCountReachableInSteps(maxSteps);
        // // return resultNaive;
        GardenGrid.FloodUnit flooded = gardenGrid.flood();
        long resultFlood = gardenGrid.getPlotCountReachableInSteps(maxSteps, flooded);
        // System.out.printf("result: naive %d, flood %d%n", resultNaive, resultFlood);
        return resultFlood;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        /*
        1. base grid is repeated through whole garden universe in every direction indefinitely
        2. we start from origin
        3. as the max steps value grows, we add base grids as needed going in circles around origin
        4. if we subtract added grid from specific one chosen from added before we can see tha pattern:
        4a. after first 1,2 circles the pattens of flood fills of base grids are repeating
        4b. when we go along the diagonal or any axis, subtracting subsequent base grids flood fills the difference is constant
        4c. when we choose a base grid from any octant and go one grid back in direction of origin (for axis) or diagonal (for octants) the difference is constant
        5. thus, each flood grid can be easily obtained by adding a constant multiplied by distance from "stable" flood grid from axis or diagonal
         */
        long plotCountReachableInSteps = 0;
        int maxSteps = getInputSuffix().isEmpty() ? 26501365 : 5000;
        ReturnData result = getPlotCountReachableInSteps(maxSteps, true, false, false);
        analysis(result);
        plotCountReachableInSteps = result.plotCountReachableInSteps;
        return plotCountReachableInSteps;
    }

    public void debug() {
        IntStream.range(1, 501).forEach(maxSteps -> {
            System.out.printf("max steps %d%n", maxSteps);
            ReturnData result = getPlotCountReachableInSteps(maxSteps, false, false, false);
            analysis(result);
            System.out.printf("max steps %d: plot count %d%n", maxSteps, result.plotCountReachableInSteps);
        });
    }

    private record ReturnData(
            int maxSteps,
            long plotCountReachableInSteps,
            Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitMap,
            Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitParents,
            long cycles,
            boolean optimized
    ) {}

    private enum WorkPhase { Intro, Intro2ndCircle, FindCycle, Skip, Finish }
    private ReturnData getPlotCountReachableInSteps(int maxSteps) {
        return getPlotCountReachableInSteps(maxSteps, false, false, false);
    }
    private ReturnData getPlotCountReachableInSteps(int maxSteps, boolean logging, boolean doNotOptimize, boolean storeParentFloods) {
        long plotCountReachableInSteps = 0;
        // create grid of floods going CW from pos (1,0) making ever bigger circles around origin
        Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitMap = new HashMap<>();
        Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitParents = new HashMap<>();
        GardenGrid.FloodUnit floodUnit = gardenGrid.flood();
        boolean mapReadyForMaxSteps = !floodUnit.isAchievableIn(maxSteps);
        plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
        floodUnitMap.put(new Pair<>(0, 0), floodUnit);
        int cycles = 0;
        Pair<Integer, Integer> mapPosParent, mapPosCurrent;
        int fuPosCol = 1, fuPosRow = 0;
        GardenGrid.FloodUnit floodUnitPrev = null, floodUnitParent, floodUnitParentTowardsDiagonal;
        boolean cycleStable = false;
        long cycleFloodsAdded, cyclePlotCountAdded;
        WorkPhase workPhase = WorkPhase.Intro;
        int simulationPhase1_MinCycles = Integer.MAX_VALUE;
        int minCycleStart = 0;
        boolean continueWork = doNotOptimize;
        while (!mapReadyForMaxSteps) {
            boolean canSkip = cycleStable;// && (cycles & 1) == 0; // all floods stable (constant difference with respect to parent) and stable diagonal is in circle
            boolean earlyExit = false;
            switch (workPhase) {
                case Intro: {
                    if (!continueWork && canSkip) {
                        if (false) {
                            workPhase = WorkPhase.Intro2ndCircle;
                            minCycleStart = cycles;
                            cycles++;
                            mapReadyForMaxSteps = true;
                            cycleStable = false;
                            if (logging) System.out.printf("simulation phase %d: after first stable cycle create the next one%n", workPhase.ordinal());
                        } else {
                            workPhase = WorkPhase.FindCycle;
                            mapReadyForMaxSteps = false; // we're not computing this in this phase
                            minCycleStart = cycles;
                            fuPosCol--; // go back to previous cycle
                            if (logging) System.out.printf("simulation phase %d: computing last fully covered cycle%n", workPhase.ordinal());
                        }
                    } else {
                        cycles++;
                        mapReadyForMaxSteps = true;
                        if (cycles > 8)
                        if (false) { // debug: exit immediately
                            earlyExit = true;
                            break;
                        }
                    }
                    break;
                }
                case Intro2ndCircle: { // Intro2ndCircle - TODO REMOVE
                    if (canSkip) {
                        workPhase = WorkPhase.FindCycle;
                        mapReadyForMaxSteps = false; // we're not computing this in this phase
                        // minCycleStart = cycles;
                        fuPosCol--; // go back to previous cycle
                        if (logging) System.out.printf("simulation phase %d: computing first not fully covered cycle%n", workPhase.ordinal());
                        if (true) { // debug: exit immediately
                            earlyExit = true;
                            break;
                        }
                    } else {
                        // throw new IllegalStateException("second cycle after first stable must also be stable");
                        cycles++;
                        mapReadyForMaxSteps = true;
                    }
                    break;
                }
                case FindCycle: { // one lap along first cycle with all floods stable to compute id of last cycle with full steps coverage (next cycle after is partially covered)
                    simulationPhase1_MinCycles += minCycleStart - 1;
                    if (logging) System.out.printf("simulation phase %d: last fully covered cycle %d%n", workPhase.ordinal(), simulationPhase1_MinCycles);
                    if (cycles >= simulationPhase1_MinCycles || false) { // debug: force work phase to finish map building without shortcut
                        workPhase = WorkPhase.Intro;
                        cycles++;
                        continueWork = true;
                        mapReadyForMaxSteps = true;
                        break;
                    }
                    if (false) { // debug: exit immediately
                        earlyExit = true;
                        break;
                    }
                    // skip cycles and enter next simulation step (last fully covered cycle)
                    workPhase = WorkPhase.Skip;
                    // add plot counts for every skipped cycle
                    cycleFloodsAdded = 0;
                    cyclePlotCountAdded = plotCountReachableInSteps;
                    for (int skipCycle = cycles + 1; skipCycle < simulationPhase1_MinCycles; skipCycle++) {
                        int floodsAdded = skipCycle * 4;
                        cycleFloodsAdded += floodsAdded;
                        int plotCountPerFlood = ((skipCycle & 1) == ((maxSteps - 1) & 1)) ? gardenGrid.allReachableNonStoneOdd : gardenGrid.allReachableNonStoneEven;
                        long plotCountForCycle = (long)floodsAdded * plotCountPerFlood;
                        plotCountReachableInSteps += plotCountForCycle;
                    }
                    cyclePlotCountAdded = plotCountReachableInSteps - cyclePlotCountAdded;
                    if (logging) System.out.printf("cycle skipped: floods added %d, plot count increase %d%n", cycleFloodsAdded, cyclePlotCountAdded);
                    // move to last cycle with full steps coverage
                    cycles = simulationPhase1_MinCycles;
                    fuPosCol += simulationPhase1_MinCycles - minCycleStart - 1;
                    break;
                }
                case Skip: {
                    if (false) System.out.println(analysis_DumpFloodUnitMapAsTopLevelMap(floodUnitMap, maxSteps));
                    if (false) { // debug: exit immediately
                        earlyExit = true;
                        break;
                    }
                    workPhase = WorkPhase.Intro;
                    cycles++;
                    continueWork = true;
                    mapReadyForMaxSteps = true;
                    // TODO: after finishing last fully covered cycle check average plot count per flood added, if not from two known values, then stop (or restart) b/c that cycle was not fully covered!
                    break;
                }
                default: {
                    throw new IllegalStateException("illegal simulation phase %d".formatted(workPhase.ordinal()));
                }
            }
            if (earlyExit) {
                System.out.println("early exit");
                break;
            }
            boolean allFloodsInCycleStable = true;
            cycleFloodsAdded = 0;
            cyclePlotCountAdded = plotCountReachableInSteps;
            // 1. at the beginning of the cycle position is (col>0,0): flood right
            mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
            if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                mapPosParent = new Pair<>(fuPosCol - 1, fuPosRow); // TODO: QUADRANT DEPENDENT
                floodUnitParent = floodUnitMap.get(mapPosParent);
                if (floodUnitParent.isStable) {
                    floodUnit = floodUnitParent.floodStable(); // if parent is stable just offset current flood using parent
                } else {
                    floodUnit = gardenGrid.floodAdjacentMapRight(floodUnitParent); // generate new flood using parent
                    if (floodUnit.isStable) {
                        // if generated flood is stable, it means that parent is also stable, update parent and recreate current flood using stable parent
                        floodUnitParent = new GardenGrid.FloodUnit(floodUnitParent.floodedGrid, floodUnitParent.minSteps, floodUnitParent.maxSteps, true, floodUnit.stableOffset, 0);
                        floodUnitMap.put(mapPosParent, floodUnitParent);
                        floodUnit = floodUnitParent.floodStable();
                    }
                }
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                floodUnitPrev = floodUnitParent;
            } else if (workPhase == WorkPhase.FindCycle) {
                floodUnit = floodUnitMap.get(mapPosCurrent);
                int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset);
                if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                if (simulationPhase1_MinCycles > thisMinCycle) {
                    simulationPhase1_MinCycles = thisMinCycle;
                }
            } else if (workPhase == WorkPhase.Skip) {
                int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                // search along axis for first missing flood, parent will be right before it
                int fuPosParentCol = 0, fuPosParentRow = 0;
                Pair<Integer, Integer> mapPosParentNext;
                mapPosParent = null;
                while (floodUnitMap.containsKey(mapPosParentNext = new Pair<>(fuPosParentCol, fuPosParentRow))) {
                    mapPosParent = mapPosParentNext;
                    fuPosParentCol += deltaCol;
                    fuPosParentRow += deltaRow;
                }
                floodUnitParent = floodUnitMap.get(mapPosParent);
                floodUnit = floodUnitParent.floodStable(Math.max(Math.abs(fuPosCol - mapPosParent.getValue0()), Math.abs(fuPosRow - mapPosParent.getValue1()))); // parent is stable, just offset current flood using parent
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                // allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                // floodUnitPrev = floodUnitParent;
            } else {
                throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
            }
            // 2. go row+1,col-1 flooding diagonally until col==0 with position (0,row>0)
            while (fuPosCol > 0) {
                fuPosCol--; // TODO: QUADRANT DEPENDENT
                fuPosRow++;
                if (fuPosCol == 0) {
                    break;
                }
                if (logging) if (cycles > 200_000) {
                    if ((Math.abs(fuPosCol)) % 10_000 == 0) {
                        System.out.printf("progress: 1st quadrant: current position (%d,%d)%n", fuPosCol, fuPosRow);
                    }
                }
                boolean isDiagonal = Math.abs(fuPosCol) == Math.abs(fuPosRow);
                mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
                if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                    // failed attempt for stable diamond
                    // boolean flag = false;
                    // if (isDiagonal) {
                    //     mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                    // } else if (Math.abs(fuPosCol) == 1) {
                    //     mapPosParent = new Pair<>(fuPosCol, fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (+-c,r>0)->(+-c,r-1) | (+-c,r<0)->(+-c,r+1) - one step along ROW axis towards origin
                    // } else if (Math.abs(fuPosRow) == 1) {
                    //     mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow); // (c>0,+-r)->(c-1,+-r) | (c<0,+-r)->(c+1,+-r) - one step along COL axis towards origin
                    // } else {
                    //     flag = true;
                    //     mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                    // }
                    // floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                    // if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParentTowardsDiagonal);
                    // floodUnitParent = floodUnitMap.get(new Pair<>(fuPosCol - 1, fuPosRow)); // TODO: QUADRANT DEPENDENT
                    // boolean isParentFirstRowCol = Math.abs(mapPosParent.getValue0()) == 1 || Math.abs(mapPosParent.getValue1()) == 1;
                    // boolean stopFloodStable = flag && isParentFirstRowCol;
                    // floodUnit = !stopFloodStable && floodUnitParentTowardsDiagonal.isStable ? floodUnitParentTowardsDiagonal.floodStable() : gardenGrid.floodAdjacentMapRightDown(floodUnitPrev, floodUnitParent, floodUnitParentTowardsDiagonal);
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                    } else if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow); // (c>0,+-r)->(c-1,+-r) | (c<0,+-r)->(c+1,+-r) - one step along COL axis towards origin
                    } else {
                        mapPosParent = new Pair<>(fuPosCol, fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (+-c,r>0)->(+-c,r-1) | (+-c,r<0)->(+-c,r+1) - one step along ROW axis towards origin
                    }
                    floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParentTowardsDiagonal);
                    floodUnitParent = floodUnitMap.get(new Pair<>(fuPosCol - 1, fuPosRow)); // TODO: QUADRANT DEPENDENT
                    boolean parentTowardsDiagonalIsDiagonal = Math.abs(mapPosParent.getValue0()) == Math.abs(mapPosParent.getValue1());
                    floodUnit = (isDiagonal || !parentTowardsDiagonalIsDiagonal) && floodUnitParentTowardsDiagonal.isStable ? floodUnitParentTowardsDiagonal.floodStable() : gardenGrid.floodAdjacentMapRightDown(floodUnitPrev, floodUnitParent, (isDiagonal || !parentTowardsDiagonalIsDiagonal) ? floodUnitParentTowardsDiagonal : null);
                    if (!isDiagonal && parentTowardsDiagonalIsDiagonal) {
                        // GardenGrid.FloodUnit floodUnitParentOther = floodUnitMap.get(new Pair<>(fuPosCol + (mapPosParent.getValue1() - fuPosRow), fuPosRow + (mapPosParent.getValue0() - fuPosCol)));
                        GardenGrid.FloodUnit floodUnitParentOther = floodUnitMap.get(new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)));
                        Grid<Integer> floodedDifference = gardenGrid.floodDifference(floodUnit, floodUnitParentOther);
                        int tmp = floodedDifference.get(0, 0);
                    }
                    cycleFloodsAdded++;
                    allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    floodUnitPrev = floodUnitParent;
                } else if (workPhase == WorkPhase.FindCycle) {
                    floodUnit = floodUnitMap.get(mapPosCurrent);
                    int thisMinCycle = (isDiagonal ? 2 : 1) * (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset); // going along diagonal is actually 2 cycles move
                    if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                    if (simulationPhase1_MinCycles > thisMinCycle) {
                        simulationPhase1_MinCycles = thisMinCycle;
                    }
                } else if (workPhase == WorkPhase.Skip) {
                    int floodCounter = 0;
                    int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol - deltaCol, fuPosRow - deltaRow); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                        floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                        floodCounter = 1;
                    } else {
                        // go along diagonal until row/col (depending on octant) of current is hit
                        int fuPosColDiagonalStop, fuPosRowDiagonalStop;
                        if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                            fuPosColDiagonalStop = Integer.MAX_VALUE;
                            fuPosRowDiagonalStop = fuPosRow;
                        } else {
                            fuPosColDiagonalStop = fuPosCol;
                            fuPosRowDiagonalStop = Integer.MAX_VALUE;
                        }
                        int fuPosDiagonalCol = 0, fuPosDiagonalRow = 0;
                        while (Math.abs(fuPosDiagonalCol) < Math.abs(fuPosColDiagonalStop) && Math.abs(fuPosDiagonalRow) < Math.abs(fuPosRowDiagonalStop)) {
                            fuPosDiagonalCol += deltaCol;
                            fuPosDiagonalRow += deltaRow;
                        }
                        if (Math.abs(fuPosDiagonalCol) + Math.abs(fuPosDiagonalRow) < minCycleStart) {
                            // if current position row/col hits last cycle below middle point then jump to border of the last cycle
                            if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                                mapPosParent = new Pair<>(deltaCol * (minCycleStart - Math.abs(fuPosDiagonalCol)), fuPosRow);
                            } else {
                                mapPosParent = new Pair<>(fuPosCol, deltaRow * (minCycleStart - Math.abs(fuPosDiagonalRow)));
                            }
                            floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                            floodCounter = simulationPhase1_MinCycles - minCycleStart;
                        } else {
                            mapPosParent = new Pair<>(fuPosDiagonalCol, fuPosDiagonalRow);
                            if (!floodUnitMap.containsKey(mapPosParent)) {
                                // if current position row/col hits diagonal above last cycle then create this diagonal flood and store on the map
                                GardenGrid.FloodUnit floodUnitDiagonalPrev = floodUnitMap.get(new Pair<>(fuPosDiagonalCol - deltaCol, fuPosDiagonalRow - deltaRow));
                                floodUnitParentTowardsDiagonal = floodUnitDiagonalPrev.floodStable();
                                floodUnitMap.put(mapPosParent, floodUnitParentTowardsDiagonal);
                                if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitDiagonalPrev);
                            } else {
                                floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                            }
                            // create flood based on diagonal towards current position (hack: uses half of stable flood offset instead of intermediate floods, diagonal stable flood is double the axis-parallel stable flood)
                            floodUnitParent = floodUnitParentTowardsDiagonal.floodStableHalf();
                            mapPosParent = new Pair<>(fuPosDiagonalCol + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? deltaCol : 0), fuPosDiagonalRow + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? 0 : deltaRow));
                            floodUnitMap.put(mapPosParent, floodUnitParent);
                            if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitParentTowardsDiagonal);
                            floodCounter = simulationPhase1_MinCycles - (Math.abs(mapPosParent.getValue0()) + Math.abs(mapPosParent.getValue1()));
                        }
                    }
                    floodUnit = floodUnitParent.floodStable(floodCounter);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                    cycleFloodsAdded++;
                    // allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    // floodUnitPrev = floodUnitParent;
                } else {
                    throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
                }
            }
            // 3. at pos (0,row>0): flood down
            mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
            if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                mapPosParent = new Pair<>(fuPosCol, fuPosRow - 1); // TODO: QUADRANT DEPENDENT
                floodUnitParent = floodUnitMap.get(mapPosParent);
                if (floodUnitParent.isStable) {
                    floodUnit = floodUnitParent.floodStable(); // if parent is stable just offset current flood using parent
                } else {
                    floodUnit = gardenGrid.floodAdjacentMapDown(floodUnitParent); // generate new flood using parent
                    if (floodUnit.isStable) {
                        // if generated flood is stable, it means that parent is also stable, update parent and recreate current flood using stable parent
                        floodUnitParent = new GardenGrid.FloodUnit(floodUnitParent.floodedGrid, floodUnitParent.minSteps, floodUnitParent.maxSteps, true, floodUnit.stableOffset, 0);
                        floodUnitMap.put(mapPosParent, floodUnitParent);
                        floodUnit = floodUnitParent.floodStable();
                    }
                }
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                floodUnitPrev = floodUnitParent;
            } else if (workPhase == WorkPhase.FindCycle) {
                floodUnit = floodUnitMap.get(mapPosCurrent);
                int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset);
                if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                if (simulationPhase1_MinCycles > thisMinCycle) {
                    simulationPhase1_MinCycles = thisMinCycle;
                }
            } else if (workPhase == WorkPhase.Skip) {
                int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                // search along axis for first missing flood, parent will be right before it
                int fuPosParentCol = 0, fuPosParentRow = 0;
                Pair<Integer, Integer> mapPosParentNext;
                mapPosParent = null;
                while (floodUnitMap.containsKey(mapPosParentNext = new Pair<>(fuPosParentCol, fuPosParentRow))) {
                    mapPosParent = mapPosParentNext;
                    fuPosParentCol += deltaCol;
                    fuPosParentRow += deltaRow;
                }
                floodUnitParent = floodUnitMap.get(mapPosParent);
                floodUnit = floodUnitParent.floodStable(Math.max(Math.abs(fuPosCol - mapPosParent.getValue0()), Math.abs(fuPosRow - mapPosParent.getValue1()))); // parent is stable, just offset current flood using parent
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                // allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                // floodUnitPrev = floodUnitParent;
            } else {
                throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
            }
            // 4. go row-1,col-1 flooding diagonally until row==0 with position (col<0,0)
            while (fuPosRow > 0) {
                fuPosCol--; // TODO: QUADRANT DEPENDENT
                fuPosRow--;
                if (fuPosRow == 0) {
                    break;
                }
                if (logging) if (cycles > 200_000) {
                    if ((Math.abs(fuPosCol)) % 10_000 == 0) {
                        System.out.printf("progress: 2nd quadrant: current position (%d,%d)%n", fuPosCol, fuPosRow);
                    }
                }
                boolean isDiagonal = Math.abs(fuPosCol) == Math.abs(fuPosRow);
                mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
                if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                    } else if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow); // (c>0,+-r)->(c-1,+-r) | (c<0,+-r)->(c+1,+-r) - one step along COL axis towards origin
                    } else {
                        mapPosParent = new Pair<>(fuPosCol, fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (+-c,r>0)->(+-c,r-1) | (+-c,r<0)->(+-c,r+1) - one step along ROW axis towards origin
                    }
                    floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParentTowardsDiagonal);
                    floodUnitParent = floodUnitMap.get(new Pair<>(fuPosCol, fuPosRow - 1)); // TODO: QUADRANT DEPENDENT
                    boolean parentTowardsDiagonalIsDiagonal = Math.abs(mapPosParent.getValue0()) == Math.abs(mapPosParent.getValue1());
                    floodUnit = (isDiagonal || !parentTowardsDiagonalIsDiagonal) && floodUnitParentTowardsDiagonal.isStable ? floodUnitParentTowardsDiagonal.floodStable() : gardenGrid.floodAdjacentMapLeftDown(floodUnitParent, floodUnitPrev, (isDiagonal || !parentTowardsDiagonalIsDiagonal) ? floodUnitParentTowardsDiagonal : null);
                    cycleFloodsAdded++;
                    allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    floodUnitPrev = floodUnitParent;
                } else if (workPhase == WorkPhase.FindCycle) {
                    floodUnit = floodUnitMap.get(mapPosCurrent);
                    int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset) * (isDiagonal ? 2 : 1); // going along diagonal is actually 2 cycles move
                    if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                    if (simulationPhase1_MinCycles > thisMinCycle) {
                        simulationPhase1_MinCycles = thisMinCycle;
                    }
                } else if (workPhase == WorkPhase.Skip) {
                    int floodCounter = 0;
                    int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol - deltaCol, fuPosRow - deltaRow); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                        floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                        floodCounter = 1;
                    } else {
                        // go along diagonal until row/col (depending on octant) of current is hit
                        int fuPosColDiagonalStop, fuPosRowDiagonalStop;
                        if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                            fuPosColDiagonalStop = Integer.MAX_VALUE;
                            fuPosRowDiagonalStop = fuPosRow;
                        } else {
                            fuPosColDiagonalStop = fuPosCol;
                            fuPosRowDiagonalStop = Integer.MAX_VALUE;
                        }
                        int fuPosDiagonalCol = 0, fuPosDiagonalRow = 0;
                        while (Math.abs(fuPosDiagonalCol) < Math.abs(fuPosColDiagonalStop) && Math.abs(fuPosDiagonalRow) < Math.abs(fuPosRowDiagonalStop)) {
                            fuPosDiagonalCol += deltaCol;
                            fuPosDiagonalRow += deltaRow;
                        }
                        if (Math.abs(fuPosDiagonalCol) + Math.abs(fuPosDiagonalRow) < minCycleStart) {
                            // if current position row/col hits last cycle below middle point then jump to border of the last cycle
                            if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                                mapPosParent = new Pair<>(deltaCol * (minCycleStart - Math.abs(fuPosDiagonalCol)), fuPosRow);
                            } else {
                                mapPosParent = new Pair<>(fuPosCol, deltaRow * (minCycleStart - Math.abs(fuPosDiagonalRow)));
                            }
                            floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                            floodCounter = simulationPhase1_MinCycles - minCycleStart;
                        } else {
                            mapPosParent = new Pair<>(fuPosDiagonalCol, fuPosDiagonalRow);
                            if (!floodUnitMap.containsKey(mapPosParent)) {
                                // if current position row/col hits diagonal above last cycle then create this diagonal flood and store on the map
                                GardenGrid.FloodUnit floodUnitDiagonalPrev = floodUnitMap.get(new Pair<>(fuPosDiagonalCol - deltaCol, fuPosDiagonalRow - deltaRow));
                                floodUnitParentTowardsDiagonal = floodUnitDiagonalPrev.floodStable();
                                floodUnitMap.put(mapPosParent, floodUnitParentTowardsDiagonal);
                                if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitDiagonalPrev);
                            } else {
                                floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                            }
                            // create flood based on diagonal towards current position (hack: uses half of stable flood offset instead of intermediate floods, diagonal stable flood is double the axis-parallel stable flood)
                            floodUnitParent = floodUnitParentTowardsDiagonal.floodStableHalf();
                            mapPosParent = new Pair<>(fuPosDiagonalCol + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? deltaCol : 0), fuPosDiagonalRow + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? 0 : deltaRow));
                            floodUnitMap.put(mapPosParent, floodUnitParent);
                            if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitParentTowardsDiagonal);
                            floodCounter = simulationPhase1_MinCycles - (Math.abs(mapPosParent.getValue0()) + Math.abs(mapPosParent.getValue1()));
                        }
                    }
                    floodUnit = floodUnitParent.floodStable(floodCounter);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                    cycleFloodsAdded++;
                    // allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    // floodUnitPrev = floodUnitParent;
                } else {
                    throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
                }
            }
            // 5. at pos (col<0,0): flood left
            mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
            if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                mapPosParent = new Pair<>(fuPosCol + 1, fuPosRow); // TODO: QUADRANT DEPENDENT
                floodUnitParent = floodUnitMap.get(mapPosParent);
                if (floodUnitParent.isStable) {
                    floodUnit = floodUnitParent.floodStable(); // if parent is stable just offset current flood using parent
                } else {
                    floodUnit = gardenGrid.floodAdjacentMapLeft(floodUnitParent); // generate new flood using parent
                    if (floodUnit.isStable) {
                        // if generated flood is stable, it means that parent is also stable, update parent and recreate current flood using stable parent
                        floodUnitParent = new GardenGrid.FloodUnit(floodUnitParent.floodedGrid, floodUnitParent.minSteps, floodUnitParent.maxSteps, true, floodUnit.stableOffset, 0);
                        floodUnitMap.put(mapPosParent, floodUnitParent);
                        floodUnit = floodUnitParent.floodStable();
                    }
                }
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                floodUnitPrev = floodUnitParent;
            } else if (workPhase == WorkPhase.FindCycle) {
                floodUnit = floodUnitMap.get(mapPosCurrent);
                int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset);
                if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                if (simulationPhase1_MinCycles > thisMinCycle) {
                    simulationPhase1_MinCycles = thisMinCycle;
                }
            } else if (workPhase == WorkPhase.Skip) {
                int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                // search along axis for first missing flood, parent will be right before it
                int fuPosParentCol = 0, fuPosParentRow = 0;
                Pair<Integer, Integer> mapPosParentNext;
                mapPosParent = null;
                while (floodUnitMap.containsKey(mapPosParentNext = new Pair<>(fuPosParentCol, fuPosParentRow))) {
                    mapPosParent = mapPosParentNext;
                    fuPosParentCol += deltaCol;
                    fuPosParentRow += deltaRow;
                }
                floodUnitParent = floodUnitMap.get(mapPosParent);
                floodUnit = floodUnitParent.floodStable(Math.max(Math.abs(fuPosCol - mapPosParent.getValue0()), Math.abs(fuPosRow - mapPosParent.getValue1()))); // parent is stable, just offset current flood using parent
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                // allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                // floodUnitPrev = floodUnitParent;
            } else {
                throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
            }
            // 6. go row-1,col+1 flooding diagonally until col==0 with position (0,row<0)
            while (fuPosCol < 0) {
                fuPosCol++; // TODO: QUADRANT DEPENDENT
                fuPosRow--;
                if (fuPosCol == 0) {
                    break;
                }
                if (logging) if (cycles > 200_000) {
                    if ((Math.abs(fuPosCol)) % 10_000 == 0) {
                        System.out.printf("progress: 3rd quadrant: current position (%d,%d)%n", fuPosCol, fuPosRow);
                    }
                }
                boolean isDiagonal = Math.abs(fuPosCol) == Math.abs(fuPosRow);
                mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
                if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                    } else if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow); // (c>0,+-r)->(c-1,+-r) | (c<0,+-r)->(c+1,+-r) - one step along COL axis towards origin
                    } else {
                        mapPosParent = new Pair<>(fuPosCol, fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (+-c,r>0)->(+-c,r-1) | (+-c,r<0)->(+-c,r+1) - one step along ROW axis towards origin
                    }
                    floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParentTowardsDiagonal);
                    floodUnitParent = floodUnitMap.get(new Pair<>(fuPosCol + 1, fuPosRow)); // TODO: QUADRANT DEPENDENT
                    boolean parentTowardsDiagonalIsDiagonal = Math.abs(mapPosParent.getValue0()) == Math.abs(mapPosParent.getValue1());
                    floodUnit = (isDiagonal || !parentTowardsDiagonalIsDiagonal) && floodUnitParentTowardsDiagonal.isStable ? floodUnitParentTowardsDiagonal.floodStable() : gardenGrid.floodAdjacentMapLeftUp(floodUnitPrev, floodUnitParent, (isDiagonal || !parentTowardsDiagonalIsDiagonal) ? floodUnitParentTowardsDiagonal : null);
                    cycleFloodsAdded++;
                    allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    floodUnitPrev = floodUnitParent;
                } else if (workPhase == WorkPhase.FindCycle) {
                    floodUnit = floodUnitMap.get(mapPosCurrent);
                    int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset) * (isDiagonal ? 2 : 1); // going along diagonal is actually 2 cycles move
                    if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                    if (simulationPhase1_MinCycles > thisMinCycle) {
                        simulationPhase1_MinCycles = thisMinCycle;
                    }
                } else if (workPhase == WorkPhase.Skip) {
                    int floodCounter = 0;
                    int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol - deltaCol, fuPosRow - deltaRow); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                        floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                        floodCounter = 1;
                    } else {
                        // go along diagonal until row/col (depending on octant) of current is hit
                        int fuPosColDiagonalStop, fuPosRowDiagonalStop;
                        if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                            fuPosColDiagonalStop = Integer.MAX_VALUE;
                            fuPosRowDiagonalStop = fuPosRow;
                        } else {
                            fuPosColDiagonalStop = fuPosCol;
                            fuPosRowDiagonalStop = Integer.MAX_VALUE;
                        }
                        int fuPosDiagonalCol = 0, fuPosDiagonalRow = 0;
                        while (Math.abs(fuPosDiagonalCol) < Math.abs(fuPosColDiagonalStop) && Math.abs(fuPosDiagonalRow) < Math.abs(fuPosRowDiagonalStop)) {
                            fuPosDiagonalCol += deltaCol;
                            fuPosDiagonalRow += deltaRow;
                        }
                        if (Math.abs(fuPosDiagonalCol) + Math.abs(fuPosDiagonalRow) < minCycleStart) {
                            // if current position row/col hits last cycle below middle point then jump to border of the last cycle
                            if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                                mapPosParent = new Pair<>(deltaCol * (minCycleStart - Math.abs(fuPosDiagonalCol)), fuPosRow);
                            } else {
                                mapPosParent = new Pair<>(fuPosCol, deltaRow * (minCycleStart - Math.abs(fuPosDiagonalRow)));
                            }
                            floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                            floodCounter = simulationPhase1_MinCycles - minCycleStart;
                        } else {
                            mapPosParent = new Pair<>(fuPosDiagonalCol, fuPosDiagonalRow);
                            if (!floodUnitMap.containsKey(mapPosParent)) {
                                // if current position row/col hits diagonal above last cycle then create this diagonal flood and store on the map
                                GardenGrid.FloodUnit floodUnitDiagonalPrev = floodUnitMap.get(new Pair<>(fuPosDiagonalCol - deltaCol, fuPosDiagonalRow - deltaRow));
                                floodUnitParentTowardsDiagonal = floodUnitDiagonalPrev.floodStable();
                                floodUnitMap.put(mapPosParent, floodUnitParentTowardsDiagonal);
                                if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitDiagonalPrev);
                            } else {
                                floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                            }
                            // create flood based on diagonal towards current position (hack: uses half of stable flood offset instead of intermediate floods, diagonal stable flood is double the axis-parallel stable flood)
                            floodUnitParent = floodUnitParentTowardsDiagonal.floodStableHalf();
                            mapPosParent = new Pair<>(fuPosDiagonalCol + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? deltaCol : 0), fuPosDiagonalRow + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? 0 : deltaRow));
                            floodUnitMap.put(mapPosParent, floodUnitParent);
                            if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitParentTowardsDiagonal);
                            floodCounter = simulationPhase1_MinCycles - (Math.abs(mapPosParent.getValue0()) + Math.abs(mapPosParent.getValue1()));
                        }
                    }
                    floodUnit = floodUnitParent.floodStable(floodCounter);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                    cycleFloodsAdded++;
                    // allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    // floodUnitPrev = floodUnitParent;
                } else {
                    throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
                }
            }
            // 7. at pos (0,row<0): flood up
            mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
            if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                mapPosParent = new Pair<>(fuPosCol, fuPosRow + 1); // TODO: QUADRANT DEPENDENT
                floodUnitParent = floodUnitMap.get(mapPosParent);
                if (floodUnitParent.isStable) {
                    floodUnit = floodUnitParent.floodStable(); // if parent is stable just offset current flood using parent
                } else {
                    floodUnit = gardenGrid.floodAdjacentMapUp(floodUnitParent); // generate new flood using parent
                    if (floodUnit.isStable) {
                        // if generated flood is stable, it means that parent is also stable, update parent and recreate current flood using stable parent
                        floodUnitParent = new GardenGrid.FloodUnit(floodUnitParent.floodedGrid, floodUnitParent.minSteps, floodUnitParent.maxSteps, true, floodUnit.stableOffset, 0);
                        floodUnitMap.put(mapPosParent, floodUnitParent);
                        floodUnit = floodUnitParent.floodStable();
                    }
                }
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                floodUnitPrev = floodUnitParent;
            } else if (workPhase == WorkPhase.FindCycle) {
                floodUnit = floodUnitMap.get(mapPosCurrent);
                int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset);
                if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                if (simulationPhase1_MinCycles > thisMinCycle) {
                    simulationPhase1_MinCycles = thisMinCycle;
                }
            } else if (workPhase == WorkPhase.Skip) {
                int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                // search along axis for first missing flood, parent will be right before it
                int fuPosParentCol = 0, fuPosParentRow = 0;
                Pair<Integer, Integer> mapPosParentNext;
                mapPosParent = null;
                while (floodUnitMap.containsKey(mapPosParentNext = new Pair<>(fuPosParentCol, fuPosParentRow))) {
                    mapPosParent = mapPosParentNext;
                    fuPosParentCol += deltaCol;
                    fuPosParentRow += deltaRow;
                }
                floodUnitParent = floodUnitMap.get(mapPosParent);
                floodUnit = floodUnitParent.floodStable(Math.max(Math.abs(fuPosCol - mapPosParent.getValue0()), Math.abs(fuPosRow - mapPosParent.getValue1()))); // parent is stable, just offset current flood using parent
                if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                cycleFloodsAdded++;
                // allFloodsInCycleStable &= floodUnit.isStable;
                mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                floodUnitMap.put(mapPosCurrent, floodUnit);
                // floodUnitPrev = floodUnitParent;
            } else {
                throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
            }
            // 8. go row+1,col+1 flooding diagonally until row==0 with position (col>0,0)
            while (fuPosRow < 0) {
                fuPosCol++; // TODO: QUADRANT DEPENDENT
                fuPosRow++;
                if (fuPosRow == 0) {
                    break;
                }
                if (logging) if (cycles > 200_000) {
                    if ((Math.abs(fuPosCol)) % 10_000 == 0) {
                        System.out.printf("progress: 4th quadrant: current position (%d,%d)%n", fuPosCol, fuPosRow);
                    }
                }
                boolean isDiagonal = Math.abs(fuPosCol) == Math.abs(fuPosRow);
                mapPosCurrent = new Pair<>(fuPosCol, fuPosRow);
                if (workPhase == WorkPhase.Intro || workPhase == WorkPhase.Intro2ndCircle) {
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                    } else if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                        mapPosParent = new Pair<>(fuPosCol + (fuPosCol < 0 ? 1 : -1), fuPosRow); // (c>0,+-r)->(c-1,+-r) | (c<0,+-r)->(c+1,+-r) - one step along COL axis towards origin
                    } else {
                        mapPosParent = new Pair<>(fuPosCol, fuPosRow + (fuPosRow < 0 ? 1 : -1)); // (+-c,r>0)->(+-c,r-1) | (+-c,r<0)->(+-c,r+1) - one step along ROW axis towards origin
                    }
                    floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParentTowardsDiagonal);
                    floodUnitParent = floodUnitMap.get(new Pair<>(fuPosCol, fuPosRow + 1)); // TODO: QUADRANT DEPENDENT
                    boolean parentTowardsDiagonalIsDiagonal = Math.abs(mapPosParent.getValue0()) == Math.abs(mapPosParent.getValue1());
                    floodUnit = (isDiagonal || !parentTowardsDiagonalIsDiagonal) && floodUnitParentTowardsDiagonal.isStable ? floodUnitParentTowardsDiagonal.floodStable() : gardenGrid.floodAdjacentMapRightUp(floodUnitParent, floodUnitPrev, (isDiagonal || !parentTowardsDiagonalIsDiagonal) ? floodUnitParentTowardsDiagonal : null);
                    cycleFloodsAdded++;
                    allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    floodUnitPrev = floodUnitParent;
                } else if (workPhase == WorkPhase.FindCycle) {
                    floodUnit = floodUnitMap.get(mapPosCurrent);
                    int thisMinCycle = (int)Math.floor((double)(maxSteps - floodUnit.minSteps) / floodUnit.stableOffset) * (isDiagonal ? 2 : 1); // going along diagonal is actually 2 cycles move
                    if (false) if (logging) System.out.printf("simulation phase %d: min cycle %d%n", workPhase.ordinal(), thisMinCycle);
                    if (simulationPhase1_MinCycles > thisMinCycle) {
                        simulationPhase1_MinCycles = thisMinCycle;
                    }
                } else if (workPhase == WorkPhase.Skip) {
                    int floodCounter = 0;
                    int deltaCol = fuPosCol == 0 ? 0 : (fuPosCol < 0 ? -1 : 1), deltaRow = fuPosRow == 0 ? 0 : (fuPosRow < 0 ? -1 : 1);
                    if (isDiagonal) {
                        mapPosParent = new Pair<>(fuPosCol - deltaCol, fuPosRow - deltaRow); // (c,r)->(c+-1,r+-1) - diagonally towards origin
                        floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                        floodCounter = 1;
                    } else {
                        // go along diagonal until row/col (depending on octant) of current is hit
                        int fuPosColDiagonalStop, fuPosRowDiagonalStop;
                        if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                            fuPosColDiagonalStop = Integer.MAX_VALUE;
                            fuPosRowDiagonalStop = fuPosRow;
                        } else {
                            fuPosColDiagonalStop = fuPosCol;
                            fuPosRowDiagonalStop = Integer.MAX_VALUE;
                        }
                        int fuPosDiagonalCol = 0, fuPosDiagonalRow = 0;
                        while (Math.abs(fuPosDiagonalCol) < Math.abs(fuPosColDiagonalStop) && Math.abs(fuPosDiagonalRow) < Math.abs(fuPosRowDiagonalStop)) {
                            fuPosDiagonalCol += deltaCol;
                            fuPosDiagonalRow += deltaRow;
                        }
                        if (Math.abs(fuPosDiagonalCol) + Math.abs(fuPosDiagonalRow) < minCycleStart) {
                            // if current position row/col hits last cycle below middle point then jump to border of the last cycle
                            if (Math.abs(fuPosCol) > Math.abs(fuPosRow)) {
                                mapPosParent = new Pair<>(deltaCol * (minCycleStart - Math.abs(fuPosDiagonalCol)), fuPosRow);
                            } else {
                                mapPosParent = new Pair<>(fuPosCol, deltaRow * (minCycleStart - Math.abs(fuPosDiagonalRow)));
                            }
                            floodUnitParent = floodUnitMap.get(mapPosParent); // it was created during first octant sweep of this quadrant
                            floodCounter = simulationPhase1_MinCycles - minCycleStart;
                        } else {
                            mapPosParent = new Pair<>(fuPosDiagonalCol, fuPosDiagonalRow);
                            if (!floodUnitMap.containsKey(mapPosParent)) {
                                // if current position row/col hits diagonal above last cycle then create this diagonal flood and store on the map
                                GardenGrid.FloodUnit floodUnitDiagonalPrev = floodUnitMap.get(new Pair<>(fuPosDiagonalCol - deltaCol, fuPosDiagonalRow - deltaRow));
                                floodUnitParentTowardsDiagonal = floodUnitDiagonalPrev.floodStable();
                                floodUnitMap.put(mapPosParent, floodUnitParentTowardsDiagonal);
                                if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitDiagonalPrev);
                            } else {
                                floodUnitParentTowardsDiagonal = floodUnitMap.get(mapPosParent);
                            }
                            // create flood based on diagonal towards current position (hack: uses half of stable flood offset instead of intermediate floods, diagonal stable flood is double the axis-parallel stable flood)
                            floodUnitParent = floodUnitParentTowardsDiagonal.floodStableHalf();
                            mapPosParent = new Pair<>(fuPosDiagonalCol + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? deltaCol : 0), fuPosDiagonalRow + (Math.abs(fuPosCol) > Math.abs(fuPosRow) ? 0 : deltaRow));
                            floodUnitMap.put(mapPosParent, floodUnitParent);
                            if (storeParentFloods) floodUnitParents.put(mapPosParent, floodUnitParentTowardsDiagonal);
                            floodCounter = simulationPhase1_MinCycles - (Math.abs(mapPosParent.getValue0()) + Math.abs(mapPosParent.getValue1()));
                        }
                    }
                    floodUnit = floodUnitParent.floodStable(floodCounter);
                    if (storeParentFloods) floodUnitParents.put(mapPosCurrent, floodUnitParent);
                    cycleFloodsAdded++;
                    // allFloodsInCycleStable &= floodUnit.isStable;
                    mapReadyForMaxSteps &= !floodUnit.isAchievableIn(maxSteps);
                    plotCountReachableInSteps += gardenGrid.getPlotCountReachableInSteps(maxSteps, floodUnit);
                    floodUnitMap.put(mapPosCurrent, floodUnit);
                    // floodUnitPrev = floodUnitParent;
                } else {
                    throw new IllegalStateException("simulation phase %d not implemented".formatted(workPhase.ordinal()));
                }
            }
            // 9. advance right to next circle
            fuPosCol++;
            // goto 1.
            cycleStable |= allFloodsInCycleStable;
            cyclePlotCountAdded = plotCountReachableInSteps - cyclePlotCountAdded;
            if (logging) System.out.printf("cycle %d: floods added %d, plot count increase %d, count/flood %f%n", cycles, cycleFloodsAdded, cyclePlotCountAdded, (double)cyclePlotCountAdded/cycleFloodsAdded);
        }
        return new ReturnData(maxSteps, plotCountReachableInSteps, floodUnitMap, floodUnitParents, cycles, !doNotOptimize);
    }

    private void analysis(ReturnData data) {
        int maxSteps = data.maxSteps;
        long plotCountReachableInSteps = data.plotCountReachableInSteps;
        Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitMap = data.floodUnitMap;
        Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitParents = data.floodUnitParents;
        long cycles = data.cycles;
        if (true) {
            System.out.printf("max steps %d: cycles %d, plot count %d%n", maxSteps, cycles, plotCountReachableInSteps);
        }
        String filenamePart = "%s_%dx%d".formatted(getInputSuffix(), gardenGrid.getWidth(), gardenGrid.getHeight());
        if (false) {
            String charMap = analysis_DumpFloodUnitMapAsTopLevelMap(floodUnitMap, maxSteps);
            if (true) analysis_SaveToFile("%s_charmap_%d".formatted(filenamePart, maxSteps), charMap);
            if (true) System.out.println(charMap);
        }
        if (false) {
            // dump view of whole garden grid
            String[] dumpTypeName = new String[] {""};
            String gardenForMaxSteps = analysis_DumpFloodUnitMapAsUnifiedMap(floodUnitMap, (coords, fu) -> {
                if (true) {
                    dumpTypeName[0] = "flood";
                    return gardenGrid.debugFloodedGrid(fu, maxSteps).toLines(); // flood view limited to max steps
                } else {
                    dumpTypeName[0] = "diff";
                    // flood next-parent difference view no limited to max steps
                    if (floodUnitParents.containsKey(coords)) {
                        return gardenGrid.debugFloodedGrid(gardenGrid.floodDifference(floodUnitParents.get(coords), fu), Integer.MAX_VALUE).toLines();
                    } else {
                        return gardenGrid.debugFloodedGrid(fu, Integer.MAX_VALUE).toLines();
                    }
                }
            });
            if (dumpTypeName[0].equals("diff") && floodUnitParents.isEmpty()) {
                System.out.println("WARNING: parents missing!");
            }
            if (true) analysis_SaveToFile("%s_%s_maxsteps_%d".formatted(filenamePart, dumpTypeName[0], maxSteps), gardenForMaxSteps);
            if (false) System.out.println(gardenForMaxSteps);
        }
        if (false) { // verify patchwork using flood on unified map
            if (data.optimized) {
                // flood map has holes, cannot restore unified map
                ReturnData dataNonOptimized = getPlotCountReachableInSteps(maxSteps, false, true, false);
                floodUnitMap = dataNonOptimized.floodUnitMap;
            }
            String gardenForMaxSteps = analysis_DumpFloodUnitMapAsUnifiedMap(floodUnitMap, (coors, fu) ->
                    gardenGrid.createInputFromFloodedGrid(fu).toLines() // creates input file for testing
            );
            // create input for testing
            if (false) analysis_SaveToFile("%s_input_maxsteps_%d".formatted(filenamePart, maxSteps), gardenForMaxSteps);
            // verify using pure flood on whole garden grid created using patchwork
            GardenGrid ggVerify = new GardenGrid(Arrays.stream(gardenForMaxSteps.split("\\r\\n")).toList(), "", s -> s.charAt(0));
            GardenGrid.FloodUnit floodVerify = ggVerify.flood();
            if (false) {
                System.out.println("verification: pure flood");
                String dumpVerify = String.join("\r\n", ggVerify.debugFloodedGrid(floodVerify.floodedGrid, maxSteps).toLines());
                System.out.println(dumpVerify);
                if (true) analysis_SaveToFile("%s_verify_maxsteps_%d".formatted(filenamePart, maxSteps), dumpVerify);
            }
            long resultVerify = ggVerify.getPlotCountReachableInSteps(maxSteps, floodVerify);
            if (plotCountReachableInSteps != resultVerify) {
                throw new IllegalStateException("result mismatch, %d patchwork, %d pure flood".formatted(plotCountReachableInSteps, resultVerify));
            }
        }
    }

    private void analysis_SaveToFile(String analysisSuffix, String gardenForMaxSteps) {
        createTestFile(analysisSuffix, writer -> {
            try {
                writer.append(gardenForMaxSteps);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> MinMaxBounds2D analysis_GetMinMaxBoundsFromMapOfEuclideanSpace(Map<Pair<Integer, Integer>, T> floodUnitMap) {
        // get map min/max coords
        MinMaxBounds2D minMax = new MinMaxBounds2D();
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        for (var key : floodUnitMap.keySet()) {
            minMax.accX(key.getValue0());
            minMax.accY(key.getValue1());
            // minCol = Math.min(minCol, key.getValue0());
            // maxCol = Math.max(maxCol, key.getValue0());
            // minRow = Math.min(minRow, key.getValue1());
            // maxRow = Math.max(maxRow, key.getValue1());
        }
        // return new MinMaxBounds(minCol, maxCol, minRow, maxRow);
        return minMax;
    }
    private String analysis_DumpFloodUnitMapAsTopLevelMap(Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitMap, int maxSteps) {
        // get map min/max coords
        MinMaxBounds2D mapBounds = analysis_GetMinMaxBoundsFromMapOfEuclideanSpace(floodUnitMap);
        if (mapBounds.getXRange() == 0 || mapBounds.getYRange() == 0) return "";
        // convert from map to grid (of chars)
        var charGrid = new Grid<Character>(mapBounds.getXRange(), mapBounds.getYRange(), '.', "");
        for (var e : floodUnitMap.entrySet()) {
            GardenGrid.FloodUnit mapVal = e.getValue();
            boolean isAchievable = mapVal.isAchievableIn(maxSteps);
            boolean isFullyCovered = mapVal.isFullyCoveredBy(maxSteps);
            int chIdx = !isAchievable ? 0 : !isFullyCovered ? 1 : 2;
            char ch = mapVal.isStable ? (mapVal.stableCounter == 0 ? ":sS" : "|pP").charAt(chIdx) : "-+*".charAt(chIdx);
            charGrid.set(e.getKey().getValue0() - mapBounds.getMinX(), e.getKey().getValue1() - mapBounds.getMinY(), ch);
        }
        return charGrid.toString();
    }
    private String analysis_DumpFloodUnitMapAsUnifiedMap(
            Map<Pair<Integer, Integer>, GardenGrid.FloodUnit> floodUnitMap,
            BiFunction<Pair<Integer, Integer>, GardenGrid.FloodUnit, List<String>> floodUnitToLines) {
        // get map min/max coords
        MinMaxBounds2D mapBounds = analysis_GetMinMaxBoundsFromMapOfEuclideanSpace(floodUnitMap);
        // convert from map to grid (of floods)
        GardenGrid.FloodUnit floodUnitAny = floodUnitMap.values().stream().findFirst().get();
        var floodUnitGrid = new Grid<GardenGrid.FloodUnit>(mapBounds.getXRange(), mapBounds.getYRange(), null, floodUnitAny.getClass(), "");
        for (var e : floodUnitMap.entrySet()) {
            floodUnitGrid.set(e.getKey().getValue0() - mapBounds.getMinX(), e.getKey().getValue1() - mapBounds.getMinY(), e.getValue());
        }
        // convert from grid of floods to grid of list of strings
        var charGridEmpty = new Grid<>(floodUnitAny.floodedGrid.getWidth(), floodUnitAny.floodedGrid.getHeight(), '.', "");
        List<String> emptyGridLines = charGridEmpty.toLines();
        int finalMinCol = mapBounds.getMinX(), finalMinRow = mapBounds.getMinY();
        BiFunction<Pair<Integer, Integer>, GardenGrid.FloodUnit, List<String>> mapFUtoLofS = (coords, gridFU) -> {
            if (gridFU == null) {
                return emptyGridLines;
            } else {
                // convert grid coords to map coords
                var mapCoords = new Pair<>(finalMinCol + coords.getValue0(), finalMinRow + coords.getValue1());
                return floodUnitToLines.apply(mapCoords, gridFU);
            }
        };
        Grid<List<String>> listStrGrid = floodUnitGrid.map(mapFUtoLofS, emptyGridLines.getClass());
        // convert from grid of list of string to list of string
        List<String> acc = new ArrayList<>();
        for (int row = 0; row < listStrGrid.getHeight(); row++) {
            List<List<String>> accRow = null;
            for (int col = 0; col < listStrGrid.getWidth(); col++) {
                List<String> lines = listStrGrid.get(col, row);
                if (accRow == null) {
                    accRow = new ArrayList<>(lines.size());
                    for (int li = 0; li < lines.size(); li++) {
                        accRow.add(new ArrayList<>(listStrGrid.getWidth()));
                    }
                }
                for (int li = 0; li < lines.size(); li++) {
                    accRow.get(li).add(lines.get(li));
                }
            }
            acc.addAll(accRow.stream().map(list -> String.join("", list)).toList());
        }
        // convert from list of strings to one string
        String dump = String.join("\r\n", acc);
        // System.out.println(dump);
        return dump;
    }

    private void analysis_GoingInOneDirection_StopWhenStable(GardenGrid gardenGrid, Function<Grid<Integer>, GardenGrid.FloodUnit> getAdjacent) {
        GardenGrid.FloodUnit flooded = gardenGrid.flood();
        Grid<Character> debugFlooded = gardenGrid.debugFloodedGrid(flooded.floodedGrid, Integer.MAX_VALUE);
        System.out.println(debugFlooded);
        System.out.println();
        int diffHashCode = 0;
        for (int i = 0; i < 100; i++) {
            GardenGrid.FloodUnit floodedAdjacent = getAdjacent.apply(flooded.floodedGrid);
            Grid<Character> debugFloodedAdjacent = gardenGrid.debugFloodedGrid(floodedAdjacent.floodedGrid, Integer.MAX_VALUE);
            System.out.println(debugFloodedAdjacent);
            System.out.println();
            Grid<Integer> floodedDifference = gardenGrid.floodDifference(flooded.floodedGrid, floodedAdjacent.floodedGrid);
            Grid<Character> debugFloodedDifference = gardenGrid.debugFloodedGrid(floodedDifference, Integer.MAX_VALUE);
            System.out.println(debugFloodedDifference);
            System.out.printf("--- %d ---%n", i + 1);
            int newDiffHashCode = debugFloodedDifference.hashCode();
            if (diffHashCode == newDiffHashCode) {
                // if truly equal...
                System.out.println("stable");
                System.out.println(debugFloodedAdjacent);
                System.out.println();
                System.out.println(debugFloodedDifference);
                break;
            } else {
                diffHashCode = newDiffHashCode;
            }
            flooded = floodedAdjacent;
            // detect difference is constant
        }
    }

    public static class Day21Test {
        @Test
        void knownBugs() {
            var day = new Day21("_sample_twohead_1x1maps");
            day.parsePart2();
            assertEquals(3839L, day.getPlotCountReachableInSteps(94, false, false, false));
            assertEquals(3841L, day.getPlotCountReachableInSteps(94, false, true, false)); // wrong fully covered cycle chosen - 7 instead of 6
        }

        @Test
        void knownGoodInputs() {
            var day = new Day21("_sample_empty_1x1maps");
            day.parsePart2();
            assertEquals(1002001L, day.getPlotCountReachableInSteps(1000));
        }

        @Test
        void solvePart1_small() {
            var day = new Day21("_sample_1x1maps");
            day.parsePart1();
            assertEquals(16L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day21("");
            day.parsePart1();
            assertEquals(3615L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day21("_sample_1x1maps");
            day.parsePart2();
            assertEquals(16733044L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day21("");
            day.parsePart2();
            assertEquals(602259568764234L, day.solvePart2());
        }
    }
}
/*

--- Day 21: Step Counter ---

You manage to catch the airship right as it's dropping someone else off on their all-expenses-paid trip to Desert Island! It even helpfully drops you off near the gardener and his massive farm.

"You got the sand flowing again! Great work! Now we just need to wait until we have enough sand to filter the water for Snow Island and we'll have snow again in no time."

While you wait, one of the Elves that works with the gardener heard how good you are at solving problems and would like your help. He needs to get his steps in for the day, and so he'd like to know which garden plots he can reach with exactly his remaining 64 steps.

He gives you an up-to-date map (your puzzle input) of his starting position (S), garden plots (.), and rocks (#). For example:

...........
.....###.#.
.###.##..#.
..#.#...#..
....#.#....
.##..S####.
.##..#...#.
.......##..
.##.#.####.
.##..##.##.
...........

The Elf starts at the starting position (S) which also counts as a garden plot. Then, he can take one step north, south, east, or west, but only onto tiles that are garden plots. This would allow him to reach any of the tiles marked O:

...........
.....###.#.
.###.##..#.
..#.#...#..
....#O#....
.##.OS####.
.##..#...#.
.......##..
.##.#.####.
.##..##.##.
...........

Then, he takes a second step. Since at this point he could be at either tile marked O, his second step would allow him to reach any garden plot that is one step north, south, east, or west of any tile that he could have reached after the first step:

...........
.....###.#.
.###.##..#.
..#.#O..#..
....#.#....
.##O.O####.
.##.O#...#.
.......##..
.##.#.####.
.##..##.##.
...........

After two steps, he could be at any of the tiles marked O above, including the starting position (either by going north-then-south or by going west-then-east).

A single third step leads to even more possibilities:

...........
.....###.#.
.###.##..#.
..#.#.O.#..
...O#O#....
.##.OS####.
.##O.#...#.
....O..##..
.##.#.####.
.##..##.##.
...........

He will continue like this until his steps for the day have been exhausted. After a total of 6 steps, he could reach any of the garden plots marked O:

...........
.....###.#.
.###.##.O#.
.O#O#O.O#..
O.O.#.#.O..
.##O.O####.
.##.O#O..#.
.O.O.O.##..
.##.#.####.
.##O.##.##.
...........

In this example, if the Elf's goal was to get exactly 6 more steps today, he could use them to reach any of 16 garden plots.

However, the Elf actually needs to get 64 steps today, and the map he's handed you is much larger than the example map.

Starting from the garden plot marked S on your map, how many garden plots could the Elf reach in exactly 64 steps?

Your puzzle answer was 3615.

--- Part Two ---

The Elf seems confused by your answer until he realizes his mistake: he was reading from a list of his favorite numbers that are both perfect squares and perfect cubes, not his step counter.

The actual number of steps he needs to get today is exactly 26501365.

He also points out that the garden plots and rocks are set up so that the map repeats infinitely in every direction.

So, if you were to look one additional map-width or map-height out from the edge of the example map above, you would find that it keeps repeating:

.................................
.....###.#......###.#......###.#.
.###.##..#..###.##..#..###.##..#.
..#.#...#....#.#...#....#.#...#..
....#.#........#.#........#.#....
.##...####..##...####..##...####.
.##..#...#..##..#...#..##..#...#.
.......##.........##.........##..
.##.#.####..##.#.####..##.#.####.
.##..##.##..##..##.##..##..##.##.
.................................
.................................
.....###.#......###.#......###.#.
.###.##..#..###.##..#..###.##..#.
..#.#...#....#.#...#....#.#...#..
....#.#........#.#........#.#....
.##...####..##..S####..##...####.
.##..#...#..##..#...#..##..#...#.
.......##.........##.........##..
.##.#.####..##.#.####..##.#.####.
.##..##.##..##..##.##..##..##.##.
.................................
.................................
.....###.#......###.#......###.#.
.###.##..#..###.##..#..###.##..#.
..#.#...#....#.#...#....#.#...#..
....#.#........#.#........#.#....
.##...####..##...####..##...####.
.##..#...#..##..#...#..##..#...#.
.......##.........##.........##..
.##.#.####..##.#.####..##.#.####.
.##..##.##..##..##.##..##..##.##.
.................................

This is just a tiny three-map-by-three-map slice of the inexplicably-infinite farm layout; garden plots and rocks repeat as far as you can see. The Elf still starts on the one middle tile marked S, though - every other repeated S is replaced with a normal garden plot (.).

Here are the number of reachable garden plots in this new infinite version of the example map for different numbers of steps:

    In exactly 6 steps, he can still reach 16 garden plots.
    In exactly 10 steps, he can reach any of 50 garden plots.
    In exactly 50 steps, he can reach 1594 garden plots.
    In exactly 100 steps, he can reach 6536 garden plots.
    In exactly 500 steps, he can reach 167004 garden plots.
    In exactly 1000 steps, he can reach 668697 garden plots.
    In exactly 5000 steps, he can reach 16733044 garden plots.

However, the step count the Elf needs is much larger! Starting from the garden plot marked S on your infinite map, how many garden plots could the Elf reach in exactly 26501365 steps?

Your puzzle answer was 602259568764234.

 */