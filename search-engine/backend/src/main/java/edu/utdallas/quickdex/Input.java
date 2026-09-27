package edu.utdallas.quickdex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads lines from the Input Medium, cleans them, and stores them in {@link LineStorage}.
 *
 * <p><b>Secret:</b> where input comes from and how a raw line is turned into words.
 *
 * <p>A line's words are separated by any whitespace; extra whitespace and blank lines are
 * ignored, and every word is kept exactly as entered. If the first or last word of a line
 * is an {@code http://} or {@code https://} address, it becomes the line's URL instead of a
 * word.
 */
public final class Input {

    /** Returned by {@link #setInput()} when the Input Medium has no more lines. */
    public static final int END_OF_INPUT = -1;

    /** Returned by {@link #setInput()} when the line read was blank and was skipped. */
    public static final int SKIPPED = -2;

    /** The most lines one input may contain (FR7.0). */
    public static final int MAX_LINES = 10_000;

    /** The largest input, in UTF-8 bytes (FR7.0). */
    public static final int MAX_BYTES = 1_000_000;

    /**
     * The most words one line may contain. A line of n words adds n shifts of n words each,
     * so this bounds the index size and keeps the engine within its memory (NFR10.0).
     */
    public static final int MAX_WORDS_PER_LINE = 50;

    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");
    private static final Pattern LINE_BREAK = Pattern.compile("\\r\\n|\\r|\\n");

    private final InputMedium medium;
    private final LineStorage storage;

    public Input(InputMedium medium, LineStorage storage) {
        this.medium = medium;
        this.storage = storage;
    }

    /**
     * Checks a whole input against the limits before any line is processed (FR7.0).
     *
     * @return a message that explains the problem and how to fix it, or empty if the input
     *         is acceptable
     */
    public static Optional<String> validate(String text) {
        if (text == null || text.isBlank()) {
            return Optional.of("The input is empty. Enter at least one line of text.");
        }
        int bytes = text.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BYTES) {
            return Optional.of(String.format(Locale.ROOT,
                    "The input is %,d bytes, over the 1 MB limit. Remove some text and try again.",
                    bytes));
        }
        String[] lines = splitLines(text);
        if (lines.length > MAX_LINES) {
            return Optional.of(String.format(Locale.ROOT,
                    "The input has %,d lines, over the %,d-line limit. Remove some lines and try again.",
                    lines.length, MAX_LINES));
        }
        for (int i = 0; i < lines.length; i++) {
            int words = words(lines[i]).size();
            if (words > MAX_WORDS_PER_LINE) {
                return Optional.of(String.format(Locale.ROOT,
                        "Line %,d has %,d words, over the %d-word limit per line. Split it into shorter lines.",
                        i + 1, words, MAX_WORDS_PER_LINE));
            }
        }
        if (Arrays.stream(lines).allMatch(line -> words(line).isEmpty())) {
            return Optional.of("The input has no words, only URLs. Add some text to each line.");
        }
        return Optional.empty();
    }

    /** Splits text into lines on any line break. */
    public static String[] splitLines(String text) {
        String[] lines = LINE_BREAK.split(text, -1);
        // A final line break does not start a new line.
        if (lines.length > 1 && lines[lines.length - 1].isEmpty()) {
            return Arrays.copyOf(lines, lines.length - 1);
        }
        return lines;
    }

    /**
     * Reads the next line from the Input Medium and stores it.
     *
     * @return the stored line's number in {@link LineStorage}, {@link #SKIPPED} if the line
     *         was blank, or {@link #END_OF_INPUT} if there are no more lines
     */
    public int setInput() {
        String raw = medium.readLine();
        if (raw == null) {
            return END_OF_INPUT;
        }
        List<String> words = words(raw);
        if (words.isEmpty()) {
            return SKIPPED;
        }
        return storeInputInLineStorage(words, url(tokens(raw)));
    }

    /** Returns the URL of a raw line, by the same rule {@link #setInput()} uses, or {@code null}. */
    public static String urlOf(String raw) {
        return url(tokens(raw));
    }

    /** Splits a raw line on whitespace, dropping empty tokens. */
    private static List<String> tokens(String raw) {
        List<String> tokens = new ArrayList<>(Arrays.asList(WHITESPACE.split(raw.strip())));
        tokens.removeIf(String::isEmpty);
        return tokens;
    }

    /** The line's URL: its first word if that is a URL, else its last word if that is one. */
    private static String url(List<String> tokens) {
        if (tokens.isEmpty()) {
            return null;
        }
        if (isUrl(tokens.get(0))) {
            return tokens.get(0);
        }
        return isUrl(tokens.get(tokens.size() - 1)) ? tokens.get(tokens.size() - 1) : null;
    }

    /** The line's words, without its URL. */
    private static List<String> words(String raw) {
        List<String> tokens = tokens(raw);
        if (!tokens.isEmpty() && isUrl(tokens.get(0))) {
            tokens.remove(0);
        } else if (!tokens.isEmpty() && isUrl(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        return tokens;
    }

    private int storeInputInLineStorage(List<String> words, String url) {
        int line = storage.getLineCount();
        for (int w = 0; w < words.size(); w++) {
            int[] codePoints = words.get(w).codePoints().toArray();
            for (int c = 0; c < codePoints.length; c++) {
                storage.setCharacter(line, w, c, codePoints[c]);
            }
        }
        storage.setUrl(line, url);
        return line;
    }

    private static boolean isUrl(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        return (lower.startsWith("http://") && lower.length() > 7)
                || (lower.startsWith("https://") && lower.length() > 8);
    }
}
