package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day13 extends Day {
    private final boolean doEnlargeInput;
    private final int enlargeToMinSide;

    public Day13(String inputSuffix) {
        this(inputSuffix, false, 0, 0L);
    }

    public Day13(String inputSuffix, boolean doEnlargeInput) {
        this(inputSuffix, doEnlargeInput, 1000, 0L); // const RNG seed for repeatability
    }

    private Day13(String inputSuffix, boolean doEnlargeInput, int enlargeToMinSide, long rngSeed) {
        super(inputSuffix);
        this.doEnlargeInput = doEnlargeInput;
        this.enlargeToMinSide = enlargeToMinSide;
        rng = new Random(rngSeed);
    }

    public static void main(String[] args) {
        // uses const RNG seed for repeatability for speed tests
        new Day13("_small", false).run(); // _small, _small2
    }

    /** Cache for results of finding mirrors without smudge, will be used in finding mirrors with smudge later */
    private final Map<List<String>, Long> resultsMirrors = new HashMap<>();

    /** Cache for results of finding distances between strings, often repeated for the same strings */
    private final Map<Pair<String, String>, Integer> distancesStrings = new HashMap<>();

    private final Random rng;

    private final class Pattern {
        private final List<String> lines;
        private int width, height;

        public Pattern() {
            lines = new ArrayList<>();
            width = height = 0;
        }

        private Pattern(List<String> lines) {
            this.lines = lines;
            height = lines.size();
            width = lines.getFirst().length();
            IntStream.range(1, height).forEach(i -> {
                if (lines.get(i).length() != width) {
                    throw new IllegalArgumentException("line length mismatch");
                }
            });
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int minSide() { return Math.min(width, height); }
        public int maxSide() { return Math.max(width, height); }

        public void add(String line) {
            if (!lines.isEmpty()) {
                if (width != line.length()) {
                    throw new IllegalArgumentException("line length mismatch");
                }
            } else {
                width = line.length();
            }
            lines.add(line);
            height++;
        }

        public long findMirrors() {
            long result;
            result = resultsMirrors.getOrDefault(lines, -1L);
            if (result >= 0) {
                return result;
            }
            // int[] hashes = lines.stream().mapToInt(String::hashCode).toArray(); // "optimization" that slows down by 50% !
            AtomicInteger mirrorCount = new AtomicInteger(0);
            AtomicLong acc = new AtomicLong(0L);
            IntStream.range(0, lines.size() - 1).forEach(i -> {
                if (/*hashes[i] == hashes[i + 1] && */lines.get(i).equals(lines.get(i + 1))) {
                    boolean isM = true;
                    for (int j = 0; j < Math.min(i, lines.size() - (i + 1) - 1); j++) {
                        int prevLineIdx = i - 1 - j, nextLineIdx = (i + 1) + 1 + j;
                        if (/*hashes[prevLineIdx] != hashes[nextLineIdx] || */!lines.get(prevLineIdx).equals(lines.get(nextLineIdx))) {
                            isM = false;
                            break;
                        }
                    }
                    if (isM) {
                        acc.addAndGet(i + 1);
                        mirrorCount.incrementAndGet();
                    }
                }
            });
            result = acc.get();
            if (result == 0) {
                // System.out.println("no mirrors"); // true for problem input
            } else if (mirrorCount.get() > 1) {
                // System.out.println("more than one mirror"); // false for problem input
            }
            resultsMirrors.put(lines, result);
            return result;
        }

        private int stringDistance(String a, String b) {
            Pair<String, String> abPair = new Pair<>(a, b);
            int result;
            result = distancesStrings.getOrDefault(abPair, -1);
            if (result >= 0) {
                return result;
            }
            int distance = 0;
            for (int i = 0; i < a.length(); i++) {
                if (a.charAt(i) != b.charAt(i)) {
                    distance++;
                }
            }
            distancesStrings.put(abPair, distance);
            return distance;
        }

        public long findMirrorsWithSmudge() {
            long mirrorResultWithoutSmudge = findMirrors(); // 0 if not found
            AtomicInteger mirrorCount = new AtomicInteger(0);
            AtomicLong acc = new AtomicLong(0L);
            IntStream.range(0, lines.size() - 1).forEach(i -> {
                int mirrorPairDistance = stringDistance(lines.get(i), lines.get(i + 1));
                if (mirrorPairDistance < 2) {
                    // if 0 then we can compare pairwise checking for smudge
                    // if 1 then we can compare pairwise normally, there is only one smudge per mirror
                    boolean isMirror = true, isSmudgeFound = false;
                    for (int j = 0; j < Math.min(i, lines.size() - (i + 1) - 1); j++) {
                        int prevLineIdx = i - 1 - j, nextLineIdx = (i + 1) + 1 + j;
                        int pairDistance = stringDistance(lines.get(prevLineIdx), lines.get(nextLineIdx));
                        if (mirrorPairDistance == 0 && (!isSmudgeFound && pairDistance > 1 || isSmudgeFound && pairDistance > 0) || mirrorPairDistance == 1 && pairDistance > 0) {
                            isMirror = false;
                            break;
                        }
                        if (mirrorPairDistance == 0 && pairDistance == 1) {
                            isSmudgeFound = true; // only one smudge per pattern allowed
                        }
                    }
                    if (isMirror) {
                        if (i + 1 == mirrorResultWithoutSmudge) {
                            // System.out.println("skipping mirror found without removing smudge");
                        } else {
                            acc.addAndGet(i + 1);
                            mirrorCount.incrementAndGet();
                        }
                    }
                }
            });
            long result = acc.get();
            if (result == 0) {
                // System.out.println("no mirrors"); // true for problem input
            } else if (mirrorCount.get() > 1) {
                // System.out.println("more than one mirror"); // false for problem input
            }
            return result;
        }

        public Pattern transpose() {
            char[][] tmp = new char[lines.getFirst().length()][];
            IntStream.range(0, tmp.length).forEach(si -> {
                tmp[si] = new char[lines.size()];
                IntStream.range(0, lines.size()).forEach(li -> {
                    tmp[si][li] = lines.get(li).charAt(si);
                });
            });
            return new Pattern(Arrays.stream(tmp).map(chs -> CharBuffer.wrap(chs).toString()).toList());
        }

        public Pattern duplicateHorizontalAxis() {
            return new Pattern(lines.stream().map(line -> line + line).toList());
        }
        public Pattern duplicateVerticalAxis() {
            var duplicatedLines = new ArrayList<>(lines);
            duplicatedLines.addAll(lines);
            return new Pattern(duplicatedLines);
        }

        public Pattern mirrorVertical() {
            return mirror(lines);
        }

        public Pattern mirrorHorizontal() {
            return mirror(transpose().lines).transpose();
        }
        private Pattern mirror(List<String> lines) {
            var mirroredLines = new ArrayList<>(lines);
            Collections.reverse(mirroredLines);
            var newLines = new ArrayList<>(lines);
            newLines.addAll(mirroredLines);
            return new Pattern(newLines);
        }

        public Pattern rotateVertical() {
            return rotate(lines);
        }
        public Pattern rotateHorizontal() {
            return rotate(transpose().lines).transpose();
        }
        private Pattern rotate(List<String> lines) {
            var rotatedLines = new ArrayList<>(lines);
            Collections.rotate(rotatedLines, rng.nextInt(lines.size()));
            return new Pattern(rotatedLines);
        }

        /** Get operations to grow the pattern keeping both sides below given minimum side */
        public Supplier<Pattern>[] getAllOperations(int minSide) {
            if (width < minSide && height < minSide) {
                return new Supplier[] {
                        this::duplicateHorizontalAxis,
                        this::duplicateVerticalAxis,
                        this::mirrorHorizontal,
                        this::mirrorVertical,
                        this::rotateHorizontal,
                        this::rotateVertical,
                        this::transpose
                };
            } else if (width < minSide) {
                return new Supplier[] {
                        this::duplicateHorizontalAxis,
                        // this::duplicateVerticalAxis,
                        this::mirrorHorizontal,
                        // this::mirrorVertical,
                        this::rotateHorizontal,
                        this::rotateVertical,
                        this::transpose
                };
            } else {
                return new Supplier[] {
                        // this::duplicateHorizontalAxis,
                        this::duplicateVerticalAxis,
                        // this::mirrorHorizontal,
                        this::mirrorVertical,
                        this::rotateHorizontal,
                        this::rotateVertical,
                        this::transpose
                };
            }
        }

        public Pattern mutateRandomly(int minSide) {
            // Function<Pattern, Pattern> transpose = Pattern::transpose;
            // Function<Pattern, Pattern> duplicateHorizontalAxis = Pattern::duplicateHorizontalAxis;
            // Function<Pattern, Pattern>[] suppliers = new Function[] {
            //         duplicateHorizontalAxis,
            //
            // };
            Pattern p = this;
            do {
                Supplier<Pattern>[] allOperations = p.getAllOperations(minSide);
                p = allOperations[rng.nextInt(allOperations.length)].get();
            } while (p.minSide() < minSide);
            return p;
        }

        @Override
        public String toString() {
            return String.join("%n".formatted(), lines);
        }

        @Override
        public int hashCode() {
            return lines.hashCode();
        }
    }

    private List<Pattern> patterns, patternsTransposed;

    @Override
    protected void parsePart1() {
        patterns = stream().reduce(
                new ArrayList<>(),
                (list, line) -> {
                    if (line.isEmpty() || list.isEmpty()) {
                        list.add(new Pattern());
                    }
                    if (!line.isEmpty()) {
                        list.getLast().add(line);
                    }
                    return list;
                },
                (list1, list2) -> {
                    throw new IllegalStateException("parallel processing not implemented");
                }
        );
        System.out.printf("patterns count %d%n", patterns.size());
        System.out.printf("patterns hash %d%n", patterns.hashCode());
        IntSummaryStatistics minSideSummaryStatisticsPre = patterns.stream().mapToInt(Pattern::minSide).summaryStatistics();
        System.out.printf("patters min side stats: %s%n", minSideSummaryStatisticsPre);
        IntSummaryStatistics maxSideSummaryStatisticsPre = patterns.stream().mapToInt(Pattern::maxSide).summaryStatistics();
        System.out.printf("patters max side stats: %s%n", maxSideSummaryStatisticsPre);
        if (doEnlargeInput) { // create larger patters using some mirror-preserving operations (duplications, mirrors, rotations, transpose)
            System.out.println("mutating patterns...");
            patterns = patterns.stream()
                    // .parallel()
                    .map(p -> p.mutateRandomly(enlargeToMinSide))
                    .toList()
            ;
            System.out.println("after pattern mutation");
            System.out.printf("patterns hash %d%n", patterns.hashCode());
            IntSummaryStatistics minSideSummaryStatisticsPost = patterns.stream().mapToInt(Pattern::minSide).summaryStatistics();
            System.out.printf("patters min side stats: %s%n", minSideSummaryStatisticsPost);
            IntSummaryStatistics maxSideSummaryStatisticsPost = patterns.stream().mapToInt(Pattern::maxSide).summaryStatistics();
            System.out.printf("patters max side stats: %s%n", maxSideSummaryStatisticsPost);
        }
        // create transposed patterns for vertical test
        patternsTransposed = patterns.stream()
                .parallel()
                .map(Pattern::transpose)
                .toList()
        ;
    }

    @Override
    protected Object solvePart1() {
        // if (true)return 0;
        long resultV = patterns.stream()
                // .parallel()
                // .peek(System.out::println)
                .peek(p -> { // progress
                    int idx = patterns.indexOf(p);
                    if ((idx + 1) % 10 == 0) {
                        System.out.printf("%d/%d%n", idx + 1, patterns.size());
                    }
                })
                .mapToLong(Pattern::findMirrors)
                // .peek(System.out::println)
                .sum()
                ;
        long resultH = patternsTransposed.stream()
                // .parallel()
                // .peek(System.out::println)
                .peek(p -> { // progress
                    int idx = patternsTransposed.indexOf(p);
                    if ((idx + 1) % 10 == 0) {
                        System.out.printf("%d/%d%n", idx + 1, patternsTransposed.size());
                    }
                })
                .mapToLong(Pattern::findMirrors)
                // .peek(System.out::println)
                .sum();
        return 100 * resultV + resultH;
    }

    @Override
    protected void parsePart2() {
    }

    @Override
    protected Object solvePart2() {
        // if (true)return 0;
        long resultV = patterns.stream()
                // .parallel()
                // .peek(System.out::println)
                .peek(p -> { // progress
                    int idx = patterns.indexOf(p);
                    if ((idx + 1) % 10 == 0) {
                        System.out.printf("%d/%d%n", idx + 1, patterns.size());
                    }
                })
                .mapToLong(Pattern::findMirrorsWithSmudge)
                // .peek(System.out::println)
                .sum()
                ;
        long resultH = patternsTransposed.stream()
                // .parallel()
                // .peek(System.out::println)
                .peek(p -> { // progress
                    int idx = patternsTransposed.indexOf(p);
                    if ((idx + 1) % 10 == 0) {
                        System.out.printf("%d/%d%n", idx + 1, patternsTransposed.size());
                    }
                })
                .mapToLong(Pattern::findMirrorsWithSmudge)
                // .peek(System.out::println)
                .sum();
        return 100 * resultV + resultH;
    }

    private Pattern testPattern(List<String> lines) {
        return new Pattern(lines);
    }

    public static class Day13Test {
        void assertPatterResults(long count, long countWithSmudge, Pattern pattern) {
            assertEquals(count, pattern.findMirrors());
            assertEquals(countWithSmudge, pattern.findMirrorsWithSmudge());
        }
        @Test
        void solvePart1_small() {
            var day = new Day13("_small");
            day.parsePart1();
            assertEquals(405L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day13("");
            day.parsePart1();
            assertEquals(33195L, day.solvePart1());
        }
        @Test
        void solvePart2_small() {
            var day = new Day13("_small");
            day.parsePart1();
            day.parsePart2();
            assertEquals(400L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day13("");
            day.parsePart1();
            day.parsePart2();
            assertEquals(31836L, day.solvePart2());
        }

        @Test
        void solvePart1_main_enlarged() {
            var day = new Day13("", true, 1000, 1L);
            day.parsePart1();
            assertEquals(229816144L, day.solvePart1());
        }

        @Test
        void solvePart2_main_enlarged() {
            var day = new Day13("", true, 1000, 1L);
            day.parsePart1();
            day.parsePart2();
            assertEquals(229559540L, day.solvePart2());
        }

        @Test
        void knownGoodInputs() {
            var day = new Day13("__dummy__");
            assertPatterResults(3, 0,
                    day.testPattern(List.of( // no smudges
                            "#....#",
                            ".#..#.",
                            "..##..",
                            "..##..",
                            ".#..#.",
                            "#....#"
                    ))
            );
            assertPatterResults(0, 3,
                    day.testPattern(List.of( // one smudge at the mirror
                            "#....#",
                            ".#..#.",
                            "..###.",
                            "..##..",
                            ".#..#.",
                            "#....#"
                    ))
            );
            assertPatterResults(0, 0,
                    day.testPattern(List.of( // one smudge at the mirror, one in the  reflection
                            "#....#",
                            ".#..##",
                            "..###.",
                            "..##..",
                            ".#..#.",
                            "#....#"
                    ))
            );
            assertPatterResults(0, 0,
                    day.testPattern(List.of( // two smudges one in the reflection (no detection, only one smudge per patter allowed)
                            "##...#",
                            ".#..##",
                            "..##..",
                            "..##..",
                            ".#..#.",
                            "#....#"
                    ))
            );
        }
    }
}
/*

--- Day 13: Point of Incidence ---

With your help, the hot springs team locates an appropriate spring which launches you neatly and precisely up to the edge of Lava Island.

There's just one problem: you don't see any lava.

You do see a lot of ash and igneous rock; there are even what look like gray mountains scattered around. After a while, you make your way to a nearby cluster of mountains only to discover that the valley between them is completely full of large mirrors. Most of the mirrors seem to be aligned in a consistent way; perhaps you should head in that direction?

As you move through the valley of mirrors, you find that several of them have fallen from the large metal frames keeping them in place. The mirrors are extremely flat and shiny, and many of the fallen mirrors have lodged into the ash at strange angles. Because the terrain is all one color, it's hard to tell where it's safe to walk or where you're about to run into a mirror.

You note down the patterns of ash (.) and rocks (#) that you see as you walk (your puzzle input); perhaps by carefully analyzing these patterns, you can figure out where the mirrors are!

For example:

#.##..##.
..#.##.#.
##......#
##......#
..#.##.#.
..##..##.
#.#.##.#.

#...##..#
#....#..#
..##..###
#####.##.
#####.##.
..##..###
#....#..#

To find the reflection in each pattern, you need to find a perfect reflection across either a horizontal line between two rows or across a vertical line between two columns.

In the first pattern, the reflection is across a vertical line between two columns; arrows on each of the two columns point at the line between the columns:

123456789
    ><
#.##..##.
..#.##.#.
##......#
##......#
..#.##.#.
..##..##.
#.#.##.#.
    ><
123456789

In this pattern, the line of reflection is the vertical line between columns 5 and 6. Because the vertical line is not perfectly in the middle of the pattern, part of the pattern (column 1) has nowhere to reflect onto and can be ignored; every other column has a reflected column within the pattern and must match exactly: column 2 matches column 9, column 3 matches 8, 4 matches 7, and 5 matches 6.

The second pattern reflects across a horizontal line instead:

1 #...##..# 1
2 #....#..# 2
3 ..##..### 3
4v#####.##.v4
5^#####.##.^5
6 ..##..### 6
7 #....#..# 7

This pattern reflects across the horizontal line between rows 4 and 5. Row 1 would reflect with a hypothetical row 8, but since that's not in the pattern, row 1 doesn't need to match anything. The remaining rows match: row 2 matches row 7, row 3 matches row 6, and row 4 matches row 5.

To summarize your pattern notes, add up the number of columns to the left of each vertical line of reflection; to that, also add 100 multiplied by the number of rows above each horizontal line of reflection. In the above example, the first pattern's vertical line has 5 columns to its left and the second pattern's horizontal line has 4 rows above it, a total of 405.

Find the line of reflection in each of the patterns in your notes. What number do you get after summarizing all of your notes?

Your puzzle answer was 33195.

--- Part Two ---

You resume walking through the valley of mirrors and - SMACK! - run directly into one. Hopefully nobody was watching, because that must have been pretty embarrassing.

Upon closer inspection, you discover that every mirror has exactly one smudge: exactly one . or # should be the opposite type.

In each pattern, you'll need to locate and fix the smudge that causes a different reflection line to be valid. (The old reflection line won't necessarily continue being valid after the smudge is fixed.)

Here's the above example again:

#.##..##.
..#.##.#.
##......#
##......#
..#.##.#.
..##..##.
#.#.##.#.

#...##..#
#....#..#
..##..###
#####.##.
#####.##.
..##..###
#....#..#

The first pattern's smudge is in the top-left corner. If the top-left # were instead ., it would have a different, horizontal line of reflection:

1 ..##..##. 1
2 ..#.##.#. 2
3v##......#v3
4^##......#^4
5 ..#.##.#. 5
6 ..##..##. 6
7 #.#.##.#. 7

With the smudge in the top-left corner repaired, a new horizontal line of reflection between rows 3 and 4 now exists. Row 7 has no corresponding reflected row and can be ignored, but every other row matches exactly: row 1 matches row 6, row 2 matches row 5, and row 3 matches row 4.

In the second pattern, the smudge can be fixed by changing the fifth symbol on row 2 from . to #:

1v#...##..#v1
2^#...##..#^2
3 ..##..### 3
4 #####.##. 4
5 #####.##. 5
6 ..##..### 6
7 #....#..# 7

Now, the pattern has a different horizontal line of reflection between rows 1 and 2.

Summarize your notes as before, but instead use the new different reflection lines. In this example, the first pattern's new horizontal line has 3 rows above it and the second pattern's new horizontal line has 1 row above it, summarizing to the value 400.

In each pattern, fix the smudge and find the different line of reflection. What number do you get after summarizing the new reflection line in each pattern in your notes?

Your puzzle answer was 31836.

 */