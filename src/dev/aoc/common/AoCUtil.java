package dev.aoc.common;

import dev.aoc.aoc2023.Day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AoCUtil {
    public static String getInputName(int aocYear, int aocDay, String fileSuffix) {
        return "aoc_%d-%02d%s.txt".formatted(aocYear, aocDay, fileSuffix);
    }

    public static Stream<String> readFileAsStreamOfLines(Path filePath) {
        try {
            return Files.lines(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file %s".formatted(filePath));
        }
    }

    public static long leastCommonMultiple(long a, long b) {
        return (a * b / greatestCommonDivisor(a, b));
    }

    public static long greatestCommonDivisor(long a, long b) {
        // Euclidean algorithm
        while (b != 0) {
            var temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}
