package dev.aoc.common;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public void run() {
        String prefetchInput = inputString();

        System.out.printf("Advent of Code %d day %02d, input size %d%n%n", aocYear, aocDay, prefetchInput.length());
        runPart(this::parsePart1, this::solvePart1, 1);
        runPart(this::parsePart2, this::solvePart2, 2);
    }

    private void runPart(Runnable parser, Supplier<Object> solver, int partNumber) {
        System.out.printf("PART %d PARSING%n", partNumber);
        Instant parsingStart = Instant.now();
        parser.run();
        Instant parsingFinish = Instant.now();
        System.out.printf("PART %d PARSER [elapsed: %s]%n", partNumber, Duration.between(parsingStart, parsingFinish).toString());
        System.out.printf("PART %d SOLVING%n", partNumber);
        Instant solvingStart = Instant.now();
        Object partResult = solver.get();
        Instant solvingFinish = Instant.now();
        if (partResult != null) {
            System.out.printf("PART %d SOLVER [elapsed: %s]: %n%s%n%n", partNumber, Duration.between(solvingStart, solvingFinish).toString(), partResult);
        } else {
            System.out.printf("PART %d SOLVER - UNCOMPLETED%n", partNumber);
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

    protected void logMemory() {
        var runtime = Runtime.getRuntime();
        System.out.printf("Runtime mem: max %d, total %d, free %d%n", runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory());
    }

    protected void sleepAtMost(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }
}
