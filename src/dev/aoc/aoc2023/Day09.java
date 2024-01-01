package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day09 extends Day {
    public Day09(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day09("_sample")); // _sample, _large
    }

    private List<List<Long>> inputs;

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        inputs = stream().map(s -> Arrays.stream(s.split(" ")).map(Long::parseLong).toList()).toList();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        BigInteger result = inputs.stream()
                .mapToLong(Day09::predictNext)
                // .peek(v -> System.out.printf("%d%n", v))
                .mapToObj(BigInteger::valueOf)
                .reduce(BigInteger::add)
                .get()
                // .sum() // int/long no overflow check
                // .reduce(Math::addExact).getAsLong() // int/long no overflow check
        ;
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parsePart1();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        BigInteger result = inputs.stream()
                .map(List::reversed)
                .mapToLong(Day09::predictNext)
                // .peek(v -> System.out.printf("%d%n", v))
                .mapToObj(BigInteger::valueOf)
                .reduce(BigInteger::add)
                .get()
                // .sum() // int/long no overflow check
                // .reduce(Math::addExact).getAsLong() // int/long no overflow check
        ;
        return result;
    }

    /**
     * Predicts next value for input sequence. Uses Lagrange Polynomial Form.
     */
    private static long predictNextPolynomial(List<Long> input) {
        // experimental: fitting polynomial to input data (have to find degree first by standard method of differences so not very useful)
        /*
        var points = new WeightedObservedPoints();
        IntStream.range(0, input.size()).forEach(i -> {
            points.add(i, input.get(i));
        });
        var fitter = PolynomialCurveFitter.create(10);
        final double[] coeff = fitter.fit(points.toList());
         */
        // use Lagrange polynomial, see https://en.wikipedia.org/wiki/Lagrange_polynomial
        var poly = new PolynomialFunctionLagrangeForm(
                DoubleStream.iterate(0.0, v -> v + 1.0).limit(input.size()).toArray(),
                input.stream().mapToDouble(v -> v).toArray()
        );
        // double[] coefficients = poly.getCoefficients();
        // var diffsAll = getDiffsAll(input);
        double result = poly.value(input.size());
        return Math.round(result);
    }

    /**
     * Predicts next value for input sequence. Uses recursive formula with full differences calculation.
     */
    private static long predictNextRecursive(List<Long> input) {
        if (input.stream().allMatch(v -> v == 0)) {
            return 0L;
        } else {
            long result =
                    Math.addExact(predictNextRecursive(getDiff(input)), input.getLast()) // overflow possible
                    // predictNextRecursive(getDiff(input)) + input.getLast()
                    ;
            return result;
        }
    }

    /**
     * Calculates difference table for given sequence.
     */
    private static List<Long> getDiff(List<Long> input) {
        return IntStream.range(1, input.size())
                .mapToObj(i -> Math.subtractExact(input.get(i), input.get(i - 1))) // overflow possible
                // .mapToObj(i -> input.get(i) - input.get(i - 1))
                .toList()
                ;
    }

    /**
     * Predicts next value for input sequence. Uses exhaustive calculation with full differences calculation.
     */
    private static long predictNext(List<Long> input) {
        ArrayList<ArrayList<Long>> diffs = getDiffsAll(input);
        diffs.get(diffs.size() - 1).add(0L);
        for (int i = diffs.size() - 2; i >= 0; i--) {
            diffs.get(i).add(Math.addExact(diffs.get(i + 1).getLast(), diffs.get(i).getLast())); // overflow possible
            // diffs.get(i).add(diffs.get(i + 1).getLast() + diffs.get(i).getLast());
        }
        return diffs.get(0).getLast();
    }

    /**
     * Calculates full differences table, all the way to sequence of only zeros.
     */
    private static ArrayList<ArrayList<Long>> getDiffsAll(List<Long> input) {
        ArrayList<ArrayList<Long>> diffs = new ArrayList<>();
        diffs.add(new ArrayList<>(input));
        boolean stop;
        do {
            ArrayList<Long> diff = diffs.getLast();
            ArrayList<Long> nextDiff = new ArrayList<>();
            IntStream.range(1, diff.size()).forEach(i -> {
                nextDiff.add(Math.subtractExact(diff.get(i), diff.get(i - 1))); // overflow possible
                // nextDiff.add(diff.get(i) - diff.get(i - 1));
            });
            diffs.add(nextDiff);
            stop = nextDiff.stream().allMatch(v -> v == 0);
        } while (!stop);
        return diffs;
    }

    private void createTest(String testSuffix, int numOfSeqs, int length, int constRange) {
        createTestFile(testSuffix, writer -> {
            List<String> seqs = IntStream.range(0, numOfSeqs)
                    .mapToObj(i -> generateSequenceWithoutOverflow(length, constRange))
                    .map(seq -> Arrays.stream(seq).boxed().map(Object::toString).collect(Collectors.joining(" ")))
                    .peek(seqStr -> {
                        try {
                            writer.write(seqStr);
                            writer.write("\r\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();
            // System.out.println(writeStatuses.allMatch(s -> s) ? "test created" : "test creation failed");
        });
    }
    private static long[] generateSequenceWithoutOverflow(int length, int constRange) {
        long[] sequence = null;
        do {
            try {
                // generate sequence
                sequence = generateSequence(length, constRange);
                // find solutions, will check if overflows
                List<Long> seqList = Arrays.stream(sequence).boxed().toList();
                predictNext(seqList);
                predictNext(seqList.reversed());
            } catch (ArithmeticException e) {
                System.out.println("long overflow");
            }
        } while (sequence == null);
        return sequence;
    }
    private static long[] generateSequence(int length, int constRange) {
        int zeroDiffLevel = 1 + (int)Math.round(Math.random() * (length - 1));
        double rs = Math.nextUp(-constRange - 0.5), re = Math.nextDown(constRange + 0.5); // move ranges by 0.5 to increase the chance of hitting range extremes
        long[] constants = IntStream.range(0, zeroDiffLevel).mapToLong(i -> {
            double f = Math.random() / Math.nextDown(1.0);
            return Math.round(rs * (1.0 - f) + re * f);
        }).toArray();
        long[] sequence = new long[length - zeroDiffLevel];
        Arrays.fill(sequence, 0);
        int i = zeroDiffLevel;
        while (i-- > 0) {
            sequence = generateSequence(constants[i], sequence);
            // System.out.printf("%s%n", Arrays.toString(sequence));
        }
        // System.out.println(Arrays.stream(sequence).boxed().map(Object::toString).collect(Collectors.joining(" ")));
        // System.out.printf("%s%n", Arrays.toString(sequence));
        // ArrayList<ArrayList<Long>> diffsAll = getDiffsAll(Arrays.stream(sequence).boxed().toList());
        // System.out.println(diffsAll);
        // System.out.println(predictPreviousPolynomial(Arrays.stream(sequence).skip(1).boxed().toList()));
        // System.out.println(predictPreviousRecursive(Arrays.stream(sequence).skip(1).boxed().toList()));
        // System.out.println(predictPrevious(Arrays.stream(sequence).skip(1).boxed().toList()));
        return sequence;
    }
    private static long[] generateSequence(long constant, long[] diffs) {
        long[] result = new long[diffs.length + 1];
        result[0] = constant;
        IntStream.range(1, result.length).forEach(i -> {
            result[i] = Math.addExact(result[i - 1], diffs[i - 1]); // overflow possible
            // result[i] = result[i - 1] + diffs[i - 1];
        });
        return result;
    }

    public static class Day09Test {
        @Test
        void solvePart1_sample() {
            var day = new Day09("_sample");
            day.parsePart1();
            assertEquals(BigInteger.valueOf(114), day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day09("");
            day.parsePart1();
            assertEquals(BigInteger.valueOf(1762065988), day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day09("_sample");
            day.parsePart2();
            assertEquals(BigInteger.valueOf(2), day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day09("");
            day.parsePart2();
            assertEquals(BigInteger.valueOf(1066), day.solvePart2());
        }
    }
}
/*

--- Day 9: Mirage Maintenance ---

You ride the camel through the sandstorm and stop where the ghost's maps told you to stop. The sandstorm subsequently subsides, somehow seeing you standing at an oasis!

The camel goes to get some water and you stretch your neck. As you look up, you discover what must be yet another giant floating island, this one made of metal! That must be where the parts to fix the sand machines come from.

There's even a hang glider partially buried in the sand here; once the sun rises and heats up the sand, you might be able to use the glider and the hot air to get all the way up to the metal island!

While you wait for the sun to rise, you admire the oasis hidden here in the middle of Desert Island. It must have a delicate ecosystem; you might as well take some ecological readings while you wait. Maybe you can report any environmental instabilities you find to someone so the oasis can be around for the next sandstorm-worn traveler.

You pull out your handy Oasis And Sand Instability Sensor and analyze your surroundings. The OASIS produces a report of many values and how they are changing over time (your puzzle input). Each line in the report contains the history of a single value. For example:

0 3 6 9 12 15
1 3 6 10 15 21
10 13 16 21 30 45

To best protect the oasis, your environmental report should include a prediction of the next value in each history. To do this, start by making a new sequence from the difference at each step of your history. If that sequence is not all zeroes, repeat this process, using the sequence you just generated as the input sequence. Once all of the values in your latest sequence are zeroes, you can extrapolate what the next value of the original history should be.

In the above dataset, the first history is 0 3 6 9 12 15. Because the values increase by 3 each step, the first sequence of differences that you generate will be 3 3 3 3 3. Note that this sequence has one fewer value than the input sequence because at each step it considers two numbers from the input. Since these values aren't all zero, repeat the process: the values differ by 0 at each step, so the next sequence is 0 0 0 0. This means you have enough information to extrapolate the history! Visually, these sequences can be arranged like this:

0   3   6   9  12  15
  3   3   3   3   3
    0   0   0   0

To extrapolate, start by adding a new zero to the end of your list of zeroes; because the zeroes represent differences between the two values above them, this also means there is now a placeholder in every sequence above it:

0   3   6   9  12  15   B
  3   3   3   3   3   A
    0   0   0   0   0

You can then start filling in placeholders from the bottom up. A needs to be the result of increasing 3 (the value to its left) by 0 (the value below it); this means A must be 3:

0   3   6   9  12  15   B
  3   3   3   3   3   3
    0   0   0   0   0

Finally, you can fill in B, which needs to be the result of increasing 15 (the value to its left) by 3 (the value below it), or 18:

0   3   6   9  12  15  18
  3   3   3   3   3   3
    0   0   0   0   0

So, the next value of the first history is 18.

Finding all-zero differences for the second history requires an additional sequence:

1   3   6  10  15  21
  2   3   4   5   6
    1   1   1   1
      0   0   0

Then, following the same process as before, work out the next value in each sequence from the bottom up:

1   3   6  10  15  21  28
  2   3   4   5   6   7
    1   1   1   1   1
      0   0   0   0

So, the next value of the second history is 28.

The third history requires even more sequences, but its next value can be found the same way:

10  13  16  21  30  45  68
   3   3   5   9  15  23
     0   2   4   6   8
       2   2   2   2
         0   0   0

So, the next value of the third history is 68.

If you find the next value for each history in this example and add them together, you get 114.

Analyze your OASIS report and extrapolate the next value for each history. What is the sum of these extrapolated values?

Your puzzle answer was 1762065988.

--- Part Two ---

Of course, it would be nice to have even more history included in your report. Surely it's safe to just extrapolate backwards as well, right?

For each history, repeat the process of finding differences until the sequence of differences is entirely zero. Then, rather than adding a zero to the end and filling in the next values of each previous sequence, you should instead add a zero to the beginning of your sequence of zeroes, then fill in new first values for each previous sequence.

In particular, here is what the third example history looks like when extrapolating back in time:

5  10  13  16  21  30  45
  5   3   3   5   9  15
   -2   0   2   4   6
      2   2   2   2
        0   0   0

Adding the new values on the left side of each sequence from bottom to top eventually reveals the new left-most history value: 5.

Doing this for the remaining example data above results in previous values of -3 for the first history and 0 for the second history. Adding all three new values together produces 2.

Analyze your OASIS report again, this time extrapolating the previous value for each history. What is the sum of these extrapolated values?

Your puzzle answer was 1066.

 */