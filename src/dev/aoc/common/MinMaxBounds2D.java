package dev.aoc.common;

import java.util.Objects;

public class MinMaxBounds2D {
    private final MinMaxBounds x;
    private final MinMaxBounds y;

    public MinMaxBounds2D() {
        this(new MinMaxBounds(), new MinMaxBounds());
    }

    public MinMaxBounds2D(MinMaxBounds x, MinMaxBounds y) {
        this.x = x;
        this.y = y;
    }

    public void reset() {
        x.reset();
        y.reset();
    }

    public int getXRange() {
        return x.getRange();
    }

    public int getYRange() {
        return y.getRange();
    }

    public void accX(int value) {
        x.acc(value);
    }
    public void accY(int value) {
        y.acc(value);
    }

    public int getMinX() {
        return x.getMin();
    }

    public int getMaxX() {
        return x.getMax();
    }

    public int getMinY() {
        return y.getMin();
    }

    public int getMaxY() {
        return y.getMax();
    }

    public boolean equals(MinMaxBounds2D that) {
        return x.equals(that.x) && y.equals(that.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinMaxBounds2D that = (MinMaxBounds2D) o;
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
