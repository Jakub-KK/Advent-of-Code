package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day03 extends Day {
    public Day03(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day03("_sample")); // _sample
    }

    private List<String> engine;
    private int width, height;

    private int makeGearId(int w, int h) { return h * width + w; }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        engine = stream().toList();
        width = engine.get(0).length();
        height = engine.size();

        long result = 0;
        for (int h = 0; h < height; h++) {
            int w = 0;
            String engineLine = engine.get(h);
            do {
                while (w < width && (engineLine.charAt(w) < '0' || engineLine.charAt(w) > '9')) {
                    w++;
                }
                if (w >= width) {
                    continue;
                }
                int pStart = w;
                while (w < width && engineLine.charAt(w) >= '0' && engineLine.charAt(w) <= '9') {
                    w++;
                }
                int pEnd = w;
                int part = Integer.parseInt(engineLine.substring(pStart, pEnd));
                // check if has symbol adjacent
                boolean hasSymbol = false;
                if (!hasSymbol && h > 0) {
                    String sline = engine.get(h - 1);
                    for (int sw = Math.max(0, pStart - 1); sw < Math.min(width, pEnd + 1); sw++) {
                        char sch = sline.charAt(sw);
                        if (sch != '.' && (sch < '0' || sch > '9')) {
                            hasSymbol = true;
                            break;
                        }
                    }
                }
                if (!hasSymbol && h < height - 1) {
                    String sline = engine.get(h + 1);
                    for (int sw = Math.max(0, pStart - 1); sw < Math.min(width, pEnd + 1); sw++) {
                        char sch = sline.charAt(sw);
                        if (sch != '.' && (sch < '0' || sch > '9')) {
                            hasSymbol = true;
                            break;
                        }
                    }
                }
                if (!hasSymbol && pStart > 0) {
                    char sch = engineLine.charAt(pStart - 1);
                    if (sch != '.' && (sch < '0' || sch > '9')) {
                        hasSymbol = true;
                    }
                }
                if (!hasSymbol && pEnd < width) {
                    char sch = engineLine.charAt(pEnd);
                    if (sch != '.' && (sch < '0' || sch > '9')) {
                        hasSymbol = true;
                    }
                }
                // add to result
                if (hasSymbol) {
                    result += part;
                }
            } while (w < width);
        }
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        solvePart1();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        Map<Integer, ArrayList<Integer>> gears = new HashMap<Integer, ArrayList<Integer>>();
        for (int h = 0; h < height; h++) {
            int w = 0;
            String engineLine = engine.get(h);
            do {
                while (w < width && (engineLine.charAt(w) < '0' || engineLine.charAt(w) > '9')) {
                    w++;
                }
                if (w >= width) {
                    continue;
                }
                int pStart = w;
                while (w < width && engineLine.charAt(w) >= '0' && engineLine.charAt(w) <= '9') {
                    w++;
                }
                int pEnd = w;
                int part = Integer.parseInt(engineLine.substring(pStart, pEnd));
                // check is has symbol adjacent
                if (h > 0) {
                    String sline = engine.get(h - 1);
                    for (int sw = Math.max(0, pStart - 1); sw < Math.min(width, pEnd + 1); sw++) {
                        char sch = sline.charAt(sw);
                        if (sch == '*') {
                            gears.computeIfAbsent(makeGearId(sw, h - 1), k -> new ArrayList<>()).add(part);
                        }
                    }
                }
                if (h < height - 1) {
                    String sline = engine.get(h + 1);
                    for (int sw = Math.max(0, pStart - 1); sw < Math.min(width, pEnd + 1); sw++) {
                        char sch = sline.charAt(sw);
                        if (sch == '*') {
                            gears.computeIfAbsent(makeGearId(sw, h + 1), k -> new ArrayList<>()).add(part);
                        }
                    }
                }
                if (pStart > 0) {
                    char sch = engineLine.charAt(pStart - 1);
                    if (sch == '*') {
                        gears.computeIfAbsent(makeGearId(pStart - 1, h), k -> new ArrayList<>()).add(part);
                    }
                }
                if (pEnd < width) {
                    char sch = engineLine.charAt(pEnd);
                    if (sch == '*') {
                        gears.computeIfAbsent(makeGearId(pEnd, h), k -> new ArrayList<>()).add(part);
                    }
                }
            } while (w < width);
        }
        long result = 0;
        for (Map.Entry<Integer, ArrayList<Integer>> gearEntry : gears.entrySet()) {
            ArrayList<Integer> parts = gearEntry.getValue();
            if (parts.size() == 2) {
                result += parts.get(0) * parts.get(1);
            }
        }
        return result;
    }

    public static class Day03Test {
        @Test
        void solvePart1_sample() {
            var day = new Day03("_sample");
            day.parsePart1();
            assertEquals(4361L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day03("");
            day.parsePart1();
            assertEquals(531932L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day03("_sample");
            day.parsePart2();
            assertEquals(467835L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day03("");
            day.parsePart2();
            assertEquals(73646890L, day.solvePart2());
        }
    }
}
/*

--- Day 3: Gear Ratios ---

You and the Elf eventually reach a gondola lift station; he says the gondola lift will take you up to the water source, but this is as far as he can bring you. You go inside.

It doesn't take long to find the gondolas, but there seems to be a problem: they're not moving.

"Aaah!"

You turn around to see a slightly-greasy Elf with a wrench and a look of surprise. "Sorry, I wasn't expecting anyone! The gondola lift isn't working right now; it'll still be a while before I can fix it." You offer to help.

The engineer explains that an engine part seems to be missing from the engine, but nobody can figure out which one. If you can add up all the part numbers in the engine schematic, it should be easy to work out which part is missing.

The engine schematic (your puzzle input) consists of a visual representation of the engine. There are lots of numbers and symbols you don't really understand, but apparently any number adjacent to a symbol, even diagonally, is a "part number" and should be included in your sum. (Periods (.) do not count as a symbol.)

Here is an example engine schematic:

467..114..
...*......
..35..633.
......#...
617*......
.....+.58.
..592.....
......755.
...$.*....
.664.598..

In this schematic, two numbers are not part numbers because they are not adjacent to a symbol: 114 (top right) and 58 (middle right). Every other number is adjacent to a symbol and so is a part number; their sum is 4361.

Of course, the actual engine schematic is much larger. What is the sum of all of the part numbers in the engine schematic?

Your puzzle answer was 531932.
--- Part Two ---

The engineer finds the missing part and installs it in the engine! As the engine springs to life, you jump in the closest gondola, finally ready to ascend to the water source.

You don't seem to be going very fast, though. Maybe something is still wrong? Fortunately, the gondola has a phone labeled "help", so you pick it up and the engineer answers.

Before you can explain the situation, she suggests that you look out the window. There stands the engineer, holding a phone in one hand and waving with the other. You're going so slowly that you haven't even left the station. You exit the gondola.

The missing part wasn't the only issue - one of the gears in the engine is wrong. A gear is any * symbol that is adjacent to exactly two part numbers. Its gear ratio is the result of multiplying those two numbers together.

This time, you need to find the gear ratio of every gear and add them all up so that the engineer can figure out which gear needs to be replaced.

Consider the same engine schematic again:

467..114..
...*......
..35..633.
......#...
617*......
.....+.58.
..592.....
......755.
...$.*....
.664.598..

In this schematic, there are two gears. The first is in the top left; it has part numbers 467 and 35, so its gear ratio is 16345. The second gear is in the lower right; its gear ratio is 451490. (The * adjacent to 617 is not a gear because it is only adjacent to one part number.) Adding up all of the gear ratios produces 467835.

What is the sum of all of the gear ratios in your engine schematic?

Your puzzle answer was 73646890.

 */