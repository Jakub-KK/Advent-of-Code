package dev.aoc.aoc2023;

import com.google.common.collect.Sets;
import dev.aoc.common.AoCUtil;
import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day08 extends Day {
    public Day08(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day08("_sample4")); // _sample1, _sample2, _sample3, _sample4, _sample5_nodeshare, _sample6_nonLCM_1, _sample6_nonLCM_2
    }

    private record Node(String l, String r) {}

    private String instructions;
    private final Map<String, Node> map = new HashMap<>();
    private final List<String> startsForPart2 = new ArrayList<>(); // part 2 starting nodes

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        List<String> input = stream().collect(Collectors.toList());
        instructions = input.removeFirst();
        if (!input.get(0).trim().isEmpty()) throw new IllegalArgumentException("no blank line after the first one of the input");
        input.removeFirst();
        input.forEach(line -> {
            String[] splitEq = line.split(" = ");
            String strLR = splitEq[1].replace("(", "").replace(")", "");
            String[] lr = strLR.split(", ");
            map.put(splitEq[0], new Node(lr[0], lr[1]));
        });
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        PathFollower pathFollower = new PathFollower(instructions, map, "AAA", 0, s -> s.equals("ZZZ"), null);
        do {
            pathFollower.step();
        } while (pathFollower.notEnded());
        return pathFollower.stepsCount();
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parsePart1();
        map.keySet().forEach(p -> {
            if (p.endsWith("A")) { // collect starting nodes for part 2
                startsForPart2.add(p);
            }
        });
        if (startsForPart2.size() > 1) {
            // analyze if LCM method works (for every start, every cycle has to be the same length)
            boolean analyze = true;
            if (analyze) {
                if (!checkIfCanUseLeastCommonMultiplierMethod())
                    throw new IllegalArgumentException("at least one start leads to cycles of differing length, cannot use least common multiplier method to solve");
                detectNodeSharing();
            }
        }
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        long[] stepsForEachStart = startsForPart2.stream().mapToLong(c -> {
            PathFollower pathFollower = new PathFollower(instructions, map, c, 0, s -> s.endsWith("Z"), null);
            do {
                pathFollower.step();
            } while (pathFollower.notEnded());
            return pathFollower.stepsCount();
        }).toArray();
        long stepsLCM = Arrays.stream(stepsForEachStart).reduce(AoCUtil::leastCommonMultiple).getAsLong();
        final boolean checkResult = false;
        if (checkResult) {
            Stream<PathFollower> paths = startsForPart2.stream().map(c -> new PathFollower(instructions, map, c, 0, s -> s.endsWith("Z"), null));
            for (long s = 0; s < stepsLCM; s++) {
                paths = paths.peek(PathFollower::step);
            }
            boolean allEnded = paths.noneMatch(PathFollower::notEnded);
            if (!allEnded) {
                throw new IllegalStateException("result %d is invalid, some paths ended in non-terminal states".formatted(stepsLCM));
            }
        }
        return stepsLCM;
    }

    private boolean checkIfCanUseLeastCommonMultiplierMethod() {
        System.out.println("Cycle length analysis, checking if:");
        System.out.println("  - first cycle length is the same as second cycle length");
        System.out.println("  - cycle length is evenly divisible by instructions length");
        System.out.printf("Instructions length is %d%n", instructions.length());
        return startsForPart2.stream().allMatch(start -> {
            List<String> visited = new ArrayList<>();
            System.out.printf("  start from %d:%s%n", 0, start);
            PathFollower pathFollower = new PathFollower(instructions, map, start, 0, s -> s.endsWith("Z"), state -> visited.add(state.nodeName));
            // first cycle
            do {
                pathFollower.step();
            } while (pathFollower.notEnded());
            int cycle1Length = pathFollower.stepsCount();
            System.out.printf("    cycle 1: from %d:%s to %d:%s steps %d (%d x %d + %d)%n", 0, visited.get(0), cycle1Length, visited.get(cycle1Length), cycle1Length, cycle1Length / instructions.length(), instructions.length(), cycle1Length % instructions.length());
            // System.out.printf("      visited: %s%n", visited);
            // System.out.printf("      unique visited: %s%n", Set.copyOf(visited));
            // if (cycle1Length == 12599) {
            //     printTree(0, start, new HashMap<>());
            //     Set<String> visitedSet = new HashSet<>(visited);
            //     visited.forEach(nodeName -> {
            //         if (visitedSet.contains(nodeName)) {
            //             visitedSet.remove(nodeName);
            //             Node node = map.get(nodeName);
            //             System.out.printf("%s = (%s, %s)%n", nodeName, node.l, node.r);
            //         }
            //     });
            // }
            // second cycle
            do {
                pathFollower.step();
            } while (pathFollower.notEnded());
            int steps = pathFollower.stepsCount();
            int cycle2Length = steps - cycle1Length;
            System.out.printf("    cycle 2: from %d:%s to %d:%s steps %d (%d x %d + %d)%n", cycle1Length, visited.get(cycle1Length), steps, visited.get(steps), cycle2Length, cycle2Length / instructions.length(), instructions.length(), cycle2Length % instructions.length());
            // System.out.printf("      visited: %s%n", visited.stream().skip(cycle1Length).toList());
            // analysis
            boolean areCyclesTheSameLength = cycle1Length == cycle2Length;
            if (!areCyclesTheSameLength) {
                System.out.printf("    LENGTH MISMATCH: cycle 1 length %d, cycle 2 length %d%n", cycle1Length, cycle2Length);
            }
            if (cycle1Length < instructions.length()) {
                throw new IllegalArgumentException("instructions longer than cycle, not implemented"); // should run as many cycles as necessary to exhaust instructions
            }
            boolean areCyclesExactlyTheSame = cycle1Length % instructions.length() == 0;
            if (!areCyclesExactlyTheSame) {
                System.out.printf("    (POSSIBLE) DIFFERENT CYCLES: position in instructions after first cycle is %d%n", cycle1Length % instructions.length());
            }
            return areCyclesTheSameLength/* && areCyclesExactlyTheSame*/; // can use LCM method for this start
        });
    }

    private void printTree(int level, String nodeName, Map<String, Integer> visitedNodeLevels) {
        visitedNodeLevels.put(nodeName, level);
        String indent = level == 0 ? "" : IntStream.range(0, level).mapToObj(i -> "  ").reduce((a, b) -> a + b).get();
        System.out.printf("%s%02d:%s = (%n", indent, level, nodeName);
        Node node = map.get(nodeName);
        if (visitedNodeLevels.containsKey(node.l)) {
            System.out.printf("%s  %02d:%s%n", indent, visitedNodeLevels.get(node.l), node.l);
        } else {
            printTree(level + 1, node.l, visitedNodeLevels);
        }
        if (visitedNodeLevels.containsKey(node.r)) {
            System.out.printf("%s  %02d:%s%n", indent, visitedNodeLevels.get(node.r), node.r);
        } else {
            printTree(level + 1, node.r, visitedNodeLevels);
        }
        System.out.printf("%s)%n", indent);
    }

    private void detectNodeSharing() {
        // test if different starts have nodes in common
        List<Set<String>> nodesVisitedFromStarts = startsForPart2.stream().map(start -> {
            List<String> visited = new ArrayList<>(List.of(start));
            PathFollower pathFollower = new PathFollower(instructions, map, start, 0, s -> s.endsWith("Z"), state -> visited.add(state.nodeName));
            do {
                pathFollower.step();
            } while (pathFollower.notEnded());
            return Set.copyOf(visited);
        }).toList();
        if (nodesVisitedFromStarts.size() > 1) {
            List<Integer> nodesCommon = new ArrayList<>();
            IntStream.range(0, nodesVisitedFromStarts.size() - 1).forEach(i -> {
                IntStream.range(i + 1, nodesVisitedFromStarts.size()).forEach(j -> {
                    nodesCommon.add(Sets.intersection(nodesVisitedFromStarts.get(i), nodesVisitedFromStarts.get(j)).size());
                });
            });
            System.out.printf("Cycles share some nodes: %b%n".formatted(nodesCommon.stream().anyMatch(i -> i > 0)));
        }
    }

    private static class PathFollower {
        private final String instructions;
        private final Map<String, Node> map;
        private String currNodeName;
        private int currStepIdx;

        private final Predicate<String> isEnd;

        public record State(String nodeName, int stepIdx, Node node) {}

        private final Consumer<State> visitNode;

        public PathFollower(String instructions, Map<String, Node> map, String currNodeName, int currStepIdx, Predicate<String> isEnd, Consumer<State> visitNode) {
            this.instructions = instructions;
            this.map = map;
            this.currNodeName = currNodeName;
            if (!map.containsKey(currNodeName)) throw new IllegalArgumentException("starting node missing in the map");
            this.currStepIdx = currStepIdx;
            this.isEnd = isEnd != null ? isEnd : s -> true;
            this.visitNode = visitNode;
            visit(); // visit start node
        }

        public void step() {
            Node currNode = map.get(currNodeName);
            currNodeName = instructions.charAt(currStepIdx % instructions.length()) == 'L' ? currNode.l : currNode.r;
            currStepIdx++;
            visit();
        }

        public int stepsCount() {
            return currStepIdx;
        }

        public boolean notEnded() {
            return !isEnd.test(currNodeName);
        }

        private void visit() {
            if (visitNode != null) {
                visitNode.accept(new State(currNodeName, currStepIdx, map.get(currNodeName)));
            }
        }
    }

    public static class Day08Test {
        @Test
        void solvePart1_sample1() {
            var day = new Day08("_sample1");
            day.parsePart1();
            assertEquals(2, day.solvePart1());
        }

        @Test
        void solvePart1_sample2() {
            var day = new Day08("_sample2");
            day.parsePart1();
            assertEquals(6, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day08("");
            day.parsePart1();
            assertEquals(12599, day.solvePart1());
        }

        @Test
        void solvePart2_sample3() {
            var day = new Day08("_sample3");
            day.parsePart2();
            assertEquals(6L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day08("");
            day.parsePart2();
            assertEquals(8245452805243L, day.solvePart2());
        }
    }
}
// additional information about the problem and its generalization available below problem description
/*

--- Day 8: Haunted Wasteland ---

You're still riding a camel across Desert Island when you spot a sandstorm quickly approaching. When you turn to warn the Elf, she disappears before your eyes! To be fair, she had just finished warning you about ghosts a few minutes ago.

One of the camel's pouches is labeled "maps" - sure enough, it's full of documents (your puzzle input) about how to navigate the desert. At least, you're pretty sure that's what they are; one of the documents contains a list of left/right instructions, and the rest of the documents seem to describe some kind of network of labeled nodes.

It seems like you're meant to use the left/right instructions to navigate the network. Perhaps if you have the camel follow the same instructions, you can escape the haunted wasteland!

After examining the maps for a bit, two nodes stick out: AAA and ZZZ. You feel like AAA is where you are now, and you have to follow the left/right instructions until you reach ZZZ.

This format defines each node of the network individually. For example:

RL

AAA = (BBB, CCC)
BBB = (DDD, EEE)
CCC = (ZZZ, GGG)
DDD = (DDD, DDD)
EEE = (EEE, EEE)
GGG = (GGG, GGG)
ZZZ = (ZZZ, ZZZ)

Starting with AAA, you need to look up the next element based on the next left/right instruction in your input. In this example, start with AAA and go right (R) by choosing the right element of AAA, CCC. Then, L means to choose the left element of CCC, ZZZ. By following the left/right instructions, you reach ZZZ in 2 steps.

Of course, you might not find ZZZ right away. If you run out of left/right instructions, repeat the whole sequence of instructions as necessary: RL really means RLRLRLRLRLRLRLRL... and so on. For example, here is a situation that takes 6 steps to reach ZZZ:

LLR

AAA = (BBB, BBB)
BBB = (AAA, ZZZ)
ZZZ = (ZZZ, ZZZ)

Starting at AAA, follow the left/right instructions. How many steps are required to reach ZZZ?

Your puzzle answer was 12599.

--- Part Two ---

The sandstorm is upon you and you aren't any closer to escaping the wasteland. You had the camel follow the instructions, but you've barely left your starting position. It's going to take significantly more steps to escape!

What if the map isn't for people - what if the map is for ghosts? Are ghosts even bound by the laws of spacetime? Only one way to find out.

After examining the maps a bit longer, your attention is drawn to a curious fact: the number of nodes with names ending in A is equal to the number ending in Z! If you were a ghost, you'd probably just start at every node that ends with A and follow all of the paths at the same time until they all simultaneously end up at nodes that end with Z.

For example:

LR

11A = (11B, XXX)
11B = (XXX, 11Z)
11Z = (11B, XXX)
22A = (22B, XXX)
22B = (22C, 22C)
22C = (22Z, 22Z)
22Z = (22B, 22B)
XXX = (XXX, XXX)

Here, there are two starting nodes, 11A and 22A (because they both end with A). As you follow each left/right instruction, use that instruction to simultaneously navigate away from both nodes you're currently on. Repeat this process until all of the nodes you're currently on end with Z. (If only some of the nodes you're on end with Z, they act like any other node and you continue as normal.) In this example, you would proceed as follows:

    Step 0: You are at 11A and 22A.
    Step 1: You choose all of the left paths, leading you to 11B and 22B.
    Step 2: You choose all of the right paths, leading you to 11Z and 22C.
    Step 3: You choose all of the left paths, leading you to 11B and 22Z.
    Step 4: You choose all of the right paths, leading you to 11Z and 22B.
    Step 5: You choose all of the left paths, leading you to 11B and 22C.
    Step 6: You choose all of the right paths, leading you to 11Z and 22Z.

So, in this example, you end up entirely on nodes that end in Z after 6 steps.

Simultaneously start on every node that ends with A. How many steps does it take before you're only on nodes that end with Z?

Your puzzle answer was 8245452805243.

 */
/*

source: https://old.reddit.com/r/adventofcode/comments/18e6vdf/2023_day_8_part_2_an_explanation_for_why_the/

As you're probably aware if you've solved it, yesterday's puzzle can be solved by finding the length of a certain loop from each starting node, and then finding the least common multiple (LCM) of these lengths. However, as many have noted, the reason this works is that the inputs are carefully crafted so that certain conditions are satisfied. Here, I will discuss these conditions and explain what would be different in other puzzle inputs.
What loops?

To start, we need to see why we are looking for loops at all. As we traverse through the maze from a starting position, our next step is influenced by two things: our current position (node), and which instruction to execute next. So we are moving through a state space of pairs (n, i) where n is a node and i is an integer, mod the length of the instructions string, which is the index of the next instruction.

Since there are a finite number of possible states, any path through this state space will eventually loop. Once our path reaches the same state twice, we know that our path will loop from there forever. Let l be the length of this loop. If any of these states is an end node, then we know we will get back to that node again in l steps. If it took a steps to reach this state, then in the language of modular arithmetic, numbers of steps satisfying x ≡ a (mod l) will end up at this state, and hence this end node.

It's worth noting that there could be multiple states ending up at an end node during this loop, leading to multiple modular conditions, only one of which need be satisfied.
Let's have an example

Let's say our input was the following:

LRR

BBA = (BBB, XXX)
BBB = (XXX, BBZ)
BBC = (BBZ, BBC)
BBZ = (BBC, BBZ)
XXX = (XXX, XXX)

Starting from a state of (BBA, 0), we end up taking a path through state space pictured here. It takes two steps to reach the loop, and the loop has a length of 6. There are three states that include the end node, first reached in 2, 3, and 7 steps respectively. So after x steps, where x is either 2, 3, or 7 (equivalently 1) mod 6, we'll be at an end node.

Hopefully from this example you can convince yourself that any start node could end up with lots of different sets of modular conditions depending on the maps, mod the loop length l for that start node. Also consider that the loop above could have included multiple end nodes (e.g. AAZ, CCZ, ...) further complicating matters.
What's special about Day 8's input?

Yesterday's input is specially crafted so that, for each start node, there is a single state on the loop that includes an end node, and this state is reached after exactly l steps. Thus, our set of modular conditions is always just a single condition, and it is always x ≡ l (mod l), or equivalently x ≡ 0 (mod l). In other words, the condition is simply that x is a multiple of l.

Under these special circumstances, the puzzle reduces to the series of conditions that x must be a multiple of l for each of the loop lengths l of each of the start nodes. The answer, of course, is the least common multiple of all these loop lengths.
What would a solution look like in general?

Under other circumstances, we would need to instead solve series of modular equivalences, like the following:

x ≡ a1 (mod l1)
x ≡ a2 (mod l2)
x ≡ a3 (mod l3)
...

These equivalences can sometimes be solved under a generalization of the Chinese Remainder Theorem (the CRT requires that the l1, l2, l3, ... must be pairwise coprime, which may not be the case in a given puzzle input).

Furthermore, as each start node had multiple values for a that work (in our example these were 2, 3, and 7), we would need to solve these series of equivalences individually for all possible choices of a1, a2, a3, .... After solving all of these, we would pick the minimum solution among all solutions of all systems of equivalences.
Conclusion

Yesterday's puzzle inputs were specifically constrained to greatly simplify the complexity of the problem. The more general problem would also be a fair puzzle, and solvable using the above analysis, but it would be more challenging, and inputs would need to be checked to make sure that solutions did indeed exist.

 */