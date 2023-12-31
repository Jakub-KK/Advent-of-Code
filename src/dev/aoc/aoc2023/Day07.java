package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day07 extends Day {
    public Day07(String inputSuffix) {
        super(inputSuffix);
        BiConsumer<String, Map<Character, Integer>> makeMap = (chars, map) -> {
            String[] cs = chars.split("");
            IntStream.range(0, cs.length).forEach(v -> map.put(cs[v].charAt(0), v));
        };
        makeMap.accept(cardOrderPart1, cardOrderMapPart1);
        makeMap.accept(cardOrderPart2, cardOrderMapPart2);
        makeMap.accept(handOrder, handOrderMap);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day07("_sample")); // _sample, _test1, _test2
    }

    final String cardOrderPart1 = "23456789TJQKA";
    final Map<Character, Integer> cardOrderMapPart1 = new HashMap<>();

    final String cardOrderPart2 = "J23456789TQKA"; // J is joker and it's weakest
    final Map<Character, Integer> cardOrderMapPart2 = new HashMap<>();

    final String handOrder = "*123H45"; // mix, one pair, two pairs, three, house, four, five
    final Map<Character, Integer> handOrderMap = new HashMap<>();

    record HandType(char type, int[] cardCounts, int[] countsCounts) {
        // a shortcut for type calculation in part 1: expression below is unique for every type of hand
        // see: https://old.reddit.com/r/adventofcode/comments/18d0jzt/2023_day_7_an_interesting_algorithm/
        // see: https://old.reddit.com/r/adventofcode/comments/18csyvh/2023_day_7_part_1_python_ridiculously_short/
        // Arrays.stream(hb.hand.handType.cardCounts).max().getAsInt() - Arrays.stream(hb.hand.handType.cardCounts).filter(c -> c > 0).count())
    }
    record Hand(String hand, HandType handType) { }
    record HandBid(Hand hand, int bid) {}

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        return solve(this::getHandTypePart1, cardOrderMapPart1);
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        return solve(this::getHandTypePart2, cardOrderMapPart2);
    }

    private long solve(Function<String, HandType> getHandType, Map<Character, Integer> cardOrderMap) {
        var handBidsStream = stream()
                .map(line -> {
                    String[] parts = line.split("\\s");
                    var cards = parts[0];
                    var bid = parts[1];
                    return new HandBid(new Hand(cards, getHandType.apply(cards)), Integer.parseInt(bid));
                })
                .sorted((a, b) -> compareHands(a.hand, b.hand, cardOrderMap))
                ;
        // var handBids = handBidsStream.toList(); handBidsStream = handBids.stream(); // peek stream contents
        // handBids.forEach(hb -> System.out.printf("%s %c->%c %d%n", hb.hand.hand, getHandTypePart1(hb.hand.hand), hb.hand.handType, hb.bid));
        // handBids.forEach(hb -> System.out.printf("%s %d%n", hb.hand.hand, hb.bid));
        AtomicInteger idx = new AtomicInteger(0); // for ranking of stream of hands
        long result = handBidsStream
                .mapToInt(hb -> idx.incrementAndGet() * hb.bid)
                .sum()
                ;
        return result;
    }

    private int compareHands(Hand a, Hand b, Map<Character, Integer> cardOrderMap) {
        int compareTypes = handOrderMap.get(a.handType.type).compareTo(handOrderMap.get(b.handType.type));
        if (compareTypes == 0) {
            int i = 0;
            int compareCards;
            do {
                compareCards = cardOrderMap.get(a.hand.charAt(i)).compareTo(cardOrderMap.get(b.hand.charAt(i)));
            } while (++i < a.hand.length() && compareCards == 0);
            return compareCards;
        } else {
            return compareTypes;
        }
    }

    private HandType getHandTypePart1(String hand) {
        int[] cardCounts = getHandTypeCardCounts(hand, cardOrderPart1, cardOrderMapPart1);
        int[] countsCounts = getHandTypeCountsCounts(cardCounts);
        return new HandType(getHandTypeByCountsCountsPart1(countsCounts), cardCounts, countsCounts);
    }

    private HandType getHandTypePart2(String hand) {
        int[] cardCounts = getHandTypeCardCounts(hand, cardOrderPart2, cardOrderMapPart2);
        int[] countsCounts = getHandTypeCountsCounts(cardCounts);
        int jokerCount = cardCounts[cardOrderMapPart2.get('J')];
        char handTypeWithJokers = getHandTypeByCountsCountsPart2(jokerCount, countsCounts);
        // if (jokerCount > 0) {
        //     System.out.printf("%s %c %d %s%n", hand, handTypeWithJokers, jokerCount, Arrays.toString(countsCounts));
        // }
        return new HandType(handTypeWithJokers, cardCounts, countsCounts);
    }

    private static int[] getHandTypeCardCounts(String hand, String cardOrder, Map<Character, Integer> cardOrderMap) {
        if (hand.length() != 5) throw new IllegalArgumentException("hand" + hand + "too long, must be 5 cards");
        String[] handCards = hand.split("");
        int[] cardCounts = new int[cardOrder.length()];
        for (String card : handCards) {
            char c = card.charAt(0);
            if (!cardOrderMap.containsKey(c)) throw new IllegalArgumentException("unknown card " + c + " in hand " + hand);
            cardCounts[cardOrderMap.get(c)]++;
        }
        return cardCounts;
    }

    private static int[] getHandTypeCountsCounts(int[] cardCounts) {
        int[] countsCounts = new int[6];
        for (int cardCount : cardCounts) {
            if (cardCount >= countsCounts.length) throw new IllegalArgumentException();
            countsCounts[cardCount]++;
        }
        return countsCounts;
    }

    private static char getHandTypeByCountsCountsPart1(int[] countsCounts) {
        if (countsCounts[5] > 0) {
            if (countsCounts[5] != 1) throw new IllegalArgumentException();
            return '5';
        } else if (countsCounts[4] > 0) {
            if (countsCounts[4] != 1) throw new IllegalArgumentException();
            return '4';
        } else if (countsCounts[3] > 0) {
            if (countsCounts[3] != 1) throw new IllegalArgumentException();
            if (countsCounts[2] > 0) {
                if (countsCounts[2] != 1) throw new IllegalArgumentException();
                return 'H';
            } else {
                return '3';
            }
        } else if (countsCounts[2] > 0) {
            if (countsCounts[2] > 1) {
                if (countsCounts[2] != 2) throw new IllegalArgumentException();
                return '2';
            } else {
                return '1';
            }
        } else {
            return '*';
        }
    }

    private static char getHandTypeByCountsCountsPart2(int jokerCount, int[] countsCounts) {
        switch (jokerCount) {
            case 0: {
                return getHandTypeByCountsCountsPart1(countsCounts);
            }
            case 1: {
                if (countsCounts[4] > 0) {
                    if (countsCounts[4] != 1) throw new IllegalArgumentException();
                    return '5';
                } else if (countsCounts[3] > 0) {
                    if (countsCounts[3] != 1) throw new IllegalArgumentException();
                    return '4';
                } else if (countsCounts[2] > 0) {
                    if (countsCounts[2] > 1) {
                        if (countsCounts[2] != 2) throw new IllegalArgumentException();
                        return 'H';
                    } else {
                        return '3';
                    }
                } else {
                    return '1';
                }
            }
            case 2: {
                if (countsCounts[3] > 0) {
                    if (countsCounts[3] != 1) throw new IllegalArgumentException();
                    return '5';
                } else if (countsCounts[2] - 1 > 0) { // subtract own joker pair
                    if (countsCounts[2] - 1 != 1) throw new IllegalArgumentException();
                    return '4';
                } else {
                    return '3';
                }
            }
            case 3: {
                if (countsCounts[2] > 0) {
                    if (countsCounts[2] != 1) throw new IllegalArgumentException();
                    return '5';
                } else {
                    return '4';
                }
            }
            case 4:
            case 5: {
                return '5';
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    public static class Day07Test {
        @Test
        void solvePart1_sample() {
            var day = new Day07("_sample");
            day.parsePart1();
            assertEquals(6440L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day07("");
            day.parsePart1();
            assertEquals(250951660L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day07("_sample");
            day.parsePart2();
            assertEquals(5905L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day07("");
            day.parsePart2();
            assertEquals(251481660L, day.solvePart2());
        }
    }
}
/*

--- Day 7: Camel Cards ---

Your all-expenses-paid trip turns out to be a one-way, five-minute ride in an airship. (At least it's a cool airship!) It drops you off at the edge of a vast desert and descends back to Island Island.

"Did you bring the parts?"

You turn around to see an Elf completely covered in white clothing, wearing goggles, and riding a large camel.

"Did you bring the parts?" she asks again, louder this time. You aren't sure what parts she's looking for; you're here to figure out why the sand stopped.

"The parts! For the sand, yes! Come with me; I will show you." She beckons you onto the camel.

After riding a bit across the sands of Desert Island, you can see what look like very large rocks covering half of the horizon. The Elf explains that the rocks are all along the part of Desert Island that is directly above Island Island, making it hard to even get there. Normally, they use big machines to move the rocks and filter the sand, but the machines have broken down because Desert Island recently stopped receiving the parts they need to fix the machines.

You've already assumed it'll be your job to figure out why the parts stopped when she asks if you can help. You agree automatically.

Because the journey will take a few days, she offers to teach you the game of Camel Cards. Camel Cards is sort of similar to poker except it's designed to be easier to play while riding a camel.

In Camel Cards, you get a list of hands, and your goal is to order them based on the strength of each hand. A hand consists of five cards labeled one of A, K, Q, J, T, 9, 8, 7, 6, 5, 4, 3, or 2. The relative strength of each card follows this order, where A is the highest and 2 is the lowest.

Every hand is exactly one type. From strongest to weakest, they are:

    Five of a kind, where all five cards have the same label: AAAAA
    Four of a kind, where four cards have the same label and one card has a different label: AA8AA
    Full house, where three cards have the same label, and the remaining two cards share a different label: 23332
    Three of a kind, where three cards have the same label, and the remaining two cards are each different from any other card in the hand: TTT98
    Two pair, where two cards share one label, two other cards share a second label, and the remaining card has a third label: 23432
    One pair, where two cards share one label, and the other three cards have a different label from the pair and each other: A23A4
    High card, where all cards' labels are distinct: 23456

Hands are primarily ordered based on type; for example, every full house is stronger than any three of a kind.

If two hands have the same type, a second ordering rule takes effect. Start by comparing the first card in each hand. If these cards are different, the hand with the stronger first card is considered stronger. If the first card in each hand have the same label, however, then move on to considering the second card in each hand. If they differ, the hand with the higher second card wins; otherwise, continue with the third card in each hand, then the fourth, then the fifth.

So, 33332 and 2AAAA are both four of a kind hands, but 33332 is stronger because its first card is stronger. Similarly, 77888 and 77788 are both a full house, but 77888 is stronger because its third card is stronger (and both hands have the same first and second card).

To play Camel Cards, you are given a list of hands and their corresponding bid (your puzzle input). For example:

32T3K 765
T55J5 684
KK677 28
KTJJT 220
QQQJA 483

This example shows five hands; each hand is followed by its bid amount. Each hand wins an amount equal to its bid multiplied by its rank, where the weakest hand gets rank 1, the second-weakest hand gets rank 2, and so on up to the strongest hand. Because there are five hands in this example, the strongest hand will have rank 5 and its bid will be multiplied by 5.

So, the first step is to put the hands in order of strength:

    32T3K is the only one pair and the other hands are all a stronger type, so it gets rank 1.
    KK677 and KTJJT are both two pair. Their first cards both have the same label, but the second card of KK677 is stronger (K vs T), so KTJJT gets rank 2 and KK677 gets rank 3.
    T55J5 and QQQJA are both three of a kind. QQQJA has a stronger first card, so it gets rank 5 and T55J5 gets rank 4.

Now, you can determine the total winnings of this set of hands by adding up the result of multiplying each hand's bid with its rank (765 * 1 + 220 * 2 + 28 * 3 + 684 * 4 + 483 * 5). So the total winnings in this example are 6440.

Find the rank of every hand in your set. What are the total winnings?

Your puzzle answer was 250951660.

--- Part Two ---

To make things a little more interesting, the Elf introduces one additional rule. Now, J cards are jokers - wildcards that can act like whatever card would make the hand the strongest type possible.

To balance this, J cards are now the weakest individual cards, weaker even than 2. The other cards stay in the same order: A, K, Q, T, 9, 8, 7, 6, 5, 4, 3, 2, J.

J cards can pretend to be whatever card is best for the purpose of determining hand type; for example, QJJQ2 is now considered four of a kind. However, for the purpose of breaking ties between two hands of the same type, J is always treated as J, not the card it's pretending to be: JKKK2 is weaker than QQQQ2 because J is weaker than Q.

Now, the above example goes very differently:

32T3K 765
T55J5 684
KK677 28
KTJJT 220
QQQJA 483

    32T3K is still the only one pair; it doesn't contain any jokers, so its strength doesn't increase.
    KK677 is now the only two pair, making it the second-weakest hand.
    T55J5, KTJJT, and QQQJA are now all four of a kind! T55J5 gets rank 3, QQQJA gets rank 4, and KTJJT gets rank 5.

With the new joker rule, the total winnings in this example are 5905.

Using the new joker rule, find the rank of every hand in your set. What are the new total winnings?

Your puzzle answer was 251481660.

 */