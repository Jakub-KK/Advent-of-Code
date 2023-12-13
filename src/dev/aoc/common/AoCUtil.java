package dev.aoc.common;

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
}
