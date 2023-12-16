package dev.aoc.aoc2023;

import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * First approach to solving the problem.
 * Overengineered class SpringsLine with verbose method of calculating arrangements using trimming and recursion.
 * See Day12 for easier method without trimming.
 */
public class Day12_FirstApproach extends Day {
    public static void main(String[] args) {
        Day.run(() -> new Day12_FirstApproach("")); //_small, _small2, _test_trimming
    }

    public Day12_FirstApproach(String inputSuffix) {
        super(inputSuffix);
    }

    private interface ISpringsLineParse {
        String getLine();
        char[] getState();
        int[] getGroups();
        ISpringsLineParse reverse();
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

    private List<SpringsLine> springs1, springs2;

    private static final class SpringsLine {
        private record ResultKey(int stateStart, int groupsStart) {}

        private final String line;
        private final char[] state;
        private final int[] groups;

        private int stateStart, stateEnd;
        private int groupsStart, groupsEnd;

        private final ResultKey resultKey;

        public SpringsLine(ISpringsLineParse lineParse) {
            this(lineParse.getLine(), lineParse.getState(), lineParse.getGroups(), 0, lineParse.getState().length, 0, lineParse.getGroups().length);
        }

        private SpringsLine(String line, char[] state, int[] groups, int stateStart, int stateEnd, int groupsStart, int groupsEnd) {
            this.line = line;
            this.state = state;
            this.groups = groups;
            this.stateStart = stateStart;
            this.stateEnd = stateEnd;
            this.groupsStart = groupsStart;
            this.groupsEnd = groupsEnd;
            if (line != null) {
                trim();
            } else {
                trimLeft(); // hack: trim left during counting arrangements, because we go from left
            }
            resultKey = new ResultKey(this.stateStart, this.groupsStart); // we don't use stateEnd and groupsEnd because we go from left to right while counting arrangements
        }

        public SpringsLine reversed() {
            char[] newState = Arrays.copyOf(state, state.length);
            Chars.reverse(newState);
            int[] newGroups = Arrays.copyOf(groups, groups.length);
            Ints.reverse(newGroups);
            return new SpringsLine(line, newState, newGroups, 0, state.length, 0, groups.length);
        }

        public long countArrangements() {
            var results = new HashMap<ResultKey, Long>();
            long result = countArrangements(results);
            return result;
        }
        public long countArrangements(Map<ResultKey, Long> results) {
            long storedResult = results.getOrDefault(resultKey, -1L);
            if (storedResult >= 0) {
                return storedResult;
            }
            if (isEmpty()) {
                return 1;
            }
            int[] max = new int[stateEnd - stateStart]; // calculate max possible group for every place
            for (int i = max.length - 1; i >= 0; i--) {
                max[i] = state[stateStart + i] != '.' ? (1 + ((i == max.length - 1) ? 0 : max[i + 1])) : 0;
            }
            long count = 0;
            int groupLength = groups[groupsStart];
            int groupsRestLength = Arrays.stream(groups).skip(groupsStart + 1).limit(groupsEnd - (groupsStart + 1)).sum();
            int groupSeparators = groupsEnd - groupsStart - 1;
            for (int i = stateStart; i < stateEnd - (groupLength + groupsRestLength + groupSeparators - 1); i++) {
                if (i - 1 >= stateStart && state[i - 1] == '#') {
                    break; // previous damaged place cannot be ignored, we must stop this loop
                }
                if (max[i - stateStart] < groupLength) {
                    continue; // cannot place here group of desired length, there is undamaged place blocking such group
                }
                if (i + groupLength < stateEnd && state[i + groupLength] == '#') {
                    continue; // no possibility of separation between this placement and next blocks
                }
                if (groupsStart + 1 == groupsEnd) {
                    // catch fail early: if no groups left, there can be no damaged place left
                    boolean impossible = false;
                    for (int p = Math.min(i + groupLength + 1, stateEnd); p < stateEnd; p++) {
                        if (state[p] == '#') {
                            impossible = true;
                            break;
                        }
                    }
                    if (impossible) {
                        continue;
                    }
                }
                ResultKey resultKeyBeforeCtor = new ResultKey(Math.min(i + groupLength + 1, stateEnd), groupsStart + 1);
                long storedResultBeforeCtor = results.getOrDefault(resultKeyBeforeCtor, -1L);
                if (storedResultBeforeCtor < 0) {
                    try {
                        SpringsLine restOfSpringsLine = new SpringsLine(null, state, groups, Math.min(i + groupLength + 1, stateEnd), stateEnd, groupsStart + 1, groupsEnd);
                        count += restOfSpringsLine.countArrangements(results);
                    } catch (IllegalArgumentException ignored) {
                        results.put(resultKeyBeforeCtor, 0L);
                    }
                } else {
                    count += storedResultBeforeCtor;
                }
            }
            results.put(resultKey, count);
            return count;
        }

        public boolean isEmpty() {
            boolean stateEmpty = stateStart == stateEnd;
            boolean groupEmpty = groupsStart == groupsEnd;
            if (stateEmpty != groupEmpty) {
                throw new IllegalArgumentException();
            }
            return stateEmpty;
        }

        public boolean isTrimmed() {
            return stateStart != 0 || stateEnd != state.length || groupsStart != 0 || groupsEnd != groups.length;
        }

        public void trim() {
            collapse();
            boolean dirty;
            do {
                dirty = false;
                dirty |= trimLeftDamaged();
                dirty |= trimLeftOperational();
                dirty |= trimRightDamaged();
                dirty |= trimRightOperational();
            } while (dirty && !isEmpty());
        }
        public void trimLeft() {
            collapse();
            boolean dirty;
            do {
                dirty = false;
                dirty |= trimLeftDamaged();
                dirty |= trimLeftOperational();
            } while (dirty && !isEmpty());
        }
        private boolean trimLeftOperational() {
            if (isEmpty()) {
                return false;
            }
            // (.*n)X R -> X,R
            int prevStateStart = stateStart;
            while (stateStart < stateEnd && state[stateStart] == '.') {
                stateStart++;
            }
            return stateStart != prevStateStart;
        }
        private boolean trimRightOperational() {
            if (isEmpty()) {
                return false;
            }
            // X(.*n) R -> X,R
            int prevStateEnd = stateEnd;
            while (stateStart < stateEnd - 1 && state[stateEnd - 1] == '.') {
                stateEnd--;
            }
            return stateEnd != prevStateEnd;
        }
        private boolean trimLeftDamaged() {
            if (isEmpty()) {
                return false;
            }
            // #((#|?)*(n-1))(.|?)X n,R -> X R
            if (state[stateStart] != '#') {
                return false;
            }
            int expectedGroupLength = groups[groupsStart];
            int nonOperationalEnd = stateStart + 1;
            while (nonOperationalEnd < stateEnd && nonOperationalEnd - stateStart < expectedGroupLength && state[nonOperationalEnd] != '.') {
                nonOperationalEnd++;
            }
            int groupLength = nonOperationalEnd - stateStart;
            expectGroupLength(groupsStart, groupLength);
            if (nonOperationalEnd < stateEnd) {
                // #((#|?)*(n-1))(.|?)X n,R -> X R
                expectUndamagedPosition(nonOperationalEnd);
                stateStart = nonOperationalEnd + 1;
            } else {
                // #((#|?)*(n-1)) n,R -> EMPTY R
                stateStart = stateEnd;
            }
            groupsStart++;
            collapse();
            return true;
        }
        private boolean trimRightDamaged() {
            if (isEmpty()) {
                return false;
            }
            // X(.|?)((#|?)*(n-1))# R,n -> X R
            if (state[stateEnd - 1] != '#') {
                return false;
            }
            int expectedGroupLength = groups[groupsEnd - 1];
            int nonOperationalStart = (stateEnd - 1) - 1;
            while (nonOperationalStart >= stateStart && (stateEnd - 1) - nonOperationalStart < expectedGroupLength && state[nonOperationalStart] != '.') {
                nonOperationalStart--;
            }
            int groupLength = (stateEnd - 1) - nonOperationalStart;
            expectGroupLength(groupsEnd - 1, groupLength);
            if (nonOperationalStart >= stateStart) {
                // X(.|?)((#|?)*(n-1))# R,n -> X R
                expectUndamagedPosition(nonOperationalStart);
                stateEnd = nonOperationalStart;
            } else {
                // ((#|?)*(n-1))# R,n -> EMPTY R
                stateStart = stateEnd;
            }
            groupsEnd--;
            collapse();
            return true;
        }
        /** Ff groups changed to EMPTY, resolve possible empty state to EMPTY too */

        private void collapse() {
            if (groupsEnd - groupsStart == 0) {
                // ((.|?)*n) EMPTY -> EMPTY EMPTY
                expectStatePossiblyUndamaged();
                stateStart = stateEnd;
            }
        }

        private void expectGroupLength(int groupIndex, int groupLength) {
            if (groups[groupIndex] != groupLength) {
                throw new IllegalArgumentException("group length mismatch, expected %d, got %d, in %s".formatted(groupLength, groups[groupIndex], toString()));
            }
        }
        private void expectUndamagedPosition(int stateIndex) {
            if (state[stateIndex] == '#') {
                throw new IllegalArgumentException("expected undamaged position at %d, got damaged, in %s".formatted(stateIndex, toString()));
            }
        }
        private void expectStatePossiblyUndamaged() {
            boolean isDamaged = false;
            for (int si = stateStart; si < stateEnd; si++)
                isDamaged |= state[si] == '#';
            if (isDamaged) {
                throw new IllegalArgumentException("expected possibly undamaged state, got damaged state, in %s".formatted(toString()));
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (SpringsLine) obj;
            return Arrays.equals(this.state, that.state) && Arrays.equals(this.groups, that.groups);
        }

        @Override
        public int hashCode() {
            if (true) throw new IllegalStateException("TODO use instead of RecordKey");
            return Objects.hash(Arrays.hashCode(state), Arrays.hashCode(groups));
        }

        @Override
        public String toString() {
            if (isTrimmed()) {
                String stateString = CharBuffer.wrap(state).chars().skip(stateStart).limit(stateEnd - stateStart).mapToObj(ch -> (char) ch).toList().toString();
                String groupsString = Arrays.toString(Arrays.stream(groups).skip(groupsStart).limit(groupsEnd - groupsStart).toArray());
                return "[s=%s,g=%s]/[S=%s,G=%s]".formatted(stateString, groupsString, Arrays.toString(state), Arrays.toString(groups));
            } else {
                return "[S=%s,G=%s]".formatted(Arrays.toString(state), Arrays.toString(groups));
            }
        }

        public String toStringLong() {
            if (isTrimmed()) {
                String stateString = CharBuffer.wrap(state).chars().skip(stateStart).limit(stateEnd - stateStart).mapToObj(ch -> (char) ch).toList().toString();
                String groupsString = Arrays.toString(Arrays.stream(groups).skip(groupsStart).limit(groupsEnd - groupsStart).toArray());
                return "%s[s=%s,g=%s]/[S=%s,G=%s]".formatted(line != null ? "%s%n".formatted(line) : "", stateString, groupsString, Arrays.toString(state), Arrays.toString(groups));
            } else {
                return "%s[S=%s,G=%s]".formatted(line != null ? "%s%n".formatted(line) : "", Arrays.toString(state), Arrays.toString(groups));
            }
        }

        public String toStringTrimmed() {
            String stateTrimmed = CharBuffer.wrap(state).chars().skip(stateStart).limit(stateEnd - stateStart).mapToObj(ch -> String.valueOf((char)ch)).collect(Collectors.joining());
            List<String> groupsTrimmed = Arrays.stream(groups).skip(groupsStart).limit(groupsEnd - groupsStart).mapToObj(Integer::toString).toList();
            return "%s %s".formatted(stateTrimmed, String.join(",", groupsTrimmed));
        }

        public String toStringOriginal() {
            return "%s %s".formatted(CharBuffer.wrap(state).toString(), String.join(",", Arrays.stream(groups).mapToObj(Integer::toString).toList()));
        }
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        List<SpringsLineParsePart1> springsLineParse1 = stream()
                .map(SpringsLineParsePart1::new)
                .toList();
        springs1 = springsLineParse1.stream()
                .map(SpringsLine::new)
                .toList()
                ;
        // System.out.printf("stats: %d all, %d trimmed%n", springs.size(), springs.stream().filter(SpringsLine::isTrimmed).count());
        // springs.forEach(sl -> {
        //     // if (sl.isTrimmed()) System.out.printf("%s  ---  %s%n", sl.toStringOriginal(), sl.toStringTrimmed());
        //     System.out.println(sl.toStringTrimmed());
        // });
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        long result = springs1.stream()
                // .peek(sl -> System.out.printf("%d: %s%n", springs1.indexOf(sl), sl.toStringLong()))
                .mapToLong(SpringsLine::countArrangements)
                // .peek(System.out::println)
                .sum()
                ;
        // long resultReversed = springs1.stream().map(SpringsLine::reversed).peek(sl -> System.out.println(sl.toStringLong())).mapToLong(SpringsLine::countArrangements).peek(System.out::println).sum();
        // springs1.forEach(sl -> {
        //     long r = sl.countArrangements(), rrev = sl.reversed().countArrangements();
        //     if (r != rrev) {
        //         System.out.println(sl.toStringLong());
        //         System.out.printf("%d,%d%n", r, rrev);
        //     }
        // });
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        List<SpringsLineParsePart2> springsLineParse2 = stream()
                .map(SpringsLineParsePart2::new)
                .toList();
        springs2 = springsLineParse2.stream()
                .map(SpringsLine::new)
                .toList()
        ;
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        long result = springs2.stream()
                .parallel()
                // .peek(sl -> System.out.println(sl.toStringLong()))
                .mapToLong(SpringsLine::countArrangements)
                // .peek(System.out::println)
                .sum()
                ;
        return result;
    }

    public static class Day12Test {
        @Test
        void solvePart1_sample() {
            var day = new Day12_FirstApproach("_sample");
            day.parsePart1();
            assertEquals(21L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day12_FirstApproach("");
            day.parsePart1();
            assertEquals(7674L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day12_FirstApproach("_sample");
            day.parsePart2();
            assertEquals(525152L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day12_FirstApproach("");
            day.parsePart2();
            assertEquals(4443895258186L, day.solvePart2());
        }

        @Test
        void knownLargeInputs() {
            assertEquals(10518300L, new SpringsLine(new SpringsLineParsePart1("????????????????????????????????????????# 1,1,1,1,1,1,1,1,1")).countArrangements());
            assertEquals(239877544005L, new SpringsLine(new SpringsLineParsePart1("????????????????????????????????????????????????????????????# 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")).countArrangements());
        }

        @Test
        void knownGoodInputs() {
            assertEquals(5, new SpringsLine(new SpringsLineParsePart1("??????#?.???.??? 4,3")).countArrangements());
            assertEquals(5, new SpringsLine(new SpringsLineParsePart1("??????#?.???.??? 4,3")).reversed().countArrangements());
            assertEquals(2, new SpringsLine(new SpringsLineParsePart1(".???..??##.. 2,4")).countArrangements());
            assertEquals(2, new SpringsLine(new SpringsLineParsePart1(".???..??##.. 2,4")).reversed().countArrangements());
            assertEquals(6, new SpringsLine(new SpringsLineParsePart1(".???..??#???? 2,4")).countArrangements());
            assertEquals(6, new SpringsLine(new SpringsLineParsePart1("??#??#?????.?????? 7,5")).countArrangements());
            assertEquals(1, new SpringsLine(new SpringsLineParsePart1("????.#...#... 4,1,1")).countArrangements());
            assertEquals(1, new SpringsLine(new SpringsLineParsePart1("???...?.??.##..#?? 1,1,1,2,2,2")).countArrangements());
            assertEquals(1, new SpringsLine(new SpringsLineParsePart1("#???..#?##? 3,1,2")).countArrangements());
            assertEquals(21, new SpringsLine(new SpringsLineParsePart1("????????..?????#?#?? 3,5")).countArrangements());
            assertEquals(7, new SpringsLine(new SpringsLineParsePart1("????.?#??? 2,2")).countArrangements());
            assertEquals(15, new SpringsLine(new SpringsLineParsePart1("????##???.?#??.???? 2,5,1,1")).countArrangements());
            assertEquals(54, new SpringsLine(new SpringsLineParsePart1("?.#?#?#????.?????? 1,1,3,1,1,1")).countArrangements());
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