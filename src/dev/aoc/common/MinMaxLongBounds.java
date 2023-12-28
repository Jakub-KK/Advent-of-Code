package dev.aoc.common;

import java.util.Objects;

public final class MinMaxLongBounds {
    private long min;
    private long max;

    public MinMaxLongBounds() {
        this(Long.MAX_VALUE, Long.MIN_VALUE);
    }
    public MinMaxLongBounds(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public void reset() {
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }

    public long getRange() {
        if (min == Long.MAX_VALUE) {
            return 0;
        }
        return max - min + 1;
    }

    public void acc(long value) {
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public long min() {
        return min;
    }
    public long getMin() {
        return min;
    }
    public long max() {
        return max;
    }
    public long getMax() {
        return max;
    }

    public boolean equals(MinMaxLongBounds that) {
        return this.min == that.min && this.max == that.max;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MinMaxLongBounds) obj;
        return this.min == that.min && this.max == that.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return min == Long.MAX_VALUE ? "[]" : "[%s,%s]".formatted(min, max);
    }
}
