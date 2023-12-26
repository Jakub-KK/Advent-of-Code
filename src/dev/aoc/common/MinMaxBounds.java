package dev.aoc.common;

import dev.aoc.aoc2023.Day22;

import java.util.Objects;

public final class MinMaxBounds {
    private int min;
    private int max;

    public MinMaxBounds() {
        this(Integer.MAX_VALUE, Integer.MIN_VALUE);
    }
    public MinMaxBounds(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public void reset() {
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
    }

    public int getRange() {
        if (min == Integer.MAX_VALUE) {
            return 0;
        }
        return max - min + 1;
    }

    public void acc(int value) {
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public int min() {
        return min;
    }
    public int getMin() {
        return min;
    }
    public int max() {
        return max;
    }
    public int getMax() {
        return max;
    }

    public boolean equals(MinMaxBounds that) {
        return this.min == that.min && this.max == that.max;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MinMaxBounds) obj;
        return this.min == that.min && this.max == that.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return min == Integer.MAX_VALUE ? "[]" : "[%s,%s]".formatted(min, max);
    }
}
