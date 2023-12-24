package dev.aoc.common;

import org.javatuples.Pair;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.io.Files.asCharSource;
import static java.lang.Integer.parseInt;
import static java.nio.charset.Charset.defaultCharset;

public abstract class Day {
    private final int aocYear;
    private final int aocDay;

    private final String inputSuffix;
    private final List<String> inputLines;

    private Day(String inputSuffix, List<String> inputLines) {
        String className = getClass().toString();
        Matcher matcher = Pattern.compile(".+?aoc(\\d+)\\.Day(\\d+).*").matcher(className);
        if (!matcher.matches()) {
            throw new RuntimeException("Could not match year-day to full class name: %s".formatted(className));
        }
        aocYear = parseInt(matcher.group(1));
        aocDay = parseInt(matcher.group(2));
        if (inputSuffix != null && inputLines != null) {
            throw new IllegalArgumentException("give either input suffix or input lines, not both");
        } else if (inputSuffix == null && inputLines == null) {
            throw new IllegalArgumentException("give input suffix or input lines");
        }
        this.inputLines = inputLines;
        this.inputSuffix = inputSuffix;
    }

    protected Day(String inputSuffix) {
        this(inputSuffix, null);
    }

    protected Day(List<String> inputLines) {
        this(null, inputLines);
    }

    protected String getInputSuffix() { return inputSuffix != null ? inputSuffix : ""; }

    public static void run(Supplier<Day> dayFactory) {
        Day instance = dayFactory.get(); // we need instance to get class and methods, will be used later
        var dayClass = instance.getClass();
        var declaredMethods = dayClass.getDeclaredMethods();
        var parsers = Arrays.stream(declaredMethods).filter(m -> m.isAnnotationPresent(SolutionParser.class)).toList();
        var solvers = Arrays.stream(declaredMethods).filter(m -> m.isAnnotationPresent(SolutionSolver.class)).toList();
        var parsersPerPart = parsers.stream().collect(Collectors.groupingBy(m -> m.getDeclaredAnnotation(SolutionParser.class).partNumber()));
        var solversPerPart = solvers.stream().collect(Collectors.groupingBy(m -> m.getDeclaredAnnotation(SolutionSolver.class).partNumber()));
        var parts = new TreeSet<Integer>();
        parts.addAll(parsersPerPart.keySet());
        parts.addAll(solversPerPart.keySet());
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("no annotated methods found");
        }
        instance.showTitleAndPrefetchInput();
        for (int partNumber : parts) {
            var partElementsPerName = getPartElementsPerName(partNumber, parsersPerPart, solversPerPart);
            for (var entry : partElementsPerName.entrySet()) {
                if (instance == null) {
                    instance = dayFactory.get();
                }
                final Day instance_ = instance; // make it final for lambda usage
                runParserSolver(partNumber, entry.getValue().getValue0(), entry.getValue().getValue1(), instance_);
                instance = null; // instance is spent
            }
        }
    }
    private static void runParserSolver(int partNumber, Method parser, Method solver, Day instance) {
        instance.runPart(
                () -> {
                    try {
                        parser.invoke(instance);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                },
                parser.getDeclaredAnnotation(SolutionParser.class).solutionName(),
                () -> {
                    try {
                        return solver.invoke(instance);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                },
                solver.getDeclaredAnnotation(SolutionSolver.class).solutionName(),
                partNumber
        );
    }
    /**
     * Create mapping "solution name" -> parser+solver
     * There can be only one solver for a name. There must be one parser for a name that has solver or there is a default parser.
     * @return mapping "solution name" -> parser+solver
     */
    private static HashMap<String, Pair<Method, Method>> getPartElementsPerName(int partNumber, Map<Integer, List<Method>> parsersPerPart, Map<Integer, List<Method>> solversPerPart) {
        var partParsers = parsersPerPart.get(partNumber);
        var partParsersPerName = partParsers.stream().collect(Collectors.groupingBy(m -> m.getDeclaredAnnotation(SolutionParser.class).solutionName()));
        var partSolvers = solversPerPart.get(partNumber);
        var partSolversPerName = partSolvers.stream().collect(Collectors.groupingBy(m -> m.getDeclaredAnnotation(SolutionSolver.class).solutionName()));
        var partElementsPerName = new HashMap<String, Pair<Method, Method>>();
        for (String solverName : partSolversPerName.keySet()) {
            // get singular solver for current name
            var solversOfName = partSolversPerName.get(solverName);
            if (solversOfName.size() > 1) {
                throw new IllegalArgumentException("part %d solvers name collision for name %s".formatted(partNumber, solverName));
            }
            Method solver = solversOfName.getFirst();
            // get singular parser for current name, if not defined, get singular default parser
            var parsersOfName = partParsersPerName.get(solverName);
            Method parser;
            if (parsersOfName.isEmpty()) {
                var defaultParsers = partParsersPerName.get(DEFAULT_NAME);
                if (defaultParsers.isEmpty()) {
                    throw new IllegalArgumentException("part %d default parser missing for name %s".formatted(partNumber, solverName));
                } else if (defaultParsers.size() > 1) {
                    throw new IllegalArgumentException("part %d too many default parsers (%d) for name %s".formatted(partNumber, defaultParsers.size(), solverName));
                } else {
                    parser = defaultParsers.getFirst();
                }
            } else if (parsersOfName.size() > 1) {
                throw new IllegalArgumentException("part %d parsers name collision for name %s".formatted(partNumber, solverName));
            } else {
                parser = parsersOfName.getFirst();
                partParsersPerName.remove(solverName); // remove used parsers, to account for redundant ones
            }
            partElementsPerName.put(solverName, new Pair<>(parser, solver));
        }
        if (!partParsersPerName.isEmpty()) {
            System.out.printf("*** part %d redundant parsers found: %s%n", partNumber, String.join(", ", partParsersPerName.keySet().stream().toList()));
        }
        return partElementsPerName;
    }

    private void showTitleAndPrefetchInput() {
        String prefetchInput = inputString();
        System.out.printf("### Advent of Code %d day %02d, input \"%s\" size %d%n%n", aocYear, aocDay, inputSuffix, prefetchInput.length());
    }

    public void run() {
        showTitleAndPrefetchInput();
        runPart(this::parsePart1, "", this::solvePart1, "", 1);
        runPart(this::parsePart2, "", this::solvePart2, "", 2);
    }

    private static final String DEFAULT_NAME = "default";

    private void runPart(Runnable parser, String parserName, Supplier<Object> solver, String solverName, int partNumber) {
        parserName = parserName.trim().isEmpty() ? DEFAULT_NAME : parserName;
        System.out.printf("### Part %d, parser \"%s\": parsing...%n", partNumber, parserName);
        Instant parsingStart = Instant.now();
        parser.run();
        Instant parsingFinish = Instant.now();
        System.out.printf("### Part %d, parser \"%s\": parsed [elapsed: %s]%n", partNumber, parserName, Duration.between(parsingStart, parsingFinish).toString());
        solverName = solverName.trim().isEmpty() ? DEFAULT_NAME : solverName;
        System.out.printf("### Part %d, solver \"%s\": solving...%n", partNumber, solverName);
        Instant solvingStart = Instant.now();
        Object partResult = solver.get();
        Instant solvingFinish = Instant.now();
        if (partResult != null) {
            System.out.printf("### Part %d, solver \"%s\": solved [elapsed: %s]: %n%s%n%n", partNumber, solverName, Duration.between(solvingStart, solvingFinish).toString(), partResult);
        } else {
            System.out.printf("### Part %d, solver \"%s\": solver UNFINISHED%n%n", partNumber, solverName);
        }
    }

    protected void parsePart1() {}

    protected Object solvePart1() { return null; }

    protected void parsePart2() {}

    protected Object solvePart2() { return null; }

    public Stream<String> stream() {
        return inputStream().map(String::trim);
        //return Arrays.stream(inputString().replace("\r\n", "\n").split("\n")).map(String::trim);
    }

    // public Stream<String> stream(String delimiter) {
    //     return Pattern.compile(delimiter).splitAsStream(inputString()).map(String::trim);
    // }

    public Stream<String> inputStream() {
        Path filePath = getInputPath();
        ensureFileAvailable(filePath);
        return AoCUtil.readFileAsStreamOfLines(filePath);
    }

    public String inputString() {
        Path filePath = getInputPath();
        ensureFileAvailable(filePath);
        return readFileAsString(filePath.toFile());
    }

    private void ensureFileAvailable(Path filePath) {
        if (!Files.exists(filePath)) {
            if (!inputSuffix.isEmpty()) {
                throw new RuntimeException("Could not find local input problem file %s".formatted(filePath));
            } else {
                downloadInput(filePath);
            }
        }
    }

    private Path getInputPath() {
        String filename = getInputPath(inputSuffix);
        return Path.of("inputs/%d/%s".formatted(aocYear, filename));
    }

    protected String getInputPath(String fileSuffix) {
        return AoCUtil.getInputName(aocYear, aocDay, fileSuffix);
    }

    private void downloadInput(Path filePath) {
        try {
            String cookie = readFileAsString(new File("cookie.txt"));
            URL url = new URL("https://adventofcode.com/%d/day/%d/input".formatted(aocYear, aocDay));
            HttpURLConnection connection = getConnection(url, cookie);
            try {
                try (var is = connection.getInputStream()) {
                    try (var fos = new FileOutputStream(filePath.toFile())) {
                        is.transferTo(fos);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not save input problem", e);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not download input problem", e);
        }
    }

    private HttpURLConnection getConnection(URL url, String cookie) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("Cookie", "session=%s".formatted(cookie));
        } catch (IOException e) {
            throw new RuntimeException("Could not download input problem", e);
        }
        return connection;
    }

    private static String readFileAsString(File file) {
        try {
            return asCharSource(file, defaultCharset()).read();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file %s".formatted(file.getPath()));
        }
    }

    protected void createTestFile(String testSuffix, Consumer<Writer> testGenerator) {
        try {
            try (BufferedWriter testWriter = Files.newBufferedWriter(Path.of("inputs/%d/%s".formatted(aocYear, getInputPath(testSuffix))))) {
                testGenerator.accept(testWriter);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void logMemory() {
        var runtime = Runtime.getRuntime();
        System.out.printf("Runtime mem: max %d, total %d, free %d%n", runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory());
    }

    protected void sleepAtMost(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }
}
