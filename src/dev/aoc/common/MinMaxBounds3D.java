package dev.aoc.common;

import java.util.Objects;

public class MinMaxBounds3D {
    private final MinMaxBounds x;
    private final MinMaxBounds y;
    private final MinMaxBounds z;

    public MinMaxBounds3D() {
        this(new MinMaxBounds(), new MinMaxBounds(), new MinMaxBounds());
    }

    public MinMaxBounds3D(MinMaxBounds x, MinMaxBounds y, MinMaxBounds z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void reset() {
        x.reset();
        y.reset();
        z.reset();
    }

    public int getXRange() {
        return x.getRange();
    }
    public int getYRange() {
        return y.getRange();
    }
    public int getZRange() {
        return z.getRange();
    }

    public void accX(int value) {
        x.acc(value);
    }
    public void accY(int value) {
        y.acc(value);
    }
    public void accZ(int value) {
        z.acc(value);
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
    public int getMinZ() {
        return z.getMin();
    }
    public int getMaxZ() {
        return z.getMax();
    }

    public boolean equals(MinMaxBounds3D that) {
        return x.equals(that.x) && y.equals(that.y) && z.equals(that.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinMaxBounds3D that = (MinMaxBounds3D) o;
        return Objects.equals(x, that.x) && Objects.equals(y, that.y) && Objects.equals(z, that.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "[x:%s,y:%s,z:%s]".formatted(x, y, z);
    }
}
