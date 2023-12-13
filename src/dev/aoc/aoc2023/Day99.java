package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day99 extends Day {
    public Day99(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        new Day99("_small").run(); // _small
    }

    @Override
    protected void parsePart1() {
    }

    @Override
    protected Object solvePart1() {
        int result = 0;
        return result;
    }

    @Override
    protected void parsePart2() {
    }

    @Override
    protected Object solvePart2() {
        int result = 0;
        return result;
    }

    public static class Day99Test {
        @Test
        void knownGoodInputs() {

        }

        @Test
        void solvePart1_small() {
            var day = new Day99("_small");
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
            var day = new Day99("_small");
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