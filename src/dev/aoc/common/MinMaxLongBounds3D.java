package dev.aoc.common;

import java.util.Objects;

public class MinMaxLongBounds3D {
    private final MinMaxLongBounds x;
    private final MinMaxLongBounds y;
    private final MinMaxLongBounds z;

    public MinMaxLongBounds3D() {
        this(new MinMaxLongBounds(), new MinMaxLongBounds(), new MinMaxLongBounds());
    }

    public MinMaxLongBounds3D(MinMaxLongBounds x, MinMaxLongBounds y, MinMaxLongBounds z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void reset() {
        x.reset();
        y.reset();
        z.reset();
    }

    public long getXRange() {
        return x.getRange();
    }
    public long getYRange() {
        return y.getRange();
    }
    public long getZRange() {
        return z.getRange();
    }

    public void acc(long x, long y, long z) {
        accX(x);
        accY(y);
        accZ(z);
    }
    public void accX(long value) {
        x.acc(value);
    }
    public void accY(long value) {
        y.acc(value);
    }
    public void accZ(long value) {
        z.acc(value);
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
    public long getMinZ() {
        return z.getMin();
    }
    public long getMaxZ() {
        return z.getMax();
    }

    public boolean equals(MinMaxLongBounds3D that) {
        return x.equals(that.x) && y.equals(that.y) && z.equals(that.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinMaxLongBounds3D that = (MinMaxLongBounds3D) o;
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
