package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import dev.aoc.common.graphsearch.GraphNode;
import dev.aoc.common.graphsearch.GraphStatic;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day25 extends Day {
    public Day25(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day25("")); // _sample, _test_1
    }

    private static class ComponentNode implements GraphNode {
        private static final Map<Long, ComponentNode> nodes = new HashMap<>();
        public static ComponentNode getNode(String name) {
            long id = getNodeId(name);
            return nodes.computeIfAbsent(id, key -> new ComponentNode(name));
        }
        private static long getNodeId(String name) {
            if (name.length() != 3) {
                throw new IllegalArgumentException("component name must be 3 letters");
            }
            char[] chars = name.toCharArray();
            int id = 0;
            for (char ch : chars) {
                if (ch < 'a' || 'z' < ch) {
                    throw new IllegalArgumentException("component name must be only lower-case letters");
                }
                id += (ch - 'a') + 26 * id;
            }
            return id;
        }

        private final String name;
        private final long id;

        private ComponentNode(String name) {
            this.name = name;
            this.id = getNodeId(name);
        }

        public String getName() {
            return name;
        }

        @Override
        public long getId() {
            return id;
        }

        public boolean equals(ComponentNode that) {
            return this.id == that.id;
        }

        @Override
        public boolean equalsTarget(GraphNode target) {
            return equals(target);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComponentNode that = (ComponentNode) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class ComponentGraph extends GraphStatic<ComponentNode> {
        public String toStringGrahpviz() {
            Map<Pair<Long, Long>, Pair<Long, Long>> done = new HashMap<>(nodes.size());
            return "graph G{%n%s%n}".formatted(
                    String.join("\r\n",
                            nodes.values().stream().map(n -> {
                                long nId = n.getId();
                                return !edges.containsKey(nId) ? null : String.join("\r\n",
                                        edges.get(nId).stream().map(id -> {
                                            Pair<Long, Long> connId = nId < id ? new Pair<>(nId, id) : new Pair<>(id, nId);
                                            if (done.containsKey(connId))
                                                return null;
                                            done.put(connId, connId);
                                            return "%s -- %s".formatted(n.getName(), nodes.get(id).getName());
                                        }).filter(Objects::nonNull).filter(s -> !s.isEmpty()).toList()
                                );
                            }).filter(Objects::nonNull).filter(s -> !s.isEmpty()).toList()
                    )
            );
        }

        @Override
        public String toString() {
            return String.join("\r\n", nodes.values().stream().map(n -> "%s: %s".formatted(n.getName(), String.join(" ", edges.get(n.getId()).stream().map(id -> nodes.get(id).getName()).toList()))).toList());
        }
    }

    private static <T> Set<T> setSum(Set<T> a, Set<T> b) {
        HashSet<T> sum = new HashSet<>(a);
        sum.addAll(b);
        return sum;
    }

    /**
     * Karger's algorithm is a randomized algorithm to compute a minimum cut of a connected graph.
     * see https://en.wikipedia.org/wiki/Karger%27s_algorithm
     * @param <T>
     */
    public class KargerMinimumCut<T extends GraphNode> {
        private final Random rng;
        private final GraphStatic<T> graph;
        private final Set<Pair<T, T>> graphEdges;

        public KargerMinimumCut(GraphStatic<T> graph) {
            this.rng = new Random();
            this.graph = graph;
            this.graphEdges = new HashSet<>();
            Set<T> graphNodes = graph.getNodes();
            nodes = new HashSet<>(graphNodes.size());
            edgesOfNode = new HashMap<>();
            edges = new HashMap<>();
            init(graphNodes, graphEdges);
        }
        private void init(Set<T> graphNodes, Set<Pair<T, T>> graphEdges) {
            for (T graphNode : graphNodes) {
                GroupNode<T> groupNode = new GroupNode<>(graphNode);
                nodes.add(groupNode);
                Set<T> connNodes = graph.getEdges(graphNode);
                for (T connNode : connNodes) {
                    GroupNode<T> connGroupNode = new GroupNode<>(connNode);
                    nodes.add(connGroupNode);
                    edgesOfNode.computeIfAbsent(groupNode, key -> new HashSet<>()).add(connGroupNode);
                    edgesOfNode.computeIfAbsent(connGroupNode, key -> new HashSet<>()).add(groupNode);
                    if (groupNode.compareTo(connGroupNode) < 0) {
                        var edge = new Pair<>(groupNode, connGroupNode);
                        edges.put(edge, edge);
                        if (graphEdges != null) {
                            graphEdges.add(new Pair<>(graphNode, connNode));
                        }
                    } else {
                        var edge = new Pair<>(connGroupNode, groupNode);
                        edges.put(edge, edge);
                        if (graphEdges != null) {
                            graphEdges.add(new Pair<>(connNode, graphNode));
                        }
                    }
                }
            }
        }
        public void reset() {
            nodes.clear();
            edgesOfNode.clear();
            edges.clear();
            init(graph.getNodes(), null);
        }

        private record GroupNode<T>(Set<T> nodes) implements Comparable<GroupNode<T>> {
            public GroupNode(T node) {
                this(Set.of(node));
            }
            public GroupNode(GroupNode<T> a, GroupNode<T> b) {
                this(setSum(a.nodes, b.nodes));
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                GroupNode<?> groupNode = (GroupNode<?>) o;
                return nodes.equals(groupNode.nodes);
            }

            @Override
            public int hashCode() {
                return nodes.hashCode();
            }

            @Override
            public int compareTo(GroupNode<T> o) {
                int hashCodeCmp = Integer.compare(this.hashCode(), o.hashCode());
                if (hashCodeCmp != 0) {
                    return hashCodeCmp;
                }
                int sizeCmp = Integer.compare(this.nodes.size(), o.nodes.size());
                if (sizeCmp != 0) {
                    return sizeCmp;
                }
                return Arrays.compare(this.nodes.stream().mapToLong(n -> ((GraphNode)n).getId()).toArray(), o.nodes.stream().mapToLong(n -> ((GraphNode)n).getId()).toArray());
            }
        }

        private final Set<GroupNode<T>> nodes;
        private final Map<GroupNode<T>, Set<GroupNode<T>>> edgesOfNode;
        private final Map<Pair<GroupNode<T>, GroupNode<T>>, Pair<GroupNode<T>, GroupNode<T>>> edges; // TODO HashSet !

        public record MinimumCutResult<T>(Set<Pair<T, T>> edges, Set<T> componentA, Set<T> componentB) {}

        public MinimumCutResult<T> findMinimumCut() {
            long seed = this.rng.nextLong();// winner seed for puzzle input -7902906308268857679L
            // System.out.printf("rng seed %d%n", seed);
            Random rng = new Random(seed);
            while (nodes.size() > 2) {
                // if (nodes.size() % 100 == 0) {
                //     System.out.println(nodes.size());
                // }
                int edgeIdx = rng.nextInt(edges.size());
                int i = 0;
                Pair<GroupNode<T>, GroupNode<T>> edge = null;
                for (Pair<GroupNode<T>, GroupNode<T>> edgeI : edges.values()) {
                    if (i >= edgeIdx) {
                        edge = edgeI;
                        break;
                    } else {
                        i++;
                    }
                }
                // contract edge
                GroupNode<T> nodeA = edge.getValue0();
                GroupNode<T> nodeB = edge.getValue1();
                GroupNode<T> sumNode = new GroupNode<>(nodeA, nodeB);
                nodes.add(sumNode);
                Set<GroupNode<T>> edgesOfA = edgesOfNode.get(nodeA);
                Set<GroupNode<T>> edgesOfB = edgesOfNode.get(nodeB);
                HashSet<GroupNode<T>> sumNodeEdges = new HashSet<>();
                sumNodeEdges.addAll(edgesOfA);
                sumNodeEdges.addAll(edgesOfB);
                sumNodeEdges.remove(nodeA);
                sumNodeEdges.remove(nodeB);
                edgesOfNode.put(sumNode, sumNodeEdges);
                for (GroupNode<T> connOfA : edgesOfA) {
                    if (nodeB.equals(connOfA)) {
                        continue;
                    }
                    Pair<GroupNode<T>, GroupNode<T>> edgeAtoX = nodeA.compareTo(connOfA) < 0 ? new Pair<>(nodeA, connOfA) : new Pair<>(connOfA, nodeA);
                    if (edges.remove(edgeAtoX) == null) { throw new IllegalStateException(); }
                    Pair<GroupNode<T>, GroupNode<T>> edgeStoX = sumNode.compareTo(connOfA) < 0 ? new Pair<>(sumNode, connOfA) : new Pair<>(connOfA, sumNode);
                    edges.put(edgeStoX, edgeStoX);
                    Set<GroupNode<T>> reverseConnOfA = edgesOfNode.get(connOfA);
                    reverseConnOfA.remove(nodeA);
                    reverseConnOfA.add(sumNode);
                }
                for (GroupNode<T> connOfB : edgesOfB) {
                    if (nodeA.equals(connOfB)) {
                        continue;
                    }
                    Pair<GroupNode<T>, GroupNode<T>> edgeBtoX = nodeB.compareTo(connOfB) < 0 ? new Pair<>(nodeB, connOfB) : new Pair<>(connOfB, nodeB);
                    if (edges.remove(edgeBtoX) == null) { throw new IllegalStateException(); }
                    Pair<GroupNode<T>, GroupNode<T>> edgeStoX = sumNode.compareTo(connOfB) < 0 ? new Pair<>(sumNode, connOfB) : new Pair<>(connOfB, sumNode);
                    edges.put(edgeStoX, edgeStoX);
                    Set<GroupNode<T>> reverseConnOfB = edgesOfNode.get(connOfB);
                    reverseConnOfB.remove(nodeB);
                    reverseConnOfB.add(sumNode);
                }
                // remove nodes and edge
                if (!nodes.remove(nodeA)) { throw new IllegalStateException(); }
                if (edgesOfNode.remove(nodeA) == null) { throw new IllegalStateException(); }
                if (!nodes.remove(nodeB)) { throw new IllegalStateException(); }
                if (edgesOfNode.remove(nodeB) == null) { throw new IllegalStateException(); }
                if (edges.remove(edge) == null) { throw new IllegalStateException(); }
            }
            List<GroupNode<T>> nodeList = List.copyOf(nodes);
            GroupNode<T> componentA = nodeList.get(0);
            GroupNode<T> componentB = nodeList.get(1);
            Set<Pair<T, T>> cut = new HashSet<>();
            for (Pair<T, T> graphEdge : graphEdges) {
                T graphEdgeNodeA = graphEdge.getValue0();
                T graphEdgeNodeB = graphEdge.getValue1();
                if (componentA.nodes.contains(graphEdgeNodeA) && componentB.nodes.contains(graphEdgeNodeB) ||
                        componentB.nodes.contains(graphEdgeNodeA) && componentA.nodes.contains(graphEdgeNodeB)) {
                    cut.add(graphEdge);
                }
            }
            return new MinimumCutResult<>(cut, componentA.nodes, componentB.nodes);
        }
    }

    private ComponentGraph graph;

    private void parse() {
        graph = new ComponentGraph();
        stream().forEach(line -> {
            String[] nodeEdges = line.split(": ");
            String nodeName = nodeEdges[0];
            // graph.addNode(ComponentNode.getNode(nodeName)); // will be auto added when adding connections
            ComponentNode fromNode = ComponentNode.getNode(nodeName);
            graph.addNode(fromNode);
            String edgeNodesString = nodeEdges[1];
            Arrays.stream(edgeNodesString.split(" ")).forEach(connName -> {
                ComponentNode connNode = ComponentNode.getNode(connName);
                graph.addNode(connNode);
                if (fromNode.getId() < connNode.getId()) { // add edge only in lower id -> bigger id directions, as this is non-directed graph
                    graph.addEdge(fromNode, connNode);
                } else {
                    graph.addEdge(connNode, fromNode);
                }
            });
        });
    }

    /**
     * Given graph is 3-edge-connected (it remains connected if fewer than 3 edges are removed). See https://en.wikipedia.org/wiki/K-edge-connected_graph
     * To find 3 edges to remove we solve a "minimum cut" problem (see https://en.wikipedia.org/wiki/Minimum_cut),
     * of "no terminal nodes" variation. We partition the graph into 2 disjoint sets that have minimal number of edges between them.
     */
    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
        // System.out.println(graph.toStringGrahpviz());
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        // if (true) return null;
        KargerMinimumCut<ComponentNode> minimumCut = new KargerMinimumCut<>(graph);
        int bestCut = Integer.MAX_VALUE, i = 0;
        int componentSizesMul = 0;
        List<Integer> iters = new ArrayList<>();
        while (true) {
            KargerMinimumCut.MinimumCutResult<ComponentNode> cutResult = minimumCut.findMinimumCut();
            if (bestCut > cutResult.edges.size()) {
                bestCut = cutResult.edges.size();
                System.out.printf("%d: new best cut, %d edges%n", i, bestCut);
                System.out.printf("%d: %d - %s%n", i, cutResult.edges.size(), String.join(", ", cutResult.edges.stream().map(edge -> "[%s,%s]".formatted(edge.getValue0().name, edge.getValue1().name)).toList()));
            }
            if (bestCut == 3) {
                componentSizesMul = cutResult.componentA.size() * cutResult.componentB.size();
                iters.add(i);
                System.out.println(String.join(", ", iters.stream().map(Object::toString).toList()));
                // bestCut = Integer.MAX_VALUE;
                break;
            }
            minimumCut.reset();
            i++;
            // break;
        }
        long result = componentSizesMul;
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        long result = 0;
        return null;
    }

    public static class Day25Test {
        @Test
        void solvePart1_sample() {
            var day = new Day25("_sample");
            day.parsePart1();
            assertEquals(54L, day.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day25("");
            day.parsePart1();
            assertEquals(558376L, day.solvePart1());
        }

        @Test
        void solvePart2_sample() {
            var day = new Day25("_sample");
            day.parsePart2();
            assertEquals(null, day.solvePart2()); // 25 part 2 puzzle was to just solve all the previous puzzles :)
        }

        @Test
        void solvePart2_main() {
            var day = new Day25("");
            day.parsePart2();
            assertEquals(null, day.solvePart2()); // 25 part 2 puzzle was to just solve all the previous puzzles :)
        }
    }
}
/*


Advent of Code

    [About][Events][Shop][Settings][Log Out]

Jakub-KK 50*
   sub y{2023}

    [Calendar][AoC++][Sponsors][Leaderboard][Stats]

Our sponsors help make Advent of Code possible:
Cerbos - Easily implement and manage fine-grained access control in your app
--- Day 25: Snowverload ---

Still somehow without snow, you go to the last place you haven't checked: the center of Snow Island, directly below the waterfall.

Here, someone has clearly been trying to fix the problem. Scattered everywhere are hundreds of weather machines, almanacs, communication modules, hoof prints, machine parts, mirrors, lenses, and so on.

Somehow, everything has been wired together into a massive snow-producing apparatus, but nothing seems to be running. You check a tiny screen on one of the communication modules: Error 2023. It doesn't say what Error 2023 means, but it does have the phone number for a support line printed on it.

"Hi, you've reached Weather Machines And So On, Inc. How can I help you?" You explain the situation.

"Error 2023, you say? Why, that's a power overload error, of course! It means you have too many components plugged in. Try unplugging some components and--" You explain that there are hundreds of components here and you're in a bit of a hurry.

"Well, let's see how bad it is; do you see a big red reset button somewhere? It should be on its own module. If you push it, it probably won't fix anything, but it'll report how overloaded things are." After a minute or two, you find the reset button; it's so big that it takes two hands just to get enough leverage to push it. Its screen then displays:

SYSTEM OVERLOAD!

Connected components would require
power equal to at least 100 stars!

"Wait, how many components did you say are plugged in? With that much equipment, you could produce snow for an entire--" You disconnect the call.

You have nowhere near that many stars - you need to find a way to disconnect at least half of the equipment here, but it's already Christmas! You only have time to disconnect three wires.

Fortunately, someone left a wiring diagram (your puzzle input) that shows how the components are connected. For example:

jqt: rhn xhk nvd
rsh: frs pzl lsr
xhk: hfx
cmg: qnr nvd lhk bvb
rhn: xhk bvb hfx
bvb: xhk hfx
pzl: lsr hfx nvd
qnr: nvd
ntq: jqt hfx bvb xhk
nvd: lhk
lsr: lhk
rzs: qnr cmg lsr rsh
frs: qnr lhk lsr

Each line shows the name of a component, a colon, and then a list of other components to which that component is connected. Connections aren't directional; abc: xyz and xyz: abc both represent the same configuration. Each connection between two components is represented only once, so some components might only ever appear on the left or right side of a colon.

In this example, if you disconnect the wire between hfx/pzl, the wire between bvb/cmg, and the wire between nvd/jqt, you will divide the components into two separate, disconnected groups:

    9 components: cmg, frs, lhk, lsr, nvd, pzl, qnr, rsh, and rzs.
    6 components: bvb, hfx, jqt, ntq, rhn, and xhk.

Multiplying the sizes of these groups together produces 54.

Find the three wires you need to disconnect in order to divide the components into two separate groups. What do you get if you multiply the sizes of these two groups together?

Your puzzle answer was 558376.

--- Part Two ---

You climb over weather machines, under giant springs, and narrowly avoid a pile of pipes as you find and disconnect the three wires.

A moment after you disconnect the last wire, the big red reset button module makes a small ding noise:

System overload resolved!
Power required is now 50 stars.

Out of the corner of your eye, you notice goggles and a loose-fitting hard hat peeking at you from behind an ultra crucible. You think you see a faint glow, but before you can investigate, you hear another small ding:

Power required is now 49 stars.

Please supply the necessary stars and
push the button to restart the system.

If you like, you can Push The Big Red Button Again.

Both parts of this puzzle are complete! They provide two gold stars: **

At this point, all that is left is for you to admire your Advent calendar.

 */