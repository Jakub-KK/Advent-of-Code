package dev.aoc.common;

import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Grid<T> {
    private final T[][] elements;
    private final int width;
    private final int height;
    /** Element delimiter in string representation */
    private final String elementDelimiter;
    protected final Class<?> elementClass;

    public Grid(List<String> lines, String elementDelimiter, Function<String, T> parser, Class<?> elementClass) {
        this(
                verifyEqualLengths(toArray(lines, parser, elementDelimiter, elementClass)),
                lines.getFirst().length(),
                lines.size(),
                elementDelimiter,
                elementClass
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
    private static <T> T[][] verifyEqualLengths(T[][] elements) {
        int height = elements.length;
        if (height > 0) {
            int width = elements[0].length;
            IntStream.range(1, height).forEach(row -> {
                if (elements[row].length != width) {
                    throw new IllegalArgumentException("line length mismatch at row %d".formatted(row));
                }
            });
        }
        return elements;
    }
    public Grid(int width, int height, T fillElement, String elementDelimiter) {
        this(emptyGrid(width, height, fillElement), width, height, elementDelimiter, fillElement.getClass());
    }
    public Grid(int width, int height, T fillElement, Class<?> anElementClass, String elementDelimiter) {
        this(emptyGrid(width, height, anElementClass, fillElement), width, height, elementDelimiter, anElementClass);
    }
    private static <T> T[][] emptyGrid(int width, int height, T fillSymbol) {
        Class<?> aClass = fillSymbol.getClass();
        return emptyGrid(width, height, aClass, fillSymbol);
    }
    private static <T> T[][] emptyGrid(int width, int height, Class<?> anElementClass, T fillSymbol) {
        @SuppressWarnings("unchecked")
        T[][] result = (T[][])Array.newInstance(anElementClass, height, width);
        IntStream.range(0, height).forEach(row -> Arrays.fill(result[row], fillSymbol));
        return result;
    }
    private Grid(T[][] elements, int width, int height, String elementDelimiter, Class<?> elementClass) {
        this.elements = elements;
        this.width = width;
        this.height = height;
        this.elementDelimiter = elementDelimiter;
        this.elementClass = elementClass;
    }
    /** Get Grid with the same dimensions (and element delimiter) as this one */
    public Grid<T> getTemplate(T fillElement) {
        return new Grid<>(width, height, fillElement, elementClass, elementDelimiter);
    }
    /** Get Grid with the same dimensions and contentst (and element delimiter) as this one */
    public Grid<T> getClone() {
        return new Grid<T>(cloneArray(elements, elementClass), width, height, elementDelimiter, elementClass);
    }

    public T get(int col, int row) {
        return elements[row][col];
    }
    public void set(int col, int row, T s) {
        elements[row][col] = s;
    }

    public boolean has(int col, int row) {
        return elements[row][col] != null;
    }
    public boolean is(int col, int row, T s) {
        return elements[row][col] == s;
    }
    public boolean isNot(int col, int row, T s) {
        return elements[row][col] != s;
    }
    public boolean isInSet(int col, int row, Set<T> set) {
        return set.contains(elements[row][col]);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getElementDelimiter() {
        return elementDelimiter;
    }

    public int getUniqueId(int col, int row) {
        return row * width + col;
    }

    public int getUniqueIdMax() {
        return width * height;
    }

    public boolean hasColumn(int col) {
        return col >= 0 && col < width;
    }

    public boolean hasRow(int row) {
        return row >= 0 && row < height;
    }

    public boolean hasPos(int col, int row) {
        return hasColumn(col) && hasRow(row);
    }

    public int count(Predicate<T> predicate) {
        int count = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (predicate.test(elements[row][col])) {
                    count++;
                }
            }
        }
        return count;
    }

    public int count(BiPredicate<Integer, Integer> predicate) {
        int count = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (predicate.test(col, row)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void map(BiFunction<Integer, Integer, T> mapper) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                elements[row][col] = mapper.apply(col, row);
            }
        }
    }

    public <R> Grid<R> map(Function<T, R> mapper, Class<?> elementUClass) {
        Grid<R> result = new Grid<>(getWidth(), getHeight(), null, elementUClass, elementDelimiter);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result.elements[row][col] = mapper.apply(elements[row][col]);
            }
        }
        return result;
    }

    public <R> Grid<R> map(BiFunction<Pair<Integer, Integer>, T, R> mapper, Class<?> elementRClass) {
        Grid<R> result = new Grid<>(getWidth(), getHeight(), null, elementRClass, elementDelimiter);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result.elements[row][col] = mapper.apply(new Pair<>(col, row), elements[row][col]);
            }
        }
        return result;
    }

    public <R> Stream<R> mapLine(Function<T[], R> mapper) {
        return Arrays.stream(elements).map(mapper);
    }

    public void forEach(BiConsumer<Pair<Integer, Integer>, T> consumer) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                consumer.accept(new Pair<>(col, row), elements[row][col]);
            }
        }
    }

    public void fill(char fillElement) {
        IntStream.range(0, height).forEach(row -> Arrays.fill(elements[row], fillElement));
    }

    protected String toStringCell(T cell) {
        return cell == null ? "<null>" : cell.toString();
    }

    private List<String> toLines(T[][] mapArray) {
        return Arrays.stream(mapArray).map(ts -> String.join(elementDelimiter, Arrays.stream(ts).map(this::toStringCell).toList())).toList();
    }

    public List<String> toLines() {
        return toLines(elements);
    }

    private static <T> T[][] toArray(List<String> lines, Function<String, T> parser, String elementDelimiter, Class<?> elementClass) {
        Class<?> arrayClass = Array.newInstance(elementClass, 0).getClass();
        @SuppressWarnings("unchecked")
        T[][] result = (T[][])Array.newInstance(arrayClass, lines.size());
        IntStream.range(0, result.length).forEach(row -> {
            String line = lines.get(row);
            String[] lineElements = line.split(elementDelimiter);
            result[row] = (T[])Array.newInstance(elementClass, lineElements.length);
            IntStream.range(0, result[row].length).forEach(col -> result[row][col] = parser.apply(lineElements[col]));
        });
        return result;
    }

    private static <T> T[][] cloneArray(T[][] elements, Class<?> elementClass) {
        Class<?> arrayClass = Array.newInstance(elementClass, 0).getClass();
        @SuppressWarnings("unchecked")
        T[][] result = (T[][])Array.newInstance(arrayClass, elements.length);
        IntStream.range(0, result.length).forEach(row -> {
            result[row] = Arrays.copyOf(elements[row], elements[row].length);
        });
        return result;
    }

    private static char[][] clone(char[][] arr) {
        char[][] result = new char[arr.length][];
        IntStream.range(0, arr.length).forEach(row -> result[row] = Arrays.copyOf(arr[row], arr[row].length));
        return result;
    }

    @Override
    public String toString() {
        return String.join("%n".formatted(), toLines(elements));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(Arrays.stream(elements).mapToInt(Arrays::hashCode).toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grid<T> that = (Grid<T>)o;
        return Arrays.deepEquals(elements, that.elements) && elementDelimiter.equals(that.elementDelimiter);
    }

    public enum Direction {
        UP(0, -1), NORTH(0, -1),
        DOWN(0, 1), SOUTH(0, 1),
        LEFT(-1, 0), WEST(-1, 0),
        RIGHT(1, 0), EAST(1, 0),
        UNKNOWN(0, 0);
        public final int dCol;
        public final int dRow;
        Direction(int dCol, int dRow) {
            this.dCol = dCol;
            this.dRow = dRow;
        }
        public static Direction[] getAll() {
            return new Direction[] { Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT };
        }
        public Direction reverse() {
            return switch (this) {
                case UP -> DOWN;
                case NORTH -> SOUTH;
                case DOWN -> UP;
                case SOUTH -> NORTH;
                case LEFT -> RIGHT;
                case WEST -> EAST;
                case RIGHT -> LEFT;
                case EAST -> WEST;
                case UNKNOWN -> UNKNOWN;
            };
        }
    }

    public static class GridTest {
        @Test
        void test() {
            Grid<Integer> intGrid = new Grid<>(2, 2, -1, ", ");
            assertEquals(true, intGrid.is(0, 0, -1));
        }
    }
}
