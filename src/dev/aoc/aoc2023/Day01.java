package dev.aoc.aoc2023;

import dev.aoc.common.Day;

import java.util.ArrayList;

public class Day01 extends Day {
    public static void main(String[] args) {
        new Day01("").run(); // _small1, _small2
    }

    public Day01(String inputSuffix) {
        super(inputSuffix);
    }

    private final SpeltDigit[] sdigits = {
            null,
            new SpeltDigit("one", 2),
            new SpeltDigit("two", 2),
            new SpeltDigit("three", 4),
            new SpeltDigit("four", 4),
            new SpeltDigit("five", 3),
            new SpeltDigit("six", 3),
            new SpeltDigit("seven", 4),
            new SpeltDigit("eight", 4),
            new SpeltDigit("nine", 3)
    };

    @Override
    protected Object solvePart1() {
        int result = stream()
                .mapToInt(line -> {
                    var digits = new ArrayList<Integer>();
                    int pos = 0;
                    while (pos < line.length()) {
                        char ch = line.charAt(pos);
                        if (Character.isDigit(ch)) {
                            digits.add(Integer.parseInt("" + ch));
                        }
                        pos++;
                    }
                    int number;
                    if (!digits.isEmpty()) {
                        int firstDigit = digits.get(0);
                        int secondDigit = digits.get(digits.size() - 1);
                        number = firstDigit * 10 + secondDigit;
                    } else {
                        number = 0;
                    }
                    return number;
                })
                .sum()
                ;
        return result;
    }

    @Override
    protected Object solvePart2() {
        int result = stream()
                .mapToInt(line -> {
                    var digits = new ArrayList<Integer>();
                    int pos = 0;
                    while (pos < line.length()) {
                        char ch = line.charAt(pos);
                        if (Character.isDigit(ch)) {
                            digits.add(Integer.parseInt("" + ch));
                            pos++;
                        } else {
                            // disable this branch for puzzle A of AOC #1
                            boolean single = false;
                            boolean pair = false;
                            int index = -1;
                            switch (ch) {
                                case 'o': {
                                    single = true;
                                    index = 1;
                                    break;
                                }
                                case 't': {
                                    pair = true;
                                    index = 1;
                                    break;
                                }
                                case 'f': {
                                    pair = true;
                                    index = 2;
                                    break;
                                }
                                case 's': {
                                    pair = true;
                                    index = 3;
                                    break;
                                }
                                case 'e': {
                                    single = true;
                                    index = 8;
                                    break;
                                }
                                case 'n': {
                                    single = true;
                                    index = 9;
                                    break;
                                }
                            }
                            if (single) {
                                if (line.regionMatches(pos + 1, sdigits[index].name, 1, sdigits[index].name.length() - 1)) {
                                    digits.add(index);
                                    pos += sdigits[index].stride;
                                } else {
                                    pos++;
                                }
                            } else if (pair) {
                                if (line.regionMatches(pos + 1, sdigits[index * 2].name, 1, sdigits[index * 2].name.length() - 1)) {
                                    digits.add(index * 2);
                                    pos += sdigits[index * 2].stride;
                                } else if (line.regionMatches(pos + 1, sdigits[index * 2 + 1].name, 1, sdigits[index * 2 + 1].name.length() - 1)) {
                                    digits.add(index * 2 + 1);
                                    pos += sdigits[index * 2 + 1].stride;
                                } else {
                                    pos++;
                                }
                            } else {
                                pos++;
                            }
                        }
                    }
                    int firstDigit = digits.get(0);
                    int secondDigit = digits.get(digits.size() - 1);
                    int number = firstDigit * 10 + secondDigit;
                    return number;
                })
                .sum()
                ;
        return result;
    }

    private record SpeltDigit(String name, int stride) {}
}
/*

--- Day 1: Trebuchet?! ---

Something is wrong with global snow production, and you've been selected to take a look. The Elves have even given you a map; on it, they've used stars to mark the top fifty locations that are likely to be having problems.

You've been doing this long enough to know that to restore snow operations, you need to check all fifty stars by December 25th.

Collect stars by solving puzzles. Two puzzles will be made available on each day in the Advent calendar; the second puzzle is unlocked when you complete the first. Each puzzle grants one star. Good luck!

You try to ask why they can't just use a weather machine ("not powerful enough") and where they're even sending you ("the sky") and why your map looks mostly blank ("you sure ask a lot of questions") and hang on did you just say the sky ("of course, where do you think snow comes from") when you realize that the Elves are already loading you into a trebuchet ("please hold still, we need to strap you in").

As they're making the final adjustments, they discover that their calibration document (your puzzle input) has been amended by a very young Elf who was apparently just excited to show off her art skills. Consequently, the Elves are having trouble reading the values on the document.

The newly-improved calibration document consists of lines of text; each line originally contained a specific calibration value that the Elves now need to recover. On each line, the calibration value can be found by combining the first digit and the last digit (in that order) to form a single two-digit number.

For example:

1abc2
pqr3stu8vwx
a1b2c3d4e5f
treb7uchet

In this example, the calibration values of these four lines are 12, 38, 15, and 77. Adding these together produces 142.

Consider your entire calibration document. What is the sum of all of the calibration values?

Your puzzle answer was 54634.

--- Part Two ---

Your calculation isn't quite right. It looks like some of the digits are actually spelled out with letters: one, two, three, four, five, six, seven, eight, and nine also count as valid "digits".

Equipped with this new information, you now need to find the real first and last digit on each line. For example:

two1nine
eightwothree
abcone2threexyz
xtwone3four
4nineeightseven2
zoneight234
7pqrstsixteen

In this example, the calibration values are 29, 83, 13, 24, 42, 14, and 76. Adding these together produces 281.

What is the sum of all of the calibration values?

Your puzzle answer was 53855.

 */