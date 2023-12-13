package dev.aoc.aoc2023;

import dev.aoc.common.Day;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Day10 extends Day {
    public static void main(String[] args) {
        new Day10("").run(); // _small1, _small2, _small3a, _small3b, _small4, _small5
    }

    public Day10(String inputSuffix) {
        super(inputSuffix);
    }

    private List<String> map;
    private int width, height;
    private int startX, startY;

    private char getSymbol(int x, int y) {
        return map.get(y).charAt(x);
    }

    @Override
    protected void parsePart1() {
        map = stream().collect(Collectors.toList());
        width = map.get(0).length();
        height = map.size();
        IntStream.range(0, height).forEach(y -> {
            IntStream.range(0, width).forEach(x -> {
                if (getSymbol(x, y) == 'S') {
                    startX = x;
                    startY = y;
                }
            });
        });
        // starting point is marked as S, convert to proper pipe symbol
        char properStartingSymbol;
        if (new PlaceIdx(startX + 1, startY).symbol == '-' && new PlaceIdx(startX, startY + 1).symbol == '|') {
            properStartingSymbol = 'F';
        } else {
            throw new RuntimeException("incomplete logic of starting point substitution");
        }
        String startingLine = map.get(startY);
        map.set(startY, startingLine.substring(0, startX) + properStartingSymbol + startingLine.substring(startX + 1));
    }

    private class PlaceIdx {
        public final int x;
        public final int y;
        public final char symbol;

        public PlaceIdx(int x, int y) {
            this.x = x;
            this.y = y;
            this.symbol = (x < 0 || y < 0) ? ' ' : getSymbol(x, y);
        }

        public int getIdx() {
            return y * width + x;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getIdx());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlaceIdx placeIdx = (PlaceIdx) o;
            return x == placeIdx.x && y == placeIdx.y;
        }
    }
    private final PlaceIdx PIDX_NULL = new PlaceIdx(Integer.MIN_VALUE, Integer.MIN_VALUE);

    private record PipeHead(PlaceIdx head, PlaceIdx comingFrom) {}

    private List<PlaceIdx> getNext(PlaceIdx place) {
        List<PlaceIdx> result = new ArrayList<>();
        if (place.x > 0 && (place.symbol == '-' || place.symbol == '7' || place.symbol == 'J')) {
            char pLeft = getSymbol(place.x - 1, place.y);
            if (pLeft == '-' || pLeft == 'L' || pLeft == 'F') {
                result.add(new PlaceIdx(place.x - 1, place.y));
            }
        }
        if (place.x < width - 1 && (place.symbol == '-' || place.symbol == 'L' || place.symbol == 'F')) {
            char pRight = getSymbol(place.x + 1, place.y);
            if (pRight == '-' || pRight == 'J' || pRight == '7') {
                result.add(new PlaceIdx(place.x + 1, place.y));
            }
        }
        if (place.y > 0 && (place.symbol == '|' || place.symbol == 'L' || place.symbol == 'J')) {
            char pUp = getSymbol(place.x, place.y - 1);
            if (pUp == '|' || pUp == '7' || pUp == 'F') {
                result.add(new PlaceIdx(place.x, place.y - 1));
            }
        }
        if (place.y < height - 1 && (place.symbol == '|' || place.symbol == 'F' || place.symbol == '7')) {
            char pDown = getSymbol(place.x, place.y + 1);
            if (pDown == '|' || pDown == 'J' || pDown == 'L') {
                result.add(new PlaceIdx(place.x, place.y + 1));
            }
        }
        return result;
    }

    private final Map<PlaceIdx, Integer> distanceFromS = new HashMap<>(); // place idx -> distance from S

    @Override
    protected Object solvePart1() {
        List<PipeHead> pipeHeads = new ArrayList<>(); // pipes most advanced place idx when searching
        PlaceIdx start = new PlaceIdx(startX, startY);
        pipeHeads.add(new PipeHead(start, PIDX_NULL));
        distanceFromS.put(start, 0);
        boolean pipeEndsMet = false;
        int result = 0;
        do {
            List<PipeHead> newPipeHeads = new ArrayList<>(pipeHeads.size());
            for (PipeHead ph : pipeHeads) {
                int phDistance = distanceFromS.get(ph.head);
                for (PlaceIdx pipeLead : getNext(ph.head)) {
                    if (pipeLead.equals(ph.comingFrom)) {
                        continue; // skip pipe leading to this place
                    }
                    newPipeHeads.add(new PipeHead(pipeLead, ph.head));
                    if (distanceFromS.containsKey(pipeLead)) {
                        pipeEndsMet = true;
                        result = phDistance + 1;
                    } else {
                        distanceFromS.put(pipeLead, phDistance + 1);
                    }
                }
            }
            pipeHeads = newPipeHeads;
        } while (!pipeEndsMet);
        return result;
    }

    private final List<String> mapOnlyLoop = new ArrayList<>();

    @Override
    protected void parsePart2() {
        // create copy of map with only the loop visible
        IntStream.range(0, height).forEach(y -> {
            var sb = new StringBuilder(width);
            IntStream.range(0, width).forEach(x -> {
               PlaceIdx pi = new PlaceIdx(x, y);
               int distance = distanceFromS.getOrDefault(pi, Integer.MIN_VALUE);
               sb.append(distance >= 0 ? pi.symbol : '.');
            });
            mapOnlyLoop.add(sb.toString());
        });
        // save the loop map
        saveMap(mapOnlyLoop, "_map_only_loop");
    }

    private void saveMap(List<String> map, String outputSuffix) {
        try {
            try (BufferedWriter testWriter = Files.newBufferedWriter(Path.of("inputs/%s".formatted(getInputPath(getInputSuffix() + outputSuffix))))) {
                map.forEach(line -> {
                    try {
                        testWriter.write(line);
                        testWriter.write("\r\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<PlaceIdx, Boolean> placesInside = new HashMap<>();

    private boolean isPlaceInside(PlaceIdx place) {
        if (distanceFromS.containsKey(place)) {
            return false; // the loop is outside
        }
        // go along the diagonal towards 0, 0 using map with only the loop visible
        // count edges crossed - including "enclosing" corners
        PlaceIdx current = place;
        int edges = 0;
        while (current.x > 0 && current.y > 0) {
            current = new PlaceIdx(current.x - 1, current.y - 1);
            // we must test if current place is part of the loop because current.symbol is based on original map with redundant pipe symbols
            if (distanceFromS.containsKey(current) && (current.symbol == '|' || current.symbol == '-' || current.symbol == 'F' || current.symbol == 'J')) {
                edges++; // simple edge
            }
        }
        return (edges & 1) == 1; // place is inside if number of edge crossings is odd
    }

    @Override
    protected Object solvePart2() {
        IntStream.range(0, height).forEach(y -> {
            IntStream.range(0, width).forEach(x -> {
                PlaceIdx pi = new PlaceIdx(x, y);
                if (isPlaceInside(pi)) {
                    placesInside.put(pi, true);
                }
            });
        });
        boolean saveMapLoopAndInside = false;
        if (saveMapLoopAndInside) {
            // create copy of map with only the loop and inside places visible
            List<String> mapLoopAndInside = new ArrayList<>();
            IntStream.range(0, height).forEach(y -> {
                var sb = new StringBuilder(width);
                IntStream.range(0, width).forEach(x -> {
                    PlaceIdx pi = new PlaceIdx(x, y);
                    int distance = distanceFromS.getOrDefault(pi, Integer.MIN_VALUE);
                    sb.append(distance >= 0 ? pi.symbol : placesInside.containsKey(pi) ? 'I' : '.');
                });
                mapLoopAndInside.add(sb.toString());
            });
            saveMap(mapLoopAndInside, "_map_inside");
        }
        int result = placesInside.size();
        return result;
    }
}
/*

--- Day 10: Pipe Maze ---

You use the hang glider to ride the hot air from Desert Island all the way up to the floating metal island. This island is surprisingly cold and there definitely aren't any thermals to glide on, so you leave your hang glider behind.

You wander around for a while, but you don't find any people or animals. However, you do occasionally find signposts labeled "Hot Springs" pointing in a seemingly consistent direction; maybe you can find someone at the hot springs and ask them where the desert-machine parts are made.

The landscape here is alien; even the flowers and trees are made of metal. As you stop to admire some metal grass, you notice something metallic scurry away in your peripheral vision and jump into a big pipe! It didn't look like any animal you've ever seen; if you want a better look, you'll need to get ahead of it.

Scanning the area, you discover that the entire field you're standing on is densely packed with pipes; it was hard to tell at first because they're the same metallic silver color as the "ground". You make a quick sketch of all of the surface pipes you can see (your puzzle input).

The pipes are arranged in a two-dimensional grid of tiles:

    | is a vertical pipe connecting north and south.
    - is a horizontal pipe connecting east and west.
    L is a 90-degree bend connecting north and east.
    J is a 90-degree bend connecting north and west.
    7 is a 90-degree bend connecting south and west.
    F is a 90-degree bend connecting south and east.
    . is ground; there is no pipe in this tile.
    S is the starting position of the animal; there is a pipe on this tile, but your sketch doesn't show what shape the pipe has.

Based on the acoustics of the animal's scurrying, you're confident the pipe that contains the animal is one large, continuous loop.

For example, here is a square loop of pipe:

.....
.F-7.
.|.|.
.L-J.
.....

If the animal had entered this loop in the northwest corner, the sketch would instead look like this:

.....
.S-7.
.|.|.
.L-J.
.....

In the above diagram, the S tile is still a 90-degree F bend: you can tell because of how the adjacent pipes connect to it.

Unfortunately, there are also many pipes that aren't connected to the loop! This sketch shows the same loop as above:

-L|F7
7S-7|
L|7||
-L-J|
L|-JF

In the above diagram, you can still figure out which pipes form the main loop: they're the ones connected to S, pipes those pipes connect to, pipes those pipes connect to, and so on. Every pipe in the main loop connects to its two neighbors (including S, which will have exactly two pipes connecting to it, and which is assumed to connect back to those two pipes).

Here is a sketch that contains a slightly more complex main loop:

..F7.
.FJ|.
SJ.L7
|F--J
LJ...

Here's the same example sketch with the extra, non-main-loop pipe tiles also shown:

7-F7-
.FJ|7
SJLL7
|F--J
LJ.LJ

If you want to get out ahead of the animal, you should find the tile in the loop that is farthest from the starting position. Because the animal is in the pipe, it doesn't make sense to measure this by direct distance. Instead, you need to find the tile that would take the longest number of steps along the loop to reach from the starting point - regardless of which way around the loop the animal went.

In the first example with the square loop:

.....
.S-7.
.|.|.
.L-J.
.....

You can count the distance each tile in the loop is from the starting point like this:

.....
.012.
.1.3.
.234.
.....

In this example, the farthest point from the start is 4 steps away.

Here's the more complex loop again:

..F7.
.FJ|.
SJ.L7
|F--J
LJ...

Here are the distances for each tile on that loop:

..45.
.236.
01.78
14567
23...

Find the single giant loop starting at S. How many steps along the loop does it take to get from the starting position to the point farthest from the starting position?

Your puzzle answer was 7097.

--- Part Two ---

You quickly reach the farthest point of the loop, but the animal never emerges. Maybe its nest is within the area enclosed by the loop?

To determine whether it's even worth taking the time to search for such a nest, you should calculate how many tiles are contained within the loop. For example:

...........
.S-------7.
.|F-----7|.
.||.....||.
.||.....||.
.|L-7.F-J|.
.|..|.|..|.
.L--J.L--J.
...........

The above loop encloses merely four tiles - the two pairs of . in the southwest and southeast (marked I below). The middle . tiles (marked O below) are not in the loop. Here is the same loop again with those regions marked:

...........
.S-------7.
.|F-----7|.
.||OOOOO||.
.||OOOOO||.
.|L-7OF-J|.
.|II|O|II|.
.L--JOL--J.
.....O.....

In fact, there doesn't even need to be a full tile path to the outside for tiles to count as outside the loop - squeezing between pipes is also allowed! Here, I is still within the loop and O is still outside the loop:

..........
.S------7.
.|F----7|.
.||OOOO||.
.||OOOO||.
.|L-7F-J|.
.|II||II|.
.L--JL--J.
..........

In both of the above examples, 4 tiles are enclosed by the loop.

Here's a larger example:

.F----7F7F7F7F-7....
.|F--7||||||||FJ....
.||.FJ||||||||L7....
FJL7L7LJLJ||LJ.L-7..
L--J.L7...LJS7F-7L7.
....F-J..F7FJ|L7L7L7
....L7.F7||L7|.L7L7|
.....|FJLJ|FJ|F7|.LJ
....FJL-7.||.||||...
....L---J.LJ.LJLJ...

The above sketch has many random bits of ground, some of which are in the loop (I) and some of which are outside it (O):

OF----7F7F7F7F-7OOOO
O|F--7||||||||FJOOOO
O||OFJ||||||||L7OOOO
FJL7L7LJLJ||LJIL-7OO
L--JOL7IIILJS7F-7L7O
OOOOF-JIIF7FJ|L7L7L7
OOOOL7IF7||L7|IL7L7|
OOOOO|FJLJ|FJ|F7|OLJ
OOOOFJL-7O||O||||OOO
OOOOL---JOLJOLJLJOOO

In this larger example, 8 tiles are enclosed by the loop.

Any tile that isn't part of the main loop can count as being enclosed by the loop. Here's another example with many bits of junk pipe lying around that aren't connected to the main loop at all:

FF7FSF7F7F7F7F7F---7
L|LJ||||||||||||F--J
FL-7LJLJ||||||LJL-77
F--JF--7||LJLJ7F7FJ-
L---JF-JLJ.||-FJLJJ7
|F|F-JF---7F7-L7L|7|
|FFJF7L7F-JF7|JL---7
7-L-JL7||F7|L7F-7F7|
L.L7LFJ|||||FJL7||LJ
L7JLJL-JLJLJL--JLJ.L

Here are just the tiles that are enclosed by the loop marked with I:

FF7FSF7F7F7F7F7F---7
L|LJ||||||||||||F--J
FL-7LJLJ||||||LJL-77
F--JF--7||LJLJIF7FJ-
L---JF-JLJIIIIFJLJJ7
|F|F-JF---7IIIL7L|7|
|FFJF7L7F-JF7IIL---7
7-L-JL7||F7|L7F-7F7|
L.L7LFJ|||||FJL7||LJ
L7JLJL-JLJLJL--JLJ.L

In this last example, 10 tiles are enclosed by the loop.

Figure out whether you have time to search for the nest by calculating the area within the loop. How many tiles are enclosed by the loop?

Your puzzle answer was 355.

 */