package dev.aoc.common;

import java.util.Objects;

public class MinMaxLongBounds2D {
    private final MinMaxLongBounds x;
    private final MinMaxLongBounds y;

    public MinMaxLongBounds2D() {
        this(new MinMaxLongBounds(), new MinMaxLongBounds());
    }

    public MinMaxLongBounds2D(MinMaxLongBounds x, MinMaxLongBounds y) {
        this.x = x;
        this.y = y;
    }

    public void reset() {
        x.reset();
        y.reset();
    }

    public long getXRange() {
        return x.getRange();
    }

    public long getYRange() {
        return y.getRange();
    }

    public void acc(long x, long y) {
        accX(x);
        accY(y);
    }
    public void accX(long value) {
        x.acc(value);
    }
    public void accY(long value) {
        y.acc(value);
    }

    public long getMinX() {
        return x.getMin();
    }

    public long getMaxX() {
        return x.getMax();
    }

    public long getMinY() {
        return y.getMin();
    }

    public long getMaxY() {
        return y.getMax();
    }

    public boolean equals(MinMaxLongBounds2D that) {
        return x.equals(that.x) && y.equals(that.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinMaxLongBounds2D that = (MinMaxLongBounds2D) o;
        return Objects.equals(x, that.x) && Objects.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "[x:%s,y:%s]".formatted(x, y);
    }
}
