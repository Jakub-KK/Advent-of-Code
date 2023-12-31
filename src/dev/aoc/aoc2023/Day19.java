package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day19 extends Day {
    public Day19(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day19("_sample")); // _sample
    }

    private enum Category {
        UNKNOWN('!'), X('x'), M('m'), A('a'), S('s');

        public final char letter;
        Category(char cat) {
            this.letter = cat;
        }
        private static final Category[] values = values();
        public static Category fromValue(char catValue) {
            for (Category cat : values) {
                if (cat.letter == catValue) {
                    return cat;
                }
            }
            return Category.UNKNOWN;
        }
        public static Stream<Category> every() {
            return Stream.of(Category.X, Category.M, Category.A, Category.S);
        }
    }

    private enum Comparison {
        UNKNOWN('!'), LT('<'), GT('>');

        public final char letter;
        Comparison(char cat) {
            this.letter = cat;
        }
        private static final Comparison[] values = values();
        public static Comparison fromValue(char cmpValue) {
            for (Comparison cmp : values) {
                if (cmp.letter == cmpValue) {
                    return cmp;
                }
            }
            return Comparison.UNKNOWN;
        }

        public boolean compare(long value, long threshold) {
            return switch (letter) {
                case '<': yield value < threshold;
                case '>': yield value > threshold;
                default: throw new IllegalStateException("unknown comparison %c".formatted(letter));
            };
        }
    }

    private record Rule(Category category, Comparison comparison, long value, String workflowName) {
        public Pair<PartRatingsRange, PartRatingsRange> split(PartRatingsRange partRatingsRange) {
            RatingRange ratingRange = partRatingsRange.ratingsRanges.get(category);
            boolean isStart = comparison.compare(ratingRange.startInclusive, value);
            boolean isEnd = comparison.compare(ratingRange.endInclusive, value);
            if (isStart && isEnd) {
                return new Pair<>(partRatingsRange, null);
            } else if (!isStart && !isEnd) {
                return new Pair<>(null, partRatingsRange);
            } else { // isStart ^ isEnd
                // [s,e] + [<,V] | s<V,V<=e = [s,v-1] + [v,e]
                // [s,e] + [<,V] | V<=s,e<V = empty (impossible)
                // [s,e] + [>,V] | V<s,e<=V = empty (impossible)
                // [s,e] + [>,V] | s<=V,V<e = [s,v] + [v+1,e]
                if (comparison == Comparison.LT) { // LT,<
                    return new Pair<>(
                            partRatingsRange.getModified(category, new RatingRange(ratingRange.startInclusive, value - 1)),
                            partRatingsRange.getModified(category, new RatingRange(value, ratingRange.endInclusive))
                    );
                } else { // GT,>
                    return new Pair<>(
                            partRatingsRange.getModified(category, new RatingRange(value + 1, ratingRange.endInclusive)),
                            partRatingsRange.getModified(category, new RatingRange(ratingRange.startInclusive, value))
                    );
                }
            }
        }

        public boolean isTerminal() {
            return category == Category.UNKNOWN;
        }
        public boolean isAccept() {
            return Workflow.isAccept(workflowName);
        }

        public boolean isReject() {
            return Workflow.isReject(workflowName);
        }

        @Override
        public String toString() {
            return isTerminal() ?
                    workflowName :
                    "%c%c%d:%s".formatted(category.letter, comparison.letter, value, workflowName)
                    ;
        }
    }

    private record Workflow(String name, List<Rule> rules) {
        public static boolean isAccept(String workflowName) {
            return workflowName.equals("A");
        }
        public static boolean isReject(String workflowName) {
            return workflowName.equals("R");
        }

        private static final Pattern workflowTop = Pattern.compile("(\\w+?)\\{(.+)\\}");
        private static final Pattern workflowRule = Pattern.compile("(?:([xmas])([<>])(\\d+):(\\w+))|(\\w+)");
        public static Workflow parse(String line) {
            Matcher matcherWorkflowTop = workflowTop.matcher(line);
            if (!matcherWorkflowTop.matches()) {
                throw new IllegalArgumentException("failed workflow top match at line \"%s\"".formatted(line));
            }
            String workflowName = matcherWorkflowTop.group(1);
            String workflowRulesStr = matcherWorkflowTop.group(2);
            String[] workflowRulesStrArr = workflowRulesStr.split(",");
            try {
                List<Rule> workflowRules = Arrays.stream(workflowRulesStrArr).map(Workflow::parseRule).toList();
                return new Workflow(workflowName, workflowRules);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage() + " at line \"%s\"".formatted(line));
            }
        }
        private static Rule parseRule(String ruleStr) {
            Matcher matcherRule = workflowRule.matcher(ruleStr);
            if (!matcherRule.matches()) {
                throw new IllegalArgumentException("failed workflow rule match for \"%s\"".formatted(ruleStr));
            }
            Rule rule;
            if (matcherRule.group(5) != null) {
                rule = new Rule(Category.UNKNOWN, Comparison.UNKNOWN, 0, matcherRule.group(5));
            } else {
                rule = new Rule(
                        Category.fromValue(matcherRule.group(1).charAt(0)),
                        Comparison.fromValue(matcherRule.group(2).charAt(0)),
                        Long.parseLong(matcherRule.group(3)),
                        matcherRule.group(4)
                );
            }
            return rule;
        }

        @Override
        public String toString() {
            return "%s{%s}".formatted(name, String.join(",", rules.stream().map(Rule::toString).toList()));
        }
    }

    private final Map<String, Workflow> workflows = new HashMap<>();

    private record Part(Map<Category, Long> ratings) {
        public long getSumRatings() {
            return Category.every().mapToLong(cat -> ratings.getOrDefault(cat, 0L)).sum();
        }

        private static final Pattern partTop = Pattern.compile("\\{(.+)\\}");
        private static final Pattern partRating = Pattern.compile("([xmas])=(\\d+)");
        private static Part parse(String line) {
            Matcher matcherPartTop = partTop.matcher(line);
            if (!matcherPartTop.matches()) {
                throw new IllegalArgumentException("failed part top match at line \"%s\"".formatted(line));
            }
            String ratingsStr = matcherPartTop.group(1);
            String[] ratingsStrArr = ratingsStr.split(",");
            try {
                List<Pair<Category, Long>> partRatings = Arrays.stream(ratingsStrArr).map(Part::parseRating).toList();
                Map<Category, Long> partRatingsMap = new HashMap<>(partRatings.size());
                for (var rating : partRatings) {
                    partRatingsMap.put(rating.getValue0(), rating.getValue1());
                }
                return new Part(partRatingsMap);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage() + " at line \"%s\"".formatted(line));
            }
        }
        private static Pair<Category, Long> parseRating(String ratingStr) {
            Matcher matcherRating = partRating.matcher(ratingStr);
            if (!matcherRating.matches()) {
                throw new IllegalArgumentException("failed part rating match for \"%s\"".formatted(ratingStr));
            }
            return new Pair<>(Category.fromValue(matcherRating.group(1).charAt(0)), Long.parseLong(matcherRating.group(2)));
        }

        @Override
        public String toString() {
            return "{%s}".formatted(String.join(",", Category.every().map(cat -> "%c=%d".formatted(cat.letter, ratings.getOrDefault(cat, -1L))).toList()));
        }
    }

    private final List<Part> parts = new ArrayList<>();

    private record RatingRange(long startInclusive, long endInclusive) {
        public long getCount() {
            return endInclusive - startInclusive + 1;
        }
    }
    private record PartRatingsRange(Map<Category, RatingRange> ratingsRanges) {
        public long getCombinations() {
            return Category.every().mapToLong(cat -> ratingsRanges.get(cat).getCount()).reduce((a, v) -> a * v).getAsLong();
        }

        public static PartRatingsRange getMax() {
            return new PartRatingsRange(Category.every().collect(Collectors.toMap(cat -> cat, cat -> new RatingRange(1, 4000))));
        }

        public PartRatingsRange getModified(Category category, RatingRange ratingRange) {
            return new PartRatingsRange(ratingsRanges.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getKey() == category ? ratingRange : e.getValue())));
        }

        @Override
        public String toString() {
            return "{%s}".formatted(String.join(",", Category.every().map(cat -> "%c=<%d,%d>".formatted(cat.letter, ratingsRanges.get(cat).startInclusive, ratingsRanges.get(cat).endInclusive)).toList()));
        }
    }

    private boolean isAccepted(Part part) {
        String nextWorkflowName = "in";
        do {
            Workflow current = workflows.get(nextWorkflowName);
            for (Rule rule : current.rules) {
                if (rule.isTerminal()) {
                    nextWorkflowName = rule.workflowName;
                    break;
                } else {
                    long categoryRating = part.ratings.get(rule.category);
                    Comparison cmp = rule.comparison;
                    long threshold = rule.value;
                    if (cmp.compare(categoryRating, threshold)) {
                        nextWorkflowName = rule.workflowName;
                        break;
                    }
                }
            }
            if (nextWorkflowName.equals(current.name)) {
                throw new IllegalStateException("workflow failure in workflow \"%s\"".formatted(current.name));
            }
        } while (!Workflow.isAccept(nextWorkflowName) && !Workflow.isReject(nextWorkflowName));
        return Workflow.isAccept(nextWorkflowName);
    }

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
        // System.out.printf("Workflows: %s%n", Arrays.toString(workflows.keySet().toArray(new String[0])));
        // System.out.printf("%s%n", String.join(",\r\n", workflows.values().stream().map(Workflow::toString).toList()));
        // System.out.printf("Parts:%n%s%n", String.join(",\r\n", parts.stream().map(Part::toString).toList()));
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        long result = parts.stream().filter(this::isAccepted).mapToLong(Part::getSumRatings).sum();
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    private record WorkflowAndRatingsRange(String workflowName, PartRatingsRange ratingsRange) {}

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        List<PartRatingsRange> accepted = new ArrayList<>();
        List<WorkflowAndRatingsRange> open = new ArrayList<>();
        open.add(new WorkflowAndRatingsRange("in", PartRatingsRange.getMax()));
        while (!open.isEmpty()) {
            WorkflowAndRatingsRange currentWandRR = open.removeLast();
            PartRatingsRange rr = currentWandRR.ratingsRange;
            Workflow w = workflows.get(currentWandRR.workflowName);
            for (Rule rule : w.rules) {
                if (rule.isTerminal()) {
                    if (rule.isAccept()) {
                        accepted.add(rr);
                    } else if (!rule.isReject()) {
                        open.add(new WorkflowAndRatingsRange(rule.workflowName, rr));
                    }
                    break;
                } else {
                    Pair<PartRatingsRange, PartRatingsRange> rr2 = rule.split(rr);
                    PartRatingsRange rrForRule = rr2.getValue0();
                    if (rrForRule != null) {
                        if (rule.isAccept()) {
                            accepted.add(rrForRule);
                        } else if (!rule.isReject()) {
                            open.add(new WorkflowAndRatingsRange(rule.workflowName, rrForRule));
                        }
                    }
                    PartRatingsRange rrNotForRule = rr2.getValue1();
                    if (rrNotForRule != null) {
                        rr = rrNotForRule;
                    } else {
                        break;
                    }
                }
            }
        }
        long result = accepted.stream().mapToLong(PartRatingsRange::getCombinations).sum();
        return result;
    }

    private void parse() {
        AtomicBoolean parsingWorkflows = new AtomicBoolean(true);
        stream().forEach(line -> {
            if (line.isEmpty()) {
                parsingWorkflows.set(false);
                return;
            }
            if (parsingWorkflows.get()) {
                Workflow workflow = Workflow.parse(line);
                workflows.put(workflow.name, workflow);
            } else {
                Part part = Part.parse(line);
                parts.add(part);
            }
        });
    }

    public static class Day19Test {
        @Test
        void solvePart1_small() {
            var day = new Day19("_sample");
            day.parsePart1();
            assertEquals(19114L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day19("");
            day.parsePart1();
            assertEquals(373302L, day.solvePart1());
        }

        @Test
        void solvePart2_small() {
            var day = new Day19("_sample");
            day.parsePart2();
            assertEquals(167409079868000L, day.solvePart2());
        }

        @Test
        void solvePart2_main() {
            var day = new Day19("");
            day.parsePart2();
            assertEquals(130262715574114L, day.solvePart2());
        }
    }
}
/*

--- Day 19: Aplenty ---

The Elves of Gear Island are thankful for your help and send you on your way. They even have a hang glider that someone stole from Desert Island; since you're already going that direction, it would help them a lot if you would use it to get down there and return it to them.

As you reach the bottom of the relentless avalanche of machine parts, you discover that they're already forming a formidable heap. Don't worry, though - a group of Elves is already here organizing the parts, and they have a system.

To start, each part is rated in each of four categories:

    x: Extremely cool looking
    m: Musical (it makes a noise when you hit it)
    a: Aerodynamic
    s: Shiny

Then, each part is sent through a series of workflows that will ultimately accept or reject the part. Each workflow has a name and contains a list of rules; each rule specifies a condition and where to send the part if the condition is true. The first rule that matches the part being considered is applied immediately, and the part moves on to the destination described by the rule. (The last rule in each workflow has no condition and always applies if reached.)

Consider the workflow ex{x>10:one,m<20:two,a>30:R,A}. This workflow is named ex and contains four rules. If workflow ex were considering a specific part, it would perform the following steps in order:

    Rule "x>10:one": If the part's x is more than 10, send the part to the workflow named one.
    Rule "m<20:two": Otherwise, if the part's m is less than 20, send the part to the workflow named two.
    Rule "a>30:R": Otherwise, if the part's a is more than 30, the part is immediately rejected (R).
    Rule "A": Otherwise, because no other rules matched the part, the part is immediately accepted (A).

If a part is sent to another workflow, it immediately switches to the start of that workflow instead and never returns. If a part is accepted (sent to A) or rejected (sent to R), the part immediately stops any further processing.

The system works, but it's not keeping up with the torrent of weird metal shapes. The Elves ask if you can help sort a few parts and give you the list of workflows and some part ratings (your puzzle input). For example:

px{a<2006:qkq,m>2090:A,rfg}
pv{a>1716:R,A}
lnx{m>1548:A,A}
rfg{s<537:gd,x>2440:R,A}
qs{s>3448:A,lnx}
qkq{x<1416:A,crn}
crn{x>2662:A,R}
in{s<1351:px,qqz}
qqz{s>2770:qs,m<1801:hdj,R}
gd{a>3333:R,R}
hdj{m>838:A,pv}

{x=787,m=2655,a=1222,s=2876}
{x=1679,m=44,a=2067,s=496}
{x=2036,m=264,a=79,s=2244}
{x=2461,m=1339,a=466,s=291}
{x=2127,m=1623,a=2188,s=1013}

The workflows are listed first, followed by a blank line, then the ratings of the parts the Elves would like you to sort. All parts begin in the workflow named in. In this example, the five listed parts go through the following workflows:

    {x=787,m=2655,a=1222,s=2876}: in -> qqz -> qs -> lnx -> A
    {x=1679,m=44,a=2067,s=496}: in -> px -> rfg -> gd -> R
    {x=2036,m=264,a=79,s=2244}: in -> qqz -> hdj -> pv -> A
    {x=2461,m=1339,a=466,s=291}: in -> px -> qkq -> crn -> R
    {x=2127,m=1623,a=2188,s=1013}: in -> px -> rfg -> A

Ultimately, three parts are accepted. Adding up the x, m, a, and s rating for each of the accepted parts gives 7540 for the part with x=787, 4623 for the part with x=2036, and 6951 for the part with x=2127. Adding all of the ratings for all of the accepted parts gives the sum total of 19114.

Sort through all of the parts you've been given; what do you get if you add together all of the rating numbers for all of the parts that ultimately get accepted?

Your puzzle answer was 373302.

--- Part Two ---

Even with your help, the sorting process still isn't fast enough.

One of the Elves comes up with a new plan: rather than sort parts individually through all of these workflows, maybe you can figure out in advance which combinations of ratings will be accepted or rejected.

Each of the four ratings (x, m, a, s) can have an integer value ranging from a minimum of 1 to a maximum of 4000. Of all possible distinct combinations of ratings, your job is to figure out which ones will be accepted.

In the above example, there are 167409079868000 distinct combinations of ratings that will be accepted.

Consider only your list of workflows; the list of part ratings that the Elves wanted you to sort is no longer relevant. How many distinct combinations of ratings will be accepted by the Elves' workflows?

Your puzzle answer was 130262715574114.

 */