package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day99 extends Day {
    public Day99(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day99("_sample")); // _sample
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        long result = 0;
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        long result = 0;
        return result;
    }

    public static class Day99Test {
        @Test
        void knownGoodInputs() {

        }

        @Test
        void solvePart1_small() {
            var day = new Day99("_sample");
            day.parsePart1();
            assertEquals(0L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day99("");
            day.parsePart1();
            assertEquals(0L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day99("_sample");
            day.parsePart1();
            day.parsePart2();
            assertEquals(0L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day99("");
            day.parsePart1();
            day.parsePart2();
            assertEquals(0L, day.solvePart2());
        }
    }
}
/*

COPY DESCRIPTION HERE

 */