package edu.utdallas.quickdex;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Calls the other components in order, once per line, and logs each line (FR5.0).
 *
 * <p><b>Secret:</b> the order of the steps. For each line, {@link #run()} (the design's
 * {@code main()}) calls {@link Input#setInput()}, {@link CircularShift#setLines},
 * {@link Alphabetizer#setShifts()}, and {@link Output#getOutput()}, so the index is sorted
 * and shown after every line. {@link #search} then hands a keyword to
 * {@link SearchEngine#setSearchEngine}.
 *
 * <p>The engine depends only on the Java standard library and on the two media, so it runs
 * the same way from the web server, the command line, or a test (NFR3.0).
 */
public final class MasterControl {

    private static final Logger LOG = System.getLogger("quickdex");

    private final Input input;
    private final CircularShift circularShift;
    private final Alphabetizer alphabetizer;
    private final Output output;
    private final SearchEngine searchEngine;
    private final LoggingMedium loggingMedium;

    /** A summary of one run, for the caller and the log. */
    public record RunSummary(int linesIndexed, int linesSkipped, int shifts, long elapsedMillis) {
    }

    public MasterControl(InputMedium inputMedium, OutputMedium outputMedium) {
        this(inputMedium, outputMedium, Alphabetizer.Order.CASE_INSENSITIVE);
    }

    public MasterControl(InputMedium inputMedium, OutputMedium outputMedium, Alphabetizer.Order order) {
        LineStorage lineStorage = new LineStorage();
        this.loggingMedium = new LoggingMedium(outputMedium);
        this.input = new Input(inputMedium, lineStorage);
        this.circularShift = new CircularShift(lineStorage);
        this.alphabetizer = new Alphabetizer(circularShift, order);
        this.output = new Output(alphabetizer, circularShift, loggingMedium);
        this.searchEngine = new SearchEngine(output, loggingMedium);
    }

    /**
     * Builds the index one line at a time, showing the output after every line.
     */
    public RunSummary run() {
        long start = System.nanoTime();
        int indexed = 0;
        int skipped = 0;
        for (int inputLine = 1; ; inputLine++) {
            long lineStart = System.nanoTime();
            int line = input.setInput();
            if (line == Input.END_OF_INPUT) {
                break;
            }
            if (line == Input.SKIPPED) {
                skipped++;
                LOG.log(Level.INFO, "input line {0}: skipped (blank)", inputLine);
                continue;
            }
            circularShift.setLines(line);
            alphabetizer.setShifts();
            output.getOutput();
            indexed++;
            logLine(inputLine, lineStart);
        }
        RunSummary summary = new RunSummary(indexed, skipped, alphabetizer.getShiftCount(),
                (System.nanoTime() - start) / 1_000_000);
        logSummary(summary);
        return summary;
    }

    /** Searches the index and shows the matching pages on the Output Medium. */
    public void search(String keyword) {
        searchEngine.setSearchEngine(keyword);
    }

    /** Suggests up to {@code limit} searches that complete {@code typed}. */
    public List<String> suggest(String typed, int limit) {
        return searchEngine.suggestSearches(typed, limit);
    }

    /** Returns the whole index formatted as a file. */
    public String export(Output.Format format) {
        return output.export(format);
    }

    /** Returns the whole sorted index. */
    public List<KwicEntry> index() {
        return output.getOutput();
    }

    private void logLine(int inputLine, long lineStart) {
        if (!LOG.isLoggable(Level.INFO)) {
            return;
        }
        List<PlacedEntry> inserted = loggingMedium.lastInserted;
        String shifts = inserted.stream()
                .map(PlacedEntry::entry)
                .sorted((a, b) -> Integer.compare(a.id(), b.id()))
                .map(e -> "\"" + e.text() + "\"")
                .collect(Collectors.joining(", "));
        LOG.log(Level.INFO, "input line {0} (line {1}): {2} shifts [{3}]; index size {4}; {5} ms",
                inputLine, inserted.get(0).entry().lineNumber(), inserted.size(), shifts,
                loggingMedium.lastSize, String.format(Locale.ROOT, "%.3f", (System.nanoTime() - lineStart) / 1e6));
    }

    private void logSummary(RunSummary summary) {
        if (!LOG.isLoggable(Level.INFO)) {
            return;
        }
        StringBuilder index = new StringBuilder();
        List<KwicEntry> entries = output.getOutput();
        for (int i = 0; i < entries.size(); i++) {
            KwicEntry e = entries.get(i);
            index.append(String.format(Locale.ROOT, "%n  %d. %s | %s | line %d%s",
                    i + 1, e.keyword(), e.context(), e.lineNumber(), e.url() == null ? "" : " | " + e.url()));
        }
        LOG.log(Level.INFO, "done: {0} lines indexed, {1} skipped, {2} shifts, {3} ms; final index:{4}",
                summary.linesIndexed(), summary.linesSkipped(), summary.shifts(), summary.elapsedMillis(), index);
    }

    /** Passes everything to the real Output Medium, remembering the last update for the log. */
    private static final class LoggingMedium implements OutputMedium {
        private final OutputMedium target;
        private List<PlacedEntry> lastInserted = List.of();
        private int lastSize;

        LoggingMedium(OutputMedium target) {
            this.target = target;
        }

        @Override
        public void displayOutput(int lineNumber, List<PlacedEntry> inserted,
                List<KwicEntry> generated, int indexSize) {
            lastInserted = inserted;
            lastSize = indexSize;
            target.displayOutput(lineNumber, inserted, generated, indexSize);
        }

        @Override
        public void displayResults(String query, List<SearchResult> results) {
            target.displayResults(query, results);
        }
    }

    /**
     * Runs Quickdex from the command line, without the web page.
     *
     * <pre>
     * java -cp quickdex-engine.jar edu.utdallas.quickdex.MasterControl [file] [--search TERM] [--csv]
     * </pre>
     *
     * Reads the input from {@code file}, or from standard input, and prints the sorted
     * index (or the search results) to standard output.
     */
    public static void main(String[] args) throws IOException {
        String file = null;
        String term = null;
        boolean csv = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--search" -> term = i + 1 < args.length ? args[++i] : "";
                case "--csv" -> csv = true;
                case "--help", "-h" -> {
                    System.out.println("usage: MasterControl [file] [--search TERM] [--csv]");
                    return;
                }
                default -> file = args[i];
            }
        }
        String text = file == null
                ? new String(System.in.readAllBytes(), StandardCharsets.UTF_8)
                : Files.readString(Path.of(file), StandardCharsets.UTF_8);
        Optional<String> problem = Input.validate(text);
        if (problem.isPresent()) {
            System.err.println(problem.get());
            System.exit(1);
        }

        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        OutputMedium console = new OutputMedium() {
            @Override
            public void displayOutput(int lineNumber, List<PlacedEntry> inserted,
                    List<KwicEntry> generated, int indexSize) {
                // The console shows only the final index.
            }

            @Override
            public void displayResults(String query, List<SearchResult> results) {
                out.printf("%d results for \"%s\"%n", results.size(), query);
                for (SearchResult r : results) {
                    out.println(r.text() + (r.url() == null ? "" : "\t" + r.url()));
                    r.matches().forEach(m -> out.println("    " + m.entry().keyword() + " | " + m.entry().context()));
                }
            }
        };
        MasterControl control = new MasterControl(new TextInputMedium(text), console);
        control.run();
        if (term != null) {
            control.search(term);
        } else {
            out.print(control.export(csv ? Output.Format.CSV : Output.Format.TEXT));
        }
    }
}
