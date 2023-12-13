package dev.aoc.aoc2023;

import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import dev.aoc.common.Day;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day12 extends Day {
    public Day12(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        new Day12("").run(); // _small, _small2, _test_trimming
    }

    private interface ISpringsLineParse {
        String getLine();
        char[] getState();
        int[] getGroups();
        ISpringsLineParse reverse();
        String toStringLong();
    }
    private static class SpringsLineParsePart1 implements ISpringsLineParse {
        private String line;
        private final char[] state;
        private final int[] groups;

        public SpringsLineParsePart1(String line) {
            this.line = line;
            String[] parts = line.split(" ");
            state = parts[0].toCharArray();
            groups = parseGroups(parts[1], line);
        }
        protected static int[] parseGroups(String groupsString, String line) {
            return Arrays.stream(groupsString.split(","))
                    .mapToInt(Integer::parseInt)
                    .peek(g -> {
                        if (g <= 0) {
                            throw new IllegalArgumentException("illegal group value %d, in line '%s'".formatted(g, line));
                        }
                    })
                    .toArray()
                    ;
        }

        @Override
        public ISpringsLineParse reverse() {
            Chars.reverse(state);
            Ints.reverse(groups);
            line = "%s %s".formatted(state, groups);
            return this;
        }

        @Override
        public String getLine() {
            return line;
        }

        @Override
        public char[] getState() {
            return state;
        }

        @Override
        public int[] getGroups() {
            return groups;
        }

        @Override
        public String toStringLong() {
            return "%s[S=%s,G=%s]".formatted(line != null ? "%s%n".formatted(line) : "", Arrays.toString(state), Arrays.toString(groups));
        }
    }
    private static class SpringsLineParsePart2 extends SpringsLineParsePart1 {
        public SpringsLineParsePart2(String line) {
            super(lineConvert(line));
        }
        private static String lineConvert(String line) {
            String[] parts = line.split(" ");
            String dupState = String.join("?", Collections.nCopies(5, parts[0]));
            String dupGroups = String.join(",", Collections.nCopies(5, parts[1]));
            return "%s %s".formatted(dupState, dupGroups);
        }
    }

    private List<SpringsLineParsePart1> springsLineParse1;
    private List<SpringsLineParsePart2> springsLineParse2;

    private static final class SpringsLineSolverContext {
        private final ISpringsLineParse springsLineParse;
        private Map<Pair<Integer, Integer>, Long> resultsCache;
        private final int[] maxGroupLength;
        private final int[] groupsWithSepRestLength;

        public SpringsLineSolverContext(ISpringsLineParse springsLineParse) {
            this.springsLineParse = springsLineParse;
            resultsCache = new HashMap<>();
            char[] state = springsLineParse.getState();
            maxGroupLength = new int[state.length]; // calculate max possible group for every place
            for (int i = maxGroupLength.length - 1; i >= 0; i--) {
                maxGroupLength[i] = state[i] != '.' ? (1 + ((i == maxGroupLength.length - 1) ? 0 : maxGroupLength[i + 1])) : 0;

            }
            int[] groups = springsLineParse.getGroups();
            groupsWithSepRestLength = new int[groups.length];
            for (int i = groupsWithSepRestLength.length - 1; i >= 0; i--) {
                groupsWithSepRestLength[i] = (i < groupsWithSepRestLength.length - 1) ? (groups[i + 1] + 1 + groupsWithSepRestLength[i + 1]) : 0;
            }
        }

        public int[] getGroups() {
            return springsLineParse.getGroups();
        }

        public char[] getState() {
            return springsLineParse.getState();
        }

        public int[] getMaxGroupLength() {
            return maxGroupLength;
        }

        public int[] getGroupsWithSepRestLength() { return groupsWithSepRestLength; }
    }
    private static long countArrangements(SpringsLineSolverContext context, int statePos, int groupIdx) {
        final var cacheKey = new Pair<>(statePos, groupIdx);
        long count = context.resultsCache.getOrDefault(cacheKey, -1L);
        if (count >= 0) {
            return count;
        }
        final char[] state = context.getState();
        final int[] groups = context.getGroups();
        boolean noGroupLeft = groupIdx >= groups.length;
        final int groupLength = !noGroupLeft ? groups[groupIdx] : Integer.MIN_VALUE;
        final int groupsWithSepRest = !noGroupLeft ? context.getGroupsWithSepRestLength()[groupIdx] : Integer.MIN_VALUE;
        final int[] maxGroupLength = context.getMaxGroupLength();
        count = !noGroupLeft ? 0 : 1; // if no groups left, confirm that the rest of state is effectively empty (non-damaged)
        // TODO: count 'good' springs at every position to further prune search range (subtract)
        for (int i = statePos; i <= state.length - (!noGroupLeft ? (groupLength + groupsWithSepRest) : 1); i++) {
            if (noGroupLeft) {
                if (state[i] == '#') {
                    // bad arrangement, more "damaged" springs in the state but no groups left
                    count = 0;
                    break;
                }
            } else {
                if (state[i] != '.') { // state[i] == '#' || state[i] == '?'
                    // must('#')/try('?') place current group starting at this position
                    if (maxGroupLength[i] >= groupLength && (i + groupLength >= state.length || state[i + groupLength] != '#')) {
                        // can place current group starting at this position
                        if (i + groupLength + 1 < state.length) {
                            count += countArrangements(context, i + groupLength + 1, groupIdx + 1);
                        } else {
                            count += 1;
                        }
                    } else {
                        ; // not enough space to place this group, discard this arrangement
                    }
                    if (state[i] == '#') {
                        break; // must place current group starting at this position
                    }
                } else { // state[i] == '.'
                    // ignore "good" springs
                }
            }
        }
        context.resultsCache.put(cacheKey, count);
        return count;
    }

    private static long countArrangements(ISpringsLineParse springsLineParse) {
        return countArrangements(new SpringsLineSolverContext(springsLineParse), 0, 0);
    }

    @Override
    protected void parsePart1() {
        springsLineParse1 = stream()
                .map(SpringsLineParsePart1::new)
                .toList()
                ;
    }

    @Override
    protected Object solvePart1() {
        long result = springsLineParse1.stream()
                // .parallel()
                // .peek(slp -> System.out.printf("%d: %s%n", springsLineParse1.indexOf(slp), slp.toStringLong()))
                .mapToLong(Day12::countArrangements)
                // .peek(System.out::println)
                .sum()
                ;
        return result;
    }

    @Override
    protected void parsePart2() {
        springsLineParse2 = stream()
                .map(SpringsLineParsePart2::new)
                .toList()
        ;
    }

    @Override
    protected Object solvePart2() {
        long result = springsLineParse2.stream()
                // .parallel()
                // .peek(slp -> System.out.println(slp.toStringLong()))
                .mapToLong(Day12::countArrangements)
                // .peek(System.out::println)
                .sum()
                ;
        return result;
    }

    public static class Day12Test {
        @Test
        void solvePart1_small() {
            var day = new Day12("_small");
            day.parsePart1();
            assertEquals(21L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day12("");
            day.parsePart1();
            assertEquals(7674L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day12("_small");
            day.parsePart2();
            assertEquals(525152L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day12("");
            day.parsePart2();
            assertEquals(4443895258186L, day.solvePart2());
        }

        @Test
        void knownGoodInputs() {
            assertEquals(5, countArrangements(new SpringsLineParsePart1("??????#?.???.??? 4,3")));
            assertEquals(5, countArrangements(new SpringsLineParsePart1("??????#?.???.??? 4,3").reverse()));
            assertEquals(2, countArrangements(new SpringsLineParsePart1(".???..??##.. 2,4")));
            assertEquals(2, countArrangements(new SpringsLineParsePart1(".???..??##.. 2,4").reverse()));
            assertEquals(6, countArrangements(new SpringsLineParsePart1(".???..??#???? 2,4")));
            assertEquals(6, countArrangements(new SpringsLineParsePart1(".???..??#???? 2,4").reverse()));
            assertEquals(6, countArrangements(new SpringsLineParsePart1("??#??#?????.?????? 7,5")));
            assertEquals(6, countArrangements(new SpringsLineParsePart1("??#??#?????.?????? 7,5").reverse()));
            assertEquals(1, countArrangements(new SpringsLineParsePart1("????.#...#... 4,1,1")));
            assertEquals(1, countArrangements(new SpringsLineParsePart1("????.#...#... 4,1,1").reverse()));
            assertEquals(1, countArrangements(new SpringsLineParsePart1("???...?.??.##..#?? 1,1,1,2,2,2")));
            assertEquals(1, countArrangements(new SpringsLineParsePart1("???...?.??.##..#?? 1,1,1,2,2,2").reverse()));
            assertEquals(1, countArrangements(new SpringsLineParsePart1("#???..#?##? 3,1,2")));
            assertEquals(1, countArrangements(new SpringsLineParsePart1("#???..#?##? 3,1,2").reverse()));
            assertEquals(21, countArrangements(new SpringsLineParsePart1("????????..?????#?#?? 3,5")));
            assertEquals(21, countArrangements(new SpringsLineParsePart1("????????..?????#?#?? 3,5").reverse()));
            assertEquals(7, countArrangements(new SpringsLineParsePart1("????.?#??? 2,2")));
            assertEquals(7, countArrangements(new SpringsLineParsePart1("????.?#??? 2,2").reverse()));
            assertEquals(15, countArrangements(new SpringsLineParsePart1("????##???.?#??.???? 2,5,1,1")));
            assertEquals(15, countArrangements(new SpringsLineParsePart1("????##???.?#??.???? 2,5,1,1").reverse()));
            assertEquals(54, countArrangements(new SpringsLineParsePart1("?.#?#?#????.?????? 1,1,3,1,1,1")));
            assertEquals(54, countArrangements(new SpringsLineParsePart1("?.#?#?#????.?????? 1,1,3,1,1,1").reverse()));
        }

        @Test
        void knownLargeInputs() {
            assertEquals(10518300L, countArrangements(new SpringsLineParsePart1("????????????????????????????????????????# 1,1,1,1,1,1,1,1,1")));
            assertEquals(239877544005L, countArrangements(new SpringsLineParsePart1("????????????????????????????????????????????????????????????# 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")));
            assertEquals(68248282427325L, countArrangements(new SpringsLineParsePart1("????????????????????????????????????????????????????????????????????????# 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")));
            assertEquals(19619725782651120L, countArrangements(new SpringsLineParsePart1("????????????????????????????????????????????????????????????????????????????????????# 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")));
            assertEquals(-2381415734801814264L, countArrangements(new SpringsLineParsePart1("????????????????????????????????????????????????????????????????????????????????????????????????????????????# 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")));
        }
    }
}
/*

--- Day 12: Hot Springs ---

You finally reach the hot springs! You can see steam rising from secluded areas attached to the primary, ornate building.

As you turn to enter, the researcher stops you. "Wait - I thought you were looking for the hot springs, weren't you?" You indicate that this definitely looks like hot springs to you.

"Oh, sorry, common mistake! This is actually the onsen! The hot springs are next door."

You look in the direction the researcher is pointing and suddenly notice the massive metal helixes towering overhead. "This way!"

It only takes you a few more steps to reach the main gate of the massive fenced-off area containing the springs. You go through the gate and into a small administrative building.

"Hello! What brings you to the hot springs today? Sorry they're not very hot right now; we're having a lava shortage at the moment." You ask about the missing machine parts for Desert Island.

"Oh, all of Gear Island is currently offline! Nothing is being manufactured at the moment, not until we get more lava to heat our forges. And our springs. The springs aren't very springy unless they're hot!"

"Say, could you go up and see why the lava stopped flowing? The springs are too cold for normal operation, but we should be able to find one springy enough to launch you up there!"

There's just one problem - many of the springs have fallen into disrepair, so they're not actually sure which springs would even be safe to use! Worse yet, their condition records of which springs are damaged (your puzzle input) are also damaged! You'll need to help them repair the damaged records.

In the giant field just outside, the springs are arranged into rows. For each row, the condition records show every spring and whether it is operational (.) or damaged (#). This is the part of the condition records that is itself damaged; for some springs, it is simply unknown (?) whether the spring is operational or damaged.

However, the engineer that produced the condition records also duplicated some of this information in a different format! After the list of springs for a given row, the size of each contiguous group of damaged springs is listed in the order those groups appear in the row. This list always accounts for every damaged spring, and each number is the entire size of its contiguous group (that is, groups are always separated by at least one operational spring: #### would always be 4, never 2,2).

So, condition records with no unknown spring conditions might look like this:

#.#.### 1,1,3
.#...#....###. 1,1,3
.#.###.#.###### 1,3,1,6
####.#...#... 4,1,1
#....######..#####. 1,6,5
.###.##....# 3,2,1

However, the condition records are partially damaged; some of the springs' conditions are actually unknown (?). For example:

???.### 1,1,3
.??..??...?##. 1,1,3
?#?#?#?#?#?#?#? 1,3,1,6
????.#...#... 4,1,1
????.######..#####. 1,6,5
?###???????? 3,2,1

Equipped with this information, it is your job to figure out how many different arrangements of operational and broken springs fit the given criteria in each row.

In the first line (???.### 1,1,3), there is exactly one way separate groups of one, one, and three broken springs (in that order) can appear in that row: the first three unknown springs must be broken, then operational, then broken (#.#), making the whole row #.#.###.

The second line is more interesting: .??..??...?##. 1,1,3 could be a total of four different arrangements. The last ? must always be broken (to satisfy the final contiguous group of three broken springs), and each ?? must hide exactly one of the two broken springs. (Neither ?? could be both broken springs or they would form a single contiguous group of two; if that were true, the numbers afterward would have been 2,3 instead.) Since each ?? can either be #. or .#, there are four possible arrangements of springs.

The last line is actually consistent with ten different arrangements! Because the first number is 3, the first and second ? must both be . (if either were #, the first number would have to be 4 or higher). However, the remaining run of unknown spring conditions have many different ways they could hold groups of two and one broken springs:

?###???????? 3,2,1
.###.##.#...
.###.##..#..
.###.##...#.
.###.##....#
.###..##.#..
.###..##..#.
.###..##...#
.###...##.#.
.###...##..#
.###....##.#

In this example, the number of possible arrangements for each row is:

    ???.### 1,1,3 - 1 arrangement
    .??..??...?##. 1,1,3 - 4 arrangements
    ?#?#?#?#?#?#?#? 1,3,1,6 - 1 arrangement
    ????.#...#... 4,1,1 - 1 arrangement
    ????.######..#####. 1,6,5 - 4 arrangements
    ?###???????? 3,2,1 - 10 arrangements

Adding all of the possible arrangement counts together produces a total of 21 arrangements.

For each row, count all of the different arrangements of operational and broken springs that meet the given criteria. What is the sum of those counts?

Your puzzle answer was 7674.

--- Part Two ---

As you look out at the field of springs, you feel like there are way more springs than the condition records list. When you examine the records, you discover that they were actually folded up this whole time!

To unfold the records, on each row, replace the list of spring conditions with five copies of itself (separated by ?) and replace the list of contiguous groups of damaged springs with five copies of itself (separated by ,).

So, this row:

.# 1

Would become:

.#?.#?.#?.#?.# 1,1,1,1,1

The first line of the above example would become:

???.###????.###????.###????.###????.### 1,1,3,1,1,3,1,1,3,1,1,3,1,1,3

In the above example, after unfolding, the number of possible arrangements for some rows is now much larger:

    ???.### 1,1,3 - 1 arrangement
    .??..??...?##. 1,1,3 - 16384 arrangements
    ?#?#?#?#?#?#?#? 1,3,1,6 - 1 arrangement
    ????.#...#... 4,1,1 - 16 arrangements
    ????.######..#####. 1,6,5 - 2500 arrangements
    ?###???????? 3,2,1 - 506250 arrangements

After unfolding, adding all of the possible arrangement counts together produces 525152.

Unfold your condition records; what is the new sum of possible arrangement counts?

Your puzzle answer was 4443895258186.

 */