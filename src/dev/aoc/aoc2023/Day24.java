package dev.aoc.aoc2023;

import dev.aoc.common.*;
import org.apache.commons.math3.linear.*;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day24 extends Day {
    public Day24(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day24("")); // _sample
    }

    private final double EPS = 1e-4;

    private record Hailstone(Triplet<Long, Long, Long> position, Triplet<Long, Long, Long> velocity) {
        public ArrayRealVector getPositionVector() {
            return new ArrayRealVector(new double[] { position.getValue0(), position.getValue1(), position.getValue2() }, false);
        }
        public ArrayRealVector getVelocityVector() {
            return new ArrayRealVector(new double[] { velocity.getValue0(), velocity.getValue1(), velocity.getValue2() }, false);
        }

        @Override
        public String toString() {
            return "%d, %d, %d @ %d, %d, %d".formatted(position.getValue0(), position.getValue1(), position.getValue2(), velocity.getValue0(), velocity.getValue1(), velocity.getValue2());
        }
        public static Hailstone parse(String s) {
            String[] parts = s.split(" @ ");
            var position = parseTriplet(parts[0]);
            var velocity = parseTriplet(parts[1]);
            return new Hailstone(position, velocity);
        }
        private static Triplet<Long, Long, Long> parseTriplet(String s) {
            String[] parts = s.trim().split(", ");
            long[] ints = Arrays.stream(parts).map(String::trim).mapToLong(Long::parseLong).toArray();
            if (Arrays.stream(ints).filter(v -> v == 0).count() > 0) {
                throw new IllegalArgumentException("velocity of 0 not allowed, in \"%s\"".formatted(s));
            }
            return new Triplet<>(ints[0], ints[1], ints[2]);
        }

    }
    private List<Hailstone> hailstones;

    private boolean testCollisionXY(long areaStart, long areaEnd, Hailstone a, Hailstone b) {
        double vDet = (double)a.velocity.getValue0() * b.velocity.getValue1() - (double)b.velocity.getValue0() * a.velocity.getValue1();
        if (Math.abs(vDet) < EPS) {
            return false; // parallel paths
        }
        long abXDiff = b.position.getValue0() - a.position.getValue0();
        long abYDiff = b.position.getValue1() - a.position.getValue1();
        double aT = ((double)b.velocity.getValue1() * abXDiff - (double)b.velocity.getValue0() * abYDiff) / vDet;
        if (aT < -EPS) {
            return false;
        }
        double aTXStart = (double) (areaStart - a.position.getValue0()) / a.velocity.getValue0();
        double aTXEnd = (double) (areaEnd - a.position.getValue0()) / a.velocity.getValue0();
        boolean aTXInArea = a.velocity.getValue0() > 0 ? (aTXStart <= aT && aT <= aTXEnd) : (aTXEnd <= aT && aT <= aTXStart);
        if (!aTXInArea) {
            return false;
        }
        double aTYStart = (double) (areaStart - a.position.getValue1()) / a.velocity.getValue1();
        double aTYEnd = (double) (areaEnd - a.position.getValue1()) / a.velocity.getValue1();
        boolean aTYInArea = a.velocity.getValue1() > 0 ? (aTYStart <= aT && aT <= aTYEnd) : (aTYEnd <= aT && aT <= aTYStart);
        if (!aTYInArea) {
            return false;
        }
        double bT = ((double)a.velocity.getValue1() * abXDiff - (double)a.velocity.getValue0() * abYDiff) / vDet;
        if (bT < -EPS) {
            return false;
        }
        double bTXStart = (double) (areaStart - b.position.getValue0()) / b.velocity.getValue0();
        double bTXEnd = (double) (areaEnd - b.position.getValue0()) / b.velocity.getValue0();
        boolean bTXInArea = b.velocity.getValue0() > 0 ? (bTXStart <= bT && bT <= bTXEnd) : (bTXEnd <= bT && bT <= bTXStart);
        if (!bTXInArea) {
            return false;
        }
        double bTYStart = (double) (areaStart - b.position.getValue1()) / b.velocity.getValue1();
        double bTYEnd = (double) (areaEnd - b.position.getValue1()) / b.velocity.getValue1();
        boolean bTYInArea = b.velocity.getValue1() > 0 ? (bTYStart <= bT && bT <= bTYEnd) : (bTYEnd <= bT && bT <= bTYStart);
        if (!bTYInArea) {
            return false;
        }
        return true;
    }

    private boolean testDetsNonZero(Hailstone a, Hailstone b) {
        double vDetXY = (double)a.velocity.getValue0() * b.velocity.getValue1() - (double)b.velocity.getValue0() * a.velocity.getValue1();
        if (Math.abs(vDetXY) < EPS) {
            return false; // parallel paths
        }
        double vDetYZ = (double)a.velocity.getValue1() * b.velocity.getValue2() - (double)b.velocity.getValue1() * a.velocity.getValue2();
        if (Math.abs(vDetYZ) < EPS) {
            return false; // parallel paths
        }
        double vDetXZ = (double)a.velocity.getValue0() * b.velocity.getValue2() - (double)b.velocity.getValue0() * a.velocity.getValue2();
        if (Math.abs(vDetXZ) < EPS) {
            return false; // parallel paths
        }
        return true;
    }

    private boolean testIntersectLines3D(Hailstone a, Hailstone b) {
        // Algorithm is ported from the C algorithm of
        // Paul Bourke at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline3d/
        // resultSegmentPoint1 = Vector3.Empty;
        // resultSegmentPoint2 = Vector3.Empty;

        // Vector3 p1 = line1Point1;
        // Vector3 p2 = line1Point2;
        // Vector3 p3 = line2Point1;
        // Vector3 p4 = line2Point2;
        // Vector3 p21 = p2 - p1;
        // Vector3 p43 = p4 - p3;
        // Vector3 p13 = p1 - p3;
        ArrayRealVector p21 = new ArrayRealVector(new double[] { a.velocity.getValue0(), a.velocity.getValue1(), a.velocity.getValue2() }, false);
        if (p21.dotProduct(p21) < EPS) {
            return false;
        }
        ArrayRealVector p43 = new ArrayRealVector(new double[] { b.velocity.getValue0(), b.velocity.getValue1(), b.velocity.getValue2() }, false);
        if (p43.dotProduct(p43) < EPS) {
            return false;
        }
        ArrayRealVector p1 = new ArrayRealVector(new double[] { a.position.getValue0(), a.position.getValue1(), a.position.getValue2() }, false);
        ArrayRealVector p3 = new ArrayRealVector(new double[] { b.position.getValue0(), b.position.getValue1(), b.position.getValue2() }, false);
        ArrayRealVector p13 = p1.subtract(p3);

        double d1343 = p13.getEntry(0) * p43.getEntry(0) + p13.getEntry(1) * p43.getEntry(1) + p13.getEntry(2) * p43.getEntry(2);
        double d4321 = p43.getEntry(0) * p21.getEntry(0) + p43.getEntry(1) * p21.getEntry(1) + p43.getEntry(2) * p21.getEntry(2);
        double d1321 = p13.getEntry(0) * p21.getEntry(0) + p13.getEntry(1) * p21.getEntry(1) + p13.getEntry(2) * p21.getEntry(2);
        double d4343 = p43.getEntry(0) * p43.getEntry(0) + p43.getEntry(1) * p43.getEntry(1) + p43.getEntry(2) * p43.getEntry(2);
        double d2121 = p21.getEntry(0) * p21.getEntry(0) + p21.getEntry(1) * p21.getEntry(1) + p21.getEntry(2) * p21.getEntry(2);

        double denom = d2121 * d4343 - d4321 * d4321;
        if (Math.abs(denom) < EPS) {
            return false;
        }
        double numer = d1343 * d4321 - d1321 * d4343;

        double mua = numer / denom;
        double mub = (d1343 + d4321 * (mua)) / d4343;

        if (mua < 0 || mub < 0) {
            return false;
        }

        // resultSegmentPoint1.X = (float)(p1.X + mua * p21.X);
        // resultSegmentPoint1.Y = (float)(p1.Y + mua * p21.Y);
        // resultSegmentPoint1.Z = (float)(p1.Z + mua * p21.Z);
        // resultSegmentPoint2.X = (float)(p3.X + mub * p43.X);
        // resultSegmentPoint2.Y = (float)(p3.Y + mub * p43.Y);
        // resultSegmentPoint2.Z = (float)(p3.Z + mub * p43.Z);

        return true;
    }

    private void parse() {
        hailstones = stream().map(Hailstone::parse).toList();
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
        // System.out.printf("hailstones:%n%s%n".formatted(String.join("\r\n", hailstones.stream().map(Hailstone::toString).toList())));
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        if (true) return null;
        long areaStart = getInputSuffix().isEmpty() ? 200000000000000L : 7;
        long areaEnd = getInputSuffix().isEmpty() ? 400000000000000L : 27;
        int count = 0;
        for (int ha = 0; ha < hailstones.size() - 1; ha++) {
            Hailstone hailstoneA = hailstones.get(ha);
            for (int hb = ha + 1; hb < hailstones.size(); hb++) {
                Hailstone hailstoneB = hailstones.get(hb);
                if (testCollisionXY(areaStart, areaEnd, hailstoneA, hailstoneB)) {
                    count++;
                }
                // if (testCollisionXY(areaStart, areaEnd, hailstoneA, hailstoneB) != testCollisionXY(areaStart, areaEnd, hailstoneB, hailstoneA)) {
                //     throw new IllegalStateException();
                // }
            }
        }
        long result = count;
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        /*
        pick 3 hailstones (small letters), find magic stone throw (big letters):
        EQ1: p1 + t1*v1 = P1 + t1*V1
        EQ2: p2 + t2*v2 = P1 + t2*V1
        EQ3: p3 + t3*v3 = P1 + t3*V1
        tN > 0, tN-tM <> 0

        subtract equations pairwise:
        EQ1-EQ2: p1-p2 + t1*v1 - t2*v2 = (t1 - t2) * V2
        EQ2-EQ3: p2-p3 + t2*v2 - t3*v3 = (t2 - t3) * V2

        divide by (tN-tM) terms, subtract again, multiply by common denominator:
        ((EQ1-EQ2)/(t1-t2)-(EQ2-EQ3)/(t2-t3))*((t1-t2)*(t2-t3)):
        (t2-t3)*(EQ1-EQ2)-(t1-t2)*(EQ2-EQ3) = 0

        (t2-t3)*(p1m2 + t1*v1 - t2*v2) - (t1-t2)*(p2m3 + t2*v2 - t3*v3) = 0
        t2*p1m2 + t1*t2*v1 - t2*t2*v2 - t3*p1m2 - t1*t3*v1 + t2*t3*v2 - t1*p2m3 - t1*t2*v2 + t1*t3*v3 + t2*p2m3 + t2*t2*v2 - t2*t3*v3 = 0
        t2*t2*v2 - t2*t2*v2 + t1*t2*v1 - t1*t2*v2 + t1*t3*v3 - t1*t3*v1 + t2*t3*v2 - t2*t3*v3 - t1*p2m3 + t2*p1m2 + t2*p2m3 - t3*p1m2 = 0
        (t2*t2*v2 - t2*t2*v2) + (t1*t2*v1 - t1*t2*v2) + (t1*t3*v3 - t1*t3*v1) + (t2*t3*v2 - t2*t3*v3) + t1*p3m2 + (t2*p1m2 + t2*p2m3) + t3*p2m1 = 0
        0 + t1*t2*(v1-v2) + t1*t3*(v3-v1) + t2*t3*(v2-v3) + t1*p3m2 + t2*p1m3 + t3*p2m1 = 0
        t1*t2*(v1-v2) + t1*t3*(v3-v1) + t2*t3*(v2-v3) + t1*p3m2 + t2*p1m3 + t3*p2m1 = 0

        x:=t1, y:=t2, z:=t3, A:=v1m2, B:=v3m1, C:=v2m3, D:=p3m2, E:=p1m3, F:=p2m1

        A*x*y + B*x*z + C*y*z + D*x + E*y + F*z = 0

        solve for x,y,z:
        A1*x*y+B1*x*z+C1*y*z+D1*x+E1*y+F1*z=0
        A2*x*y+B2*x*z+C2*y*z+D2*x+E2*y+F2*z=0
        A3*x*y+B3*x*z+C3*y*z+D3*x+E3*y+F3*z=0

        now that we have time, we can solve for V and then P
         */
        // t1 = 1008195171796 and t2 = 177443247580 and t3 = 403663893515 and t4 = 673879272412
        Hailstone h1 = hailstones.get(0);
        Hailstone h2 = hailstones.get(1);
        Hailstone h3 = hailstones.get(3);
        RealVector A = h1.getVelocityVector().subtract(h2.getVelocityVector());
        RealVector B = h3.getVelocityVector().subtract(h1.getVelocityVector());
        RealVector C = h2.getVelocityVector().subtract(h3.getVelocityVector());
        RealVector D = h3.getPositionVector().subtract(h2.getPositionVector());
        RealVector E = h1.getPositionVector().subtract(h3.getPositionVector());
        RealVector F = h2.getPositionVector().subtract(h1.getPositionVector());
        // // generate system of equations to solve for times of intersection for h1, h2, h3
        // IntStream.range(0, 3).forEach(d -> {
        //     System.out.printf("(%d)*x*y+(%d)*x*z+(%d)*y*z+(%d)*x+(%d)*y+(%d)*z=0%n", (long)A.getEntry(d), (long)B.getEntry(d), (long)C.getEntry(d), (long)D.getEntry(d), (long)E.getEntry(d), (long)F.getEntry(d));
        // });
        long[] times = new long[] { 1008195171796L, 177443247580L, 673879272412L };
        // double[] times = new double[] { 5, 3, 6 }; // for hailstone 1, 2 and 4
        RealVector V12 = h1.getPositionVector().subtract(h2.getPositionVector()).add(h1.getVelocityVector().mapMultiply(times[0])).subtract(h2.getVelocityVector().mapMultiply(times[1])).mapMultiply(1.0/(times[0]-times[1]));
        RealVector P = h1.getPositionVector().add(h1.getVelocityVector().mapMultiply(times[0])).subtract(V12.mapMultiply(times[0]));
        // RealVector V23 = h2.getPositionVector().subtract(h3.getPositionVector()).add(h2.getVelocityVector().mapMultiply(times[1])).subtract(h3.getVelocityVector().mapMultiply(times[2])).mapMultiply(1.0/(times[1]-times[2]));
        // RealMatrix constMatrix = new Array2DRowRealMatrix(3, 6);
        // RealVector[] consts = new RealVector[] { A, B, C, D, E, F };
        // IntStream.range(0, consts.length).forEach(ci -> constMatrix.setSubMatrix(new Array2DRowRealMatrix(new double[][] { consts[ci].toArray() }, false).transpose().getData(), 0, ci));
        // RealMatrix timeMatrix = new Array2DRowRealMatrix(new double[][] { new double[] { times[0]*times[1], times[0]*times[2], times[1]*times[2], times[0], times[1], times[2] } }, false).transpose();
        // RealMatrix verify = constMatrix.multiply(timeMatrix);
        // RealVector eqL = h1.getPositionVector().subtract(h2.getPositionVector()).add(h1.getVelocityVector().mapMultiply(times[0]).add(h2.getVelocityVector()).mapMultiply(times[1])).mapMultiply(1.0/(times[0]-times[1]));
        // RealVector eqR = h2.getPositionVector().subtract(h3.getPositionVector()).add(h2.getVelocityVector().mapMultiply(times[1]).add(h3.getVelocityVector()).mapMultiply(times[2])).mapMultiply(1.0/(times[1]-times[2]));
        // RealVector resultP = new ArrayRealVector(new double[] { 24, 13, 10 });
        // RealVector resultV = new ArrayRealVector(new double[] { -3, 1, 2 });
        // RealVector eq12 = h1.getPositionVector().subtract(h2.getPositionVector()).add(h1.getVelocityVector().mapMultiply(times[0]).subtract(h2.getVelocityVector().mapMultiply(times[1])));
        // RealVector eq12R = resultV.mapMultiply(times[0] - times[1]);
        // RealVector eq23 = h2.getPositionVector().subtract(h3.getPositionVector()).add(h2.getVelocityVector().mapMultiply(times[1]).subtract(h3.getVelocityVector().mapMultiply(times[2])));
        // RealVector eq23R = resultV.mapMultiply(times[1] - times[2]);
        System.out.println((long)(P.getEntry(0)+P.getEntry(1)+P.getEntry(2)));
        if (true) return null;
        long result = 0;
        return null;
    }

    public static class Day24Test {
        @Test
        void knownGoodInputs() {

        }

        @Test
        void solvePart1_sample() {
            var day = new Day24("_sample");
            day.parsePart1();
            assertEquals(2L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day24("");
            day.parsePart1();
            assertEquals(16502L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day24("_sample");
            day.parsePart2();
            assertEquals(47L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day24("");
            day.parsePart2();
            assertEquals(673641951253289L, day.solvePart2());
        }
    }
}
/*


Advent of Code

    [About][Events][Shop][Settings][Log Out]

Jakub-KK 48*
  {year=>2023}

    [Calendar][AoC++][Sponsors][Leaderboard][Stats]

Our sponsors help make Advent of Code possible:
GitButler - Code across multiple branches at the same time (from the co-founder of GitHub)
--- Day 24: Never Tell Me The Odds ---

It seems like something is going wrong with the snow-making process. Instead of forming snow, the water that's been absorbed into the air seems to be forming hail!

Maybe there's something you can do to break up the hailstones?

Due to strong, probably-magical winds, the hailstones are all flying through the air in perfectly linear trajectories. You make a note of each hailstone's position and velocity (your puzzle input). For example:

19, 13, 30 @ -2,  1, -2
18, 19, 22 @ -1, -1, -2
20, 25, 34 @ -2, -2, -4
12, 31, 28 @ -1, -2, -1
20, 19, 15 @  1, -5, -3

Each line of text corresponds to the position and velocity of a single hailstone. The positions indicate where the hailstones are right now (at time 0). The velocities are constant and indicate exactly how far each hailstone will move in one nanosecond.

Each line of text uses the format px py pz @ vx vy vz. For instance, the hailstone specified by 20, 19, 15 @ 1, -5, -3 has initial X position 20, Y position 19, Z position 15, X velocity 1, Y velocity -5, and Z velocity -3. After one nanosecond, the hailstone would be at 21, 14, 12.

Perhaps you won't have to do anything. How likely are the hailstones to collide with each other and smash into tiny ice crystals?

To estimate this, consider only the X and Y axes; ignore the Z axis. Looking forward in time, how many of the hailstones' paths will intersect within a test area? (The hailstones themselves don't have to collide, just test for intersections between the paths they will trace.)

In this example, look for intersections that happen with an X and Y position each at least 7 and at most 27; in your actual data, you'll need to check a much larger test area. Comparing all pairs of hailstones' future paths produces the following results:

Hailstone A: 19, 13, 30 @ -2, 1, -2
Hailstone B: 18, 19, 22 @ -1, -1, -2
Hailstones' paths will cross inside the test area (at x=14.333, y=15.333).

Hailstone A: 19, 13, 30 @ -2, 1, -2
Hailstone B: 20, 25, 34 @ -2, -2, -4
Hailstones' paths will cross inside the test area (at x=11.667, y=16.667).

Hailstone A: 19, 13, 30 @ -2, 1, -2
Hailstone B: 12, 31, 28 @ -1, -2, -1
Hailstones' paths will cross outside the test area (at x=6.2, y=19.4).

Hailstone A: 19, 13, 30 @ -2, 1, -2
Hailstone B: 20, 19, 15 @ 1, -5, -3
Hailstones' paths crossed in the past for hailstone A.

Hailstone A: 18, 19, 22 @ -1, -1, -2
Hailstone B: 20, 25, 34 @ -2, -2, -4
Hailstones' paths are parallel; they never intersect.

Hailstone A: 18, 19, 22 @ -1, -1, -2
Hailstone B: 12, 31, 28 @ -1, -2, -1
Hailstones' paths will cross outside the test area (at x=-6, y=-5).

Hailstone A: 18, 19, 22 @ -1, -1, -2
Hailstone B: 20, 19, 15 @ 1, -5, -3
Hailstones' paths crossed in the past for both hailstones.

Hailstone A: 20, 25, 34 @ -2, -2, -4
Hailstone B: 12, 31, 28 @ -1, -2, -1
Hailstones' paths will cross outside the test area (at x=-2, y=3).

Hailstone A: 20, 25, 34 @ -2, -2, -4
Hailstone B: 20, 19, 15 @ 1, -5, -3
Hailstones' paths crossed in the past for hailstone B.

Hailstone A: 12, 31, 28 @ -1, -2, -1
Hailstone B: 20, 19, 15 @ 1, -5, -3
Hailstones' paths crossed in the past for both hailstones.

So, in this example, 2 hailstones' future paths cross inside the boundaries of the test area.

However, you'll need to search a much larger test area if you want to see if any hailstones might collide. Look for intersections that happen with an X and Y position each at least 200000000000000 and at most 400000000000000. Disregard the Z axis entirely.

Considering only the X and Y axes, check all pairs of hailstones' future paths for intersections. How many of these intersections occur within the test area?

Your puzzle answer was 16502.

--- Part Two ---

Upon further analysis, it doesn't seem like any hailstones will naturally collide. It's up to you to fix that!

You find a rock on the ground nearby. While it seems extremely unlikely, if you throw it just right, you should be able to hit every hailstone in a single throw!

You can use the probably-magical winds to reach any integer position you like and to propel the rock at any integer velocity. Now including the Z axis in your calculations, if you throw the rock at time 0, where do you need to be so that the rock perfectly collides with every hailstone? Due to probably-magical inertia, the rock won't slow down or change direction when it collides with a hailstone.

In the example above, you can achieve this by moving to position 24, 13, 10 and throwing the rock at velocity -3, 1, 2. If you do this, you will hit every hailstone as follows:

Hailstone: 19, 13, 30 @ -2, 1, -2
Collision time: 5
Collision position: 9, 18, 20

Hailstone: 18, 19, 22 @ -1, -1, -2
Collision time: 3
Collision position: 15, 16, 16

Hailstone: 20, 25, 34 @ -2, -2, -4
Collision time: 4
Collision position: 12, 17, 18

Hailstone: 12, 31, 28 @ -1, -2, -1
Collision time: 6
Collision position: 6, 19, 22

Hailstone: 20, 19, 15 @ 1, -5, -3
Collision time: 1
Collision position: 21, 14, 12

Above, each hailstone is identified by its initial position and its velocity. Then, the time and position of that hailstone's collision with your rock are given.

After 1 nanosecond, the rock has exactly the same position as one of the hailstones, obliterating it into ice dust! Another hailstone is smashed to bits two nanoseconds after that. After a total of 6 nanoseconds, all of the hailstones have been destroyed.

So, at time 0, the rock needs to be at X position 24, Y position 13, and Z position 10. Adding these three coordinates together produces 47. (Don't add any coordinates from the rock's velocity.)

Determine the exact position and velocity the rock needs to have at time 0 so that it perfectly collides with every hailstone. What do you get if you add up the X, Y, and Z coordinates of that initial position?

Your puzzle answer was 673641951253289.

 */