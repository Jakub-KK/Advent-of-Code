package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day05 extends Day {
    public Day05(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day05("_sample")); // _sample
    }

    private long[] seeds = null;
    private List<SeedRange> seedRanges;
    private final AlmanacRange[][] almanac = new AlmanacRange[7][];

    @SolutionParser(partNumber = 1)
    protected void parsePart1() {
        inputParse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // map seed to location
        long[] seed2loc = Arrays.stream(seeds)
                .map(s -> propagateValue(almanac[chapters.get("seed2soil")], s))
                .map(s -> propagateValue(almanac[chapters.get("soil2fert")], s))
                .map(s -> propagateValue(almanac[chapters.get("fert2water")], s))
                .map(s -> propagateValue(almanac[chapters.get("water2light")], s))
                .map(s -> propagateValue(almanac[chapters.get("light2temp")], s))
                .map(s -> propagateValue(almanac[chapters.get("temp2hum")], s))
                .map(s -> propagateValue(almanac[chapters.get("hum2loc")], s))
                .toArray();
        // System.out.println(Arrays.toString(seeds));
        // System.out.println(Arrays.toString(seed2loc));
        return Arrays.stream(seed2loc)
                .min()
                .getAsLong();
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        inputParse();
        seedRanges = new ArrayList<>(seeds.length / 2);
        for (int s = 0; s < seeds.length / 2; s++) {
            seedRanges.add(new SeedRange(seeds[2 * s], seeds[2 * s +1]));
        }
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        Stream<SeedRange> srStream = seedRanges.stream();
        for (int ch = 0; ch < almanac.length; ch++) {
            int _ch = ch;
            srStream = srStream.flatMap(sr -> propagateRange(almanac[_ch], sr));
        }
        return srStream
                .mapToLong(sr -> sr.start)
                .min()
                .getAsLong();
    }

    private ArrayList<AlmanacRange> currChapterArr;
    private int currChapter;
    private void inputParse() {
        Stream<String> stream = Stream.concat(stream(), Arrays.stream(new String[]{""})); // add guard for easier parsing (it pushes last chapter to almanac)
        inputParse(stream);
    }
    private void inputParse(Stream<String> stream) {
        seeds = null;
        currChapterArr = new ArrayList<>();
        currChapter = -1;
        stream.forEach(line -> {
            if (seeds == null) {
                int posStart = line.indexOf(": ") + 2;
                seeds = Arrays.stream(line.substring(posStart).split(" "))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .mapToLong(Long::longValue)
                        .toArray();
            } else {
                if (line.isEmpty()) {
                    if (currChapter >= 0) {
                        // finalize chapter to almanac
                        almanac[currChapter] = currChapterArr.stream()
                                .sorted(Comparator.comparingLong(AlmanacRange::src))
                                .toArray(AlmanacRange[]::new);
                    }
                } else if (line.endsWith("map:")) {
                    // start new chapter
                    currChapterArr.clear();
                    currChapter++;
                } else {
                    // ingest almanac ranges to current chapter
                    long[] range = Arrays.stream(line.split(" "))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Long::parseLong)
                            .mapToLong(Long::longValue)
                            .toArray();
                    currChapterArr.add(new AlmanacRange(range[0], range[1], range[2]));
                }
            }
        });
        inputOverlapCheck();
    }

    private void inputOverlapCheck() {
        boolean overlap = false;
        for (int ch = 0; ch < almanac.length; ch++) {
            AlmanacRange[] chr = almanac[ch];
            for (int r = 0; r < chr.length - 1; r++) {
                if (chr[r].src + chr[r].len > chr[r + 1].src) {
                    System.out.printf("overlap: ch %d, r %d", ch, r);
                    overlap = true;
                }
            }
        }
        if (overlap) {
            throw new RuntimeException("Overlapping ranges detected in input, not supported");
        }
    }

    private final Map<String, Integer> chapters = Map.of(
            "seed2soil", 0,
            "soil2fert", 1,
            "fert2water", 2,
            "water2light", 3,
            "light2temp", 4,
            "temp2hum", 5,
            "hum2loc", 6
    );

    private record AlmanacRange(long dst, long src, long len) {}

    private record SeedRange(long start, long len) {}

    private static long propagateValue(AlmanacRange[] chapter, long src) {
        int r = 0;
        while (r < chapter.length) {
            if (src < chapter[r].src) {
                return src;
            } else if (src >= chapter[r].src && src < chapter[r].src + chapter[r].len) {
                return chapter[r].dst + (src - chapter[r].src);
            } else {
                r++;
            }
        }
        return src;
    }

    // See "propagateRange_List" for the same method but accumulating results in ArrayList and returning stream from it
    // instead of using custom Iterator to return elements as they are requested
    // see: https://stackoverflow.com/questions/24511052/how-to-convert-an-iterator-to-a-stream/24511534#24511534
    // see: https://stackoverflow.com/questions/63377723/create-a-stream-that-is-based-on-a-custom-generator-iterator-method
    private Stream<SeedRange> propagateRange(AlmanacRange[] chapter, SeedRange s) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        propagateRangeIterator(chapter, s),
                        Spliterator.IMMUTABLE | Spliterator.NONNULL
                ),
                false
        );
        // using Guava (the same as Iterable below)
        // return Streams.stream(propagateRangeIterator(chapter, s));
        // using Iterable (worse than manual Spliterator, omits spliterator characteristics)
        // Iterable<SeedRange> propagateRangeIterable = () -> propagateRangeIterator(chapter, s);
        // return StreamSupport.stream(propagateRangeIterable.spliterator(), false);
    }

    // Custom Iterator that produces mapped SeedRange-s from given SeedRange using almanac chapter mapping
    // As almanac chapter mappings may overlap SeedRange many SeedRanges may be produced
    // Usage of Iterator allows for returning results using Stream API instead of collecting them first and returning afterwards
    private static Iterator<SeedRange> propagateRangeIterator(AlmanacRange[] chapter, SeedRange s) {
        return new Iterator<>() {
            long cs = s.start, cl = s.len;
            int r = 0;
            boolean ended = false;
            SeedRange next = null;

            @Override
            public boolean hasNext() {
                if (ended)
                    return false;
                while (r < chapter.length) {
                    if (cs < chapter[r].src) {
                        if (cs + cl <= chapter[r].src) {
                            next = new SeedRange(cs, cl);
                            ended = true;
                            break;
                        } else {
                            long len = chapter[r].src - cs;
                            next = new SeedRange(cs, len);
                            cs = chapter[r].src;
                            cl -= len;
                            break;
                        }
                    } else if (cs >= chapter[r].src && cs < chapter[r].src + chapter[r].len) {
                        if (cs + cl <= chapter[r].src + chapter[r].len) {
                            next = new SeedRange(chapter[r].dst + (cs - chapter[r].src), cl);
                            ended = true;
                            break;
                        } else {
                            long len = chapter[r].src + chapter[r].len - cs;
                            next = new SeedRange(chapter[r].dst + (cs - chapter[r].src), len);
                            cs = chapter[r].src + chapter[r].len;
                            cl -= len;
                            r++;
                            break;
                        }
                    } else {
                        r++;
                    }
                }
                if (next == null) {
                    if (r >= chapter.length) {
                        next = new SeedRange(cs, cl);
                        ended = true;
                    }
                }
                return next != null;
            }

            @Override
            public SeedRange next() {
                SeedRange result = next;
                next = null;
                return result;
            }
        };
    }

    // See "propagateRange" for the same method implemented as custom Stream "generator"
    // This shows how to collect all results and return them afterwards - not recommended, better embrace Streams
    private static Stream<SeedRange> propagateRange_CollectFirst(AlmanacRange[] chapter, SeedRange s) {
        var result = new ArrayList<SeedRange>();
        propagateRangeIterator(chapter, s).forEachRemaining(result::add);
        return result.stream();
    }

    public static class Day05Test {
        @Test
        void solvePart1_sample() {
            var day = new Day05("_sample");
            day.parsePart1();
            assertEquals(35L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day05("");
            day.parsePart1();
            assertEquals(510109797L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day05("_sample");
            day.parsePart2();
            assertEquals(46L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day05("");
            day.parsePart2();
            assertEquals(9622622L, day.solvePart2());
        }
    }
}
/*

--- Day 5: If You Give A Seed A Fertilizer ---

You take the boat and find the gardener right where you were told he would be: managing a giant "garden" that looks more to you like a farm.

"A water source? Island Island is the water source!" You point out that Snow Island isn't receiving any water.

"Oh, we had to stop the water because we ran out of sand to filter it with! Can't make snow with dirty water. Don't worry, I'm sure we'll get more sand soon; we only turned off the water a few days... weeks... oh no." His face sinks into a look of horrified realization.

"I've been so busy making sure everyone here has food that I completely forgot to check why we stopped getting more sand! There's a ferry leaving soon that is headed over in that direction - it's much faster than your boat. Could you please go check it out?"

You barely have time to agree to this request when he brings up another. "While you wait for the ferry, maybe you can help us with our food production problem. The latest Island Island Almanac just arrived and we're having trouble making sense of it."

The almanac (your puzzle input) lists all of the seeds that need to be planted. It also lists what type of soil to use with each kind of seed, what type of fertilizer to use with each kind of soil, what type of water to use with each kind of fertilizer, and so on. Every type of seed, soil, fertilizer and so on is identified with a number, but numbers are reused by each category - that is, soil 123 and fertilizer 123 aren't necessarily related to each other.

For example:

seeds: 79 14 55 13

seed-to-soil map:
50 98 2
52 50 48

soil-to-fertilizer map:
0 15 37
37 52 2
39 0 15

fertilizer-to-water map:
49 53 8
0 11 42
42 0 7
57 7 4

water-to-light map:
88 18 7
18 25 70

light-to-temperature map:
45 77 23
81 45 19
68 64 13

temperature-to-humidity map:
0 69 1
1 0 69

humidity-to-location map:
60 56 37
56 93 4

The almanac starts by listing which seeds need to be planted: seeds 79, 14, 55, and 13.

The rest of the almanac contains a list of maps which describe how to convert numbers from a source category into numbers in a destination category. That is, the section that starts with seed-to-soil map: describes how to convert a seed number (the source) to a soil number (the destination). This lets the gardener and his team know which soil to use with which seeds, which water to use with which fertilizer, and so on.

Rather than list every source number and its corresponding destination number one by one, the maps describe entire ranges of numbers that can be converted. Each line within a map contains three numbers: the destination range start, the source range start, and the range length.

Consider again the example seed-to-soil map:

50 98 2
52 50 48

The first line has a destination range start of 50, a source range start of 98, and a range length of 2. This line means that the source range starts at 98 and contains two values: 98 and 99. The destination range is the same length, but it starts at 50, so its two values are 50 and 51. With this information, you know that seed number 98 corresponds to soil number 50 and that seed number 99 corresponds to soil number 51.

The second line means that the source range starts at 50 and contains 48 values: 50, 51, ..., 96, 97. This corresponds to a destination range starting at 52 and also containing 48 values: 52, 53, ..., 98, 99. So, seed number 53 corresponds to soil number 55.

Any source numbers that aren't mapped correspond to the same destination number. So, seed number 10 corresponds to soil number 10.

So, the entire list of seed numbers and their corresponding soil numbers looks like this:

seed  soil
0     0
1     1
...   ...
48    48
49    49
50    52
51    53
...   ...
96    98
97    99
98    50
99    51

With this map, you can look up the soil number required for each initial seed number:

    Seed number 79 corresponds to soil number 81.
    Seed number 14 corresponds to soil number 14.
    Seed number 55 corresponds to soil number 57.
    Seed number 13 corresponds to soil number 13.

The gardener and his team want to get started as soon as possible, so they'd like to know the closest location that needs a seed. Using these maps, find the lowest location number that corresponds to any of the initial seeds. To do this, you'll need to convert each seed number through other categories until you can find its corresponding location number. In this example, the corresponding types are:

    Seed 79, soil 81, fertilizer 81, water 81, light 74, temperature 78, humidity 78, location 82.
    Seed 14, soil 14, fertilizer 53, water 49, light 42, temperature 42, humidity 43, location 43.
    Seed 55, soil 57, fertilizer 57, water 53, light 46, temperature 82, humidity 82, location 86.
    Seed 13, soil 13, fertilizer 52, water 41, light 34, temperature 34, humidity 35, location 35.

So, the lowest location number in this example is 35.

What is the lowest location number that corresponds to any of the initial seed numbers?

Your puzzle answer was 510109797.

--- Part Two ---

Everyone will starve if you only plant such a small number of seeds. Re-reading the almanac, it looks like the seeds: line actually describes ranges of seed numbers.

The values on the initial seeds: line come in pairs. Within each pair, the first value is the start of the range and the second value is the length of the range. So, in the first line of the example above:

seeds: 79 14 55 13

This line describes two ranges of seed numbers to be planted in the garden. The first range starts with seed number 79 and contains 14 values: 79, 80, ..., 91, 92. The second range starts with seed number 55 and contains 13 values: 55, 56, ..., 66, 67.

Now, rather than considering four seed numbers, you need to consider a total of 27 seed numbers.

In the above example, the lowest location number can be obtained from seed number 82, which corresponds to soil 84, fertilizer 84, water 84, light 77, temperature 45, humidity 46, and location 46. So, the lowest location number is 46.

Consider all of the initial seed numbers listed in the ranges on the first line of the almanac. What is the lowest location number that corresponds to any of the initial seed numbers?

Your puzzle answer was 9622622.

 */
