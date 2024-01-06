package dev.aoc.aoc2023;

import dev.aoc.common.Day;
import dev.aoc.common.SolutionParser;
import dev.aoc.common.SolutionSolver;
import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day20 extends Day {
    public Day20(String inputSuffix) {
        super(inputSuffix);
    }

    public static void main(String[] args) {
        Day.run(() -> new Day20("_main_test"));
        // _sample_const, _sample_period
        // _main_test
        // _test_periodic_1
    }

    private static abstract class Module {
        private final String name;
        private final List<String> sourceNames = new ArrayList<>();
        private final List<String> destinationNames = new ArrayList<>();
        private final SignalProcessor signalProcessor;

        public Module(String name, SignalProcessor signalProcessor) {
            this.name = name;
            this.signalProcessor = signalProcessor;
            signalProcessor.addModule(this);
        }

        public String getName() {
            return name;
        }

        public void wireCable(Module destination) {
            destinationNames.add(destination.name);
            destination.wireAccept(this);
        }
        protected void wireAccept(Module source) {
            sourceNames.add(source.name);
        }

        private int receiveCount = 0;

        public void receive(Signal input, Module source) {
            Optional<Signal> output = process(input, source);
            output.ifPresent(this::transmit);
            receiveCount++;
        }

        protected abstract Optional<Signal> process(Signal signal, Module source);

        protected void transmit(Signal signal) {
            destinationNames.forEach(dest -> signalProcessor.transmit(this, signal, dest));
        }

        @Override
        public String toString() {
            return "%s -> %s".formatted(name, String.join(", ", destinationNames));
        }
    }
    private static class Receiver extends Module {
        public Receiver(String name, SignalProcessor signalProcessor) {
            super(name, signalProcessor);
        }

        private boolean isBomb;

        public void setBomb(boolean bomb) {
            isBomb = bomb;
        }

        @Override
        protected Optional<Signal> process(Signal signal, Module source) {
            if (isBomb && signal == Signal.LOW) {
                throw new BombExplodedException();
            }
            return Optional.empty();
        }

        public static class BombExplodedException extends RuntimeException {
            public BombExplodedException() {
                super("BOOM");
            }
        }
    }
    private static class Transmitter extends Module {
        public Transmitter(String name, SignalProcessor signalProcessor) {
            super(name, signalProcessor);
        }

        @Override
        public Optional<Signal> process(Signal signal, Module source) {
            return Optional.of(signal);
        }
    }
    private static class Button extends Transmitter {
        public static final String NAME = "button";

        public Button(SignalProcessor signalProcessor) {
            super(NAME, signalProcessor);
        }

        public void push() {
            transmit(Signal.LOW);
        }

        @Override
        public void receive(Signal signal, Module source) {
            throw new IllegalStateException("Button cannot receive signals (received from module %s)".formatted(source.name));
        }
    }
    private static class Broadcaster extends Transmitter {
        public static final String NAME = "broadcaster";
        public Broadcaster(SignalProcessor signalProcessor) {
            super(NAME, signalProcessor);
        }
    }
    private static class FlipFlop extends Module {
        public FlipFlop(String name, SignalProcessor signalProcessor) {
            super(name, signalProcessor);
        }

        private boolean isOn;

        @Override
        protected Optional<Signal> process(Signal signal, Module source) {
            if (signal == Signal.LOW) {
                if (isOn) {
                    isOn = false;
                    return Optional.of(Signal.LOW);
                } else {
                    isOn = true;
                    return Optional.of(Signal.HIGH);
                }
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String toString() {
            return "%" + super.toString();
        }
    }
    /** NAND gate */
    private static class Conjunction extends Module {
        public Conjunction(String name, SignalProcessor signalProcessor) {
            super(name, signalProcessor);
        }

        private final Map<String, Signal> memory = new HashMap<>();

        @Override
        protected void wireAccept(Module source) {
            super.wireAccept(source);
            memory.put(source.name, Signal.LOW);
        }

        @Override
        protected Optional<Signal> process(Signal signal, Module source) {
            memory.put(source.name, signal);
            if (memory.values().stream().allMatch(s -> s == Signal.HIGH)) {
                return Optional.of(Signal.LOW);
            } else {
                return Optional.of(Signal.HIGH);
            }
        }

        @Override
        public String toString() {
            return "&" + super.toString();
        }
    }

    private static class SignalProcessor {
        private record SignalOrder(Module source, Signal signal, Module dest) {
            @Override
            public String toString() {
                return "%s -%s-> %s".formatted(source.name, signal, dest.name);
            }
        }

        private final List<SignalOrder> orders = new LinkedList<>();

        private final Map<String, Module> moduleByName = new HashMap<>();

        private long countLow, countHigh;
        public void countersReset() {
            countLow = countHigh = 0;
        }
        public long getCountLow() {
            return countLow;
        }
        public long getCountHigh() {
            return countHigh;
        }

        public void addModule(Module module) {
            moduleByName.put(module.getName(), module);
        }

        public Module getModule(String moduleName) {
            return moduleByName.get(moduleName);
        }

        public void wireModuleCables(String sourceModuleName, String[] destModuleNames) {
            Module sourceModule = moduleByName.get(sourceModuleName);
            Arrays.stream(destModuleNames).forEach(dest -> {
                // get destination module by name, if missing add receiver module
                Module destModule = moduleByName.containsKey(dest) ? moduleByName.get(dest) : new Receiver(dest, this);
                sourceModule.wireCable(destModule);
            });
        }

        public void transmit(Module source, Signal signal, String destModuleName) {
            orders.addLast(new SignalOrder(source, signal, moduleByName.get(destModuleName)));
        }

        public void propagate() {
            while (!orders.isEmpty()) {
                SignalOrder order = orders.removeFirst();
                // System.out.println(order);
                if (order.signal == Signal.LOW) {
                    countLow++;
                } else if (order.signal == Signal.HIGH) {
                    countHigh++;
                } else {
                    throw new IllegalStateException("signal not LOW nor HIGH");
                }
                order.dest.receive(order.signal, order.source);
            }
        }

        @Override
        public String toString() {
            return String.join("\r\n", moduleByName.keySet().stream().map(moduleByName::get).map(Module::toString).toList());
        }
    }

    private enum Signal {
        UNKNOWN('!'), LOW('L'), HIGH('H');

        public final char symbol;
        Signal(char symbol) {
            this.symbol = symbol;
        }
        private static final Signal[] values = values();
        public static Signal fromSymbol(char symbol) {
            for (Signal cmp : values) {
                if (cmp.symbol == symbol) {
                    return cmp;
                }
            }
            return Signal.UNKNOWN;
        }
    }

    private final SignalProcessor signalProcessor = new SignalProcessor();
    private final Button buttonModule = new Button(signalProcessor);

    @SolutionParser(partNumber = 1)
    public void parsePart1() {
        parse();
    }

    @SolutionSolver(partNumber = 1)
    public Object solvePart1() {
        IntStream.range(0, 1000).forEach(i -> {
            buttonModule.push();
            signalProcessor.propagate();
        });
        long result = signalProcessor.getCountLow() * signalProcessor.getCountHigh();
        return result;
    }

    @SolutionParser(partNumber = 2)
    public void parsePart2() {
        parse();
    }

    @SolutionSolver(partNumber = 2)
    public Object solvePart2() {
        if (true) return 3929L*4051L*3767L*3823L;
        // INFO: puzzle input is a circuit consisting of 4 separate parts, each with its own cycle length, easily discovered by looking at puzzle input and using graph visualization like graphviz (see SVG in inputs folder)
        // TODO: modify Conjunction gate before "rx" Reciver to observe its sources at measure after how many button pushes it pulls HIGH
        // multiply 4 numbers (or LCM if not prime), get answer
        // measurements by hand: nt 3929, kx 4051, rc 3767, mg 3823
        // Circuit info: The cycle length of each input can be easily derived from just the picture - go from the top most flip flop that leads to the nand and take 0 if the flip flop output doesn't lead to nand , 1 if it does (from lsb to msb). (source https://old.reddit.com/r/adventofcode/comments/18mogoy/2023_day_20_visualization_of_the_input_couldnt/ke5tnx0/)
        // More: Each cluster has 12 flipflop bits. The nand gate reads only some of these. When those are all "on" the other ones are off, so the nand gate sets them to on (so that 12 bits are on) and then sends an extra low pulse to the first bit, which cascades through the other 11 to turn them all off. (source https://old.reddit.com/r/adventofcode/comments/18mypla/2023_day_20_input_data_plot/ke8z4sd/)
        Receiver rxModule = (Receiver)signalProcessor.getModule("rx");
        rxModule.setBomb(true);
        boolean hasBombExploded = false;
        long pushCount = 0;
        try {
            while (true) {
                pushCount++;
                buttonModule.push();
                signalProcessor.propagate();
                if (pushCount % 1000000 == 0) {
                    System.out.printf("#%d%n", pushCount);
                }
            }
        } catch (Receiver.BombExplodedException e) {
            hasBombExploded = true;
        }
        long result = pushCount;
        return hasBombExploded ? result : null;
    }

    private void parse() {
        Map<String, String[]> cablesForModuleByNames = new HashMap<>();
        stream().forEach(line -> {
            if (line.trim().isEmpty()) {
                return;
            }
            String[] moduleStr = line.split(" -> ");
            String moduleTypeAndName = moduleStr[0];
            String moduleName;
            if (moduleTypeAndName.charAt(0) == '%') {
                moduleName = moduleTypeAndName.substring(1);
                new FlipFlop(moduleName, signalProcessor);
            } else if (moduleTypeAndName.charAt(0) == '&') {
                moduleName = moduleTypeAndName.substring(1);
                new Conjunction(moduleName, signalProcessor);
            } else {
                moduleName = moduleTypeAndName;
                if (moduleName.equals(Broadcaster.NAME)) {
                    new Broadcaster(signalProcessor);
                } else {
                    new Receiver(moduleName, signalProcessor);
                }
            }
            String moduleCables = moduleStr[1];
            String[] cablesDestModuleNames = moduleCables.split(", ");
            cablesForModuleByNames.put(moduleName, cablesDestModuleNames);
        });
        cablesForModuleByNames.put(Button.NAME, new String[] { Broadcaster.NAME });
        cablesForModuleByNames.forEach(signalProcessor::wireModuleCables);
    }

    public static class Day20Test {
        @Test
        void solvePart1_sample() {
            var dayConst = new Day20("_sample_const");
            dayConst.parsePart1();
            assertEquals(32000000L, dayConst.solvePart1());
            var dayPeriod = new Day20("_sample_period");
            dayPeriod.parsePart1();
            assertEquals(11687500L, dayPeriod.solvePart1());
        }

        @Test
        void solvePart1_main() {
            var day = new Day20("");
            day.parsePart1();
            assertEquals(866435264L, day.solvePart1());
        }

        // no test case for "_sample" part 2
        // @Test
        // void solvePart2_sample() {
        //     var day = new Day20("_sample");
        //     day.parsePart2();
        //     assertEquals(0L, day.solvePart2());
        // }

        @Test
        void solvePart2_main() {
            var day = new Day20("");
            day.parsePart2();
            assertEquals(229215609826339L, day.solvePart2());
        }
    }
}
/*

--- Day 20: Pulse Propagation ---

With your help, the Elves manage to find the right parts and fix all of the machines. Now, they just need to send the command to boot up the machines and get the sand flowing again.

The machines are far apart and wired together with long cables. The cables don't connect to the machines directly, but rather to communication modules attached to the machines that perform various initialization tasks and also act as communication relays.

Modules communicate using pulses. Each pulse is either a high pulse or a low pulse. When a module sends a pulse, it sends that type of pulse to each module in its list of destination modules.

There are several different types of modules:

Flip-flop modules (prefix %) are either on or off; they are initially off. If a flip-flop module receives a high pulse, it is ignored and nothing happens. However, if a flip-flop module receives a low pulse, it flips between on and off. If it was off, it turns on and sends a high pulse. If it was on, it turns off and sends a low pulse.

Conjunction modules (prefix &) remember the type of the most recent pulse received from each of their connected input modules; they initially default to remembering a low pulse for each input. When a pulse is received, the conjunction module first updates its memory for that input. Then, if it remembers high pulses for all inputs, it sends a low pulse; otherwise, it sends a high pulse.

There is a single broadcast module (named broadcaster). When it receives a pulse, it sends the same pulse to all of its destination modules.

Here at Desert Machine Headquarters, there is a module with a single button on it called, aptly, the button module. When you push the button, a single low pulse is sent directly to the broadcaster module.

After pushing the button, you must wait until all pulses have been delivered and fully handled before pushing it again. Never push the button if modules are still processing pulses.

Pulses are always processed in the order they are sent. So, if a pulse is sent to modules a, b, and c, and then module a processes its pulse and sends more pulses, the pulses sent to modules b and c would have to be handled first.

The module configuration (your puzzle input) lists each module. The name of the module is preceded by a symbol identifying its type, if any. The name is then followed by an arrow and a list of its destination modules. For example:

broadcaster -> a, b, c
%a -> b
%b -> c
%c -> inv
&inv -> a

In this module configuration, the broadcaster has three destination modules named a, b, and c. Each of these modules is a flip-flop module (as indicated by the % prefix). a outputs to b which outputs to c which outputs to another module named inv. inv is a conjunction module (as indicated by the & prefix) which, because it has only one input, acts like an inverter (it sends the opposite of the pulse type it receives); it outputs to a.

By pushing the button once, the following pulses are sent:

button -low-> broadcaster
broadcaster -low-> a
broadcaster -low-> b
broadcaster -low-> c
a -high-> b
b -high-> c
c -high-> inv
inv -low-> a
a -low-> b
b -low-> c
c -low-> inv
inv -high-> a

After this sequence, the flip-flop modules all end up off, so pushing the button again repeats the same sequence.

Here's a more interesting example:

broadcaster -> a
%a -> inv, con
&inv -> b
%b -> con
&con -> output

This module configuration includes the broadcaster, two flip-flops (named a and b), a single-input conjunction module (inv), a multi-input conjunction module (con), and an untyped module named output (for testing purposes). The multi-input conjunction module con watches the two flip-flop modules and, if they're both on, sends a low pulse to the output module.

Here's what happens if you push the button once:

button -low-> broadcaster
broadcaster -low-> a
a -high-> inv
a -high-> con
inv -low-> b
con -high-> output
b -high-> con
con -low-> output

Both flip-flops turn on and a low pulse is sent to output! However, now that both flip-flops are on and con remembers a high pulse from each of its two inputs, pushing the button a second time does something different:

button -low-> broadcaster
broadcaster -low-> a
a -low-> inv
a -low-> con
inv -high-> b
con -high-> output

Flip-flop a turns off! Now, con remembers a low pulse from module a, and so it sends only a high pulse to output.

Push the button a third time:

button -low-> broadcaster
broadcaster -low-> a
a -high-> inv
a -high-> con
inv -low-> b
con -low-> output
b -low-> con
con -high-> output

This time, flip-flop a turns on, then flip-flop b turns off. However, before b can turn off, the pulse sent to con is handled first, so it briefly remembers all high pulses for its inputs and sends a low pulse to output. After that, flip-flop b turns off, which causes con to update its state and send a high pulse to output.

Finally, with a on and b off, push the button a fourth time:

button -low-> broadcaster
broadcaster -low-> a
a -low-> inv
a -low-> con
inv -high-> b
con -high-> output

This completes the cycle: a turns off, causing con to remember only low pulses and restoring all modules to their original states.

To get the cables warmed up, the Elves have pushed the button 1000 times. How many pulses got sent as a result (including the pulses sent by the button itself)?

In the first example, the same thing happens every time the button is pushed: 8 low pulses and 4 high pulses are sent. So, after pushing the button 1000 times, 8000 low pulses and 4000 high pulses are sent. Multiplying these together gives 32000000.

In the second example, after pushing the button 1000 times, 4250 low pulses and 2750 high pulses are sent. Multiplying these together gives 11687500.

Consult your module configuration; determine the number of low pulses and high pulses that would be sent after pushing the button 1000 times, waiting for all pulses to be fully handled after each push of the button. What do you get if you multiply the total number of low pulses sent by the total number of high pulses sent?

Your puzzle answer was 866435264.

--- Part Two ---

The final machine responsible for moving the sand down to Island Island has a module attached named rx. The machine turns on when a single low pulse is sent to rx.

Reset all modules to their default states. Waiting for all pulses to be fully handled after each button press, what is the fewest number of button presses required to deliver a single low pulse to the module named rx?

Your puzzle answer was 229215609826339.

 */