package dev.aoc.common;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class Grid {
    private final char[][] symbols;
    private final int width;
    private final int height;

    public Grid(List<String> lines) {
        this(
                toArray(verifyEqualLengths(lines)),
                lines.getFirst().length(),
                lines.size()
        );
    }
    private static List<String> verifyEqualLengths(List<String> lines) {
        int height = lines.size();
        int width = lines.getFirst().length();
        IntStream.range(1, height).forEach(row -> {
            if (lines.get(row).length() != width) {
                throw new IllegalArgumentException("line length mismatch");
            }
        });
        return lines;
    }
    public Grid(int width, int height, char fillSymbol) {
        this(emptyGrid(width, height, fillSymbol), width, height);
    }
    private static char[][] emptyGrid(int width, int height, char fillSymbol) {
        char[][] result = new char[height][];
        IntStream.range(0, height).forEach(row -> {
            result[row] = new char[width];
            Arrays.fill(result[row], fillSymbol);
        });
        return result;
    }
    private Grid(char[][] symbols, int width, int height) {
        this.symbols = symbols;
        this.width = width;
        this.height = height;
    }

    public char get(int col, int row) {
        return symbols[row][col];
    }
    public void set(int col, int row, char s) {
        symbols[row][col] = s;
    }

    public boolean is(int col, int row, char s) {
        return symbols[row][col] == s;
    }
    public boolean isNot(int col, int row, char s) {
        return symbols[row][col] != s;
    }
    public boolean isInSet(int col, int row, String set) {
        return set.indexOf(symbols[row][col]) >= 0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasColumn(int col) {
        return col >= 0 && col < width;
    }

    public boolean hasRow(int row) {
        return row >= 0 && row < height;
    }

    public int count(Predicate<Character> predicate) {
        int count = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (predicate.test(symbols[row][col])) {
                    count++;
                }
            }
        }
        return count;
    }

    public void map(BiFunction<Integer, Integer, Character> mapper) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                symbols[row][col] = mapper.apply(col, row);
            }
        }
    }

    public void fill(char fillSymbol) {
        IntStream.range(0, height).forEach(row -> {
            Arrays.fill(symbols[row], fillSymbol);
        });
    }

    private List<String> toLines(char[][] mapArray) {
        return Arrays.stream(mapArray).map(chs -> CharBuffer.wrap(chs).toString()).toList();
    }

    private static char[][] toArray(List<String> lines) {
        char[][] result = new char[lines.size()][];
        IntStream.range(0, result.length).forEach(row -> {
            result[row] = new char[lines.getFirst().length()];
            String rowLine = lines.get(row);
            IntStream.range(0, result[row].length).forEach(col -> {
                result[row][col] = rowLine.charAt(col);
            });
        });
        return result;
    }

    private static char[][] clone(char[][] arr) {
        char[][] result = new char[arr.length][];
        IntStream.range(0, arr.length).forEach(row -> {
            result[row] = Arrays.copyOf(arr[row], arr[row].length);
        });
        return result;
    }

    @Override
    public String toString() {
        return String.join("%n".formatted(), toLines(symbols));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(Arrays.stream(symbols).mapToInt(Arrays::hashCode).toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grid grid = (Grid)o;
        return Arrays.deepEquals(symbols, grid.symbols);
    }
}
