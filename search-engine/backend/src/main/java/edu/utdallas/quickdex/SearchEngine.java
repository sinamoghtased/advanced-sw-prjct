package edu.utdallas.quickdex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds the pages (input lines) that match the user's search words, and suggests searches
 * while the user types, using the KWIC index.
 *
 * <p><b>Secret:</b> how matches are found and ranked. {@link #setSearchEngine} receives the
 * search words and gets the KWIC index from {@link Output}; the private
 * {@link #searchKeywordMatches} and {@link #displayResults} do the rest.
 *
 * <p>Every word of a line is the keyword of one index entry, so a search needs only the
 * index: a line matches when each search word starts one of the line's keywords, ignoring
 * case (FR9.0). Common words such as "the" and "who" are ignored unless the search has
 * nothing else. If no line has every word, the lines with the most of them are shown
 * instead, with the missing words listed. Lines are ranked by these rules, in order:
 * <ol>
 *   <li>lines whose title (the text before the first colon, or the whole line) is exactly the
 *       search words;</li>
 *   <li>lines where the search words appear together and in order (the start of one shift);</li>
 *   <li>lines with more search words that are whole words, such as "sort" in "Merge sort"
 *       rather than only as the start of "sorted";</li>
 *   <li>lines that start with the search words;</li>
 *   <li>alphabetical order of the line's text.</li>
 * </ol>
 */
public final class SearchEngine {

    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

    /** Words too common to narrow a search. */
    static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for", "from",
            "how", "i", "in", "is", "it", "me", "my", "of", "on", "or", "that", "the", "this", "to",
            "was", "what", "when", "where", "which", "who", "why", "with", "you", "your");

    private final Output output;
    private final OutputMedium medium;

    public SearchEngine(Output output, OutputMedium medium) {
        this.output = output;
        this.medium = medium;
    }

    /**
     * Searches the index and shows the matching pages on the Output Medium. A search with no
     * words finds nothing.
     */
    public void setSearchEngine(String query) {
        List<KwicEntry> index = output.getOutput();
        displayResults(query, searchKeywordMatches(query, index));
    }

    /**
     * Suggests up to {@code limit} searches that complete what the user has typed so far
     * (FR12.0). Each suggestion is the start of a circular shift in the index: the typed words
     * with the last one completed, alone and extended to the next meaningful word, skipping up
     * to two common words such as "of" or "the" ("man" suggests "man in the middle"). Only
     * words that follow each other in the original line are used, never words that wrap
     * around from its end. Suggestions made from more shifts come first, then alphabetically;
     * a shift that starts its line (usually a page title) counts three times.
     */
    public List<String> suggestSearches(String typed, int limit) {
        List<String> words = words(typed);
        if (words.isEmpty() || limit <= 0) {
            return List.of();
        }
        boolean lastComplete = Character.isWhitespace(typed.codePointBefore(typed.length()));
        String first = words.get(0);
        String typedText = String.join(" ", words);

        List<KwicEntry> index = output.getOutput();
        // A line's shifts have consecutive ids, starting with its original word order.
        Map<Integer, Integer> firstId = new HashMap<>();
        for (KwicEntry entry : index) {
            firstId.merge(entry.lineNumber(), entry.id(), Math::min);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (KwicEntry entry : index) {
            String keyword = entry.keyword().toLowerCase(Locale.ROOT);
            if (!keyword.startsWith(first)) {
                continue;
            }
            List<String> shift = words(entry.text()).stream().map(SearchEngine::stripPunctuation).toList();
            if (!startsWithTyped(shift, words, lastComplete)) {
                continue;
            }
            int offset = entry.id() - firstId.get(entry.lineNumber());
            int unwrapped = shift.size() - offset;
            int typedCount = words.size();
            if (unwrapped < typedCount || shift.subList(0, typedCount).contains("")) {
                continue;
            }
            int weight = offset == 0 ? 3 : 1;
            // The typed words, with the last one completed.
            String completed = String.join(" ", shift.subList(0, typedCount));
            if (!completed.equals(typedText)) {
                counts.merge(completed, weight, Integer::sum);
            }
            // Extended to the next meaningful word.
            for (int n = typedCount + 1; n <= Math.min(unwrapped, typedCount + 3); n++) {
                List<String> phrase = shift.subList(0, n);
                if (phrase.get(n - 1).isEmpty()) {
                    break;
                }
                if (!STOP_WORDS.contains(phrase.get(n - 1))) {
                    counts.merge(String.join(" ", phrase), weight, Integer::sum);
                    break;
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static boolean startsWithTyped(List<String> shift, List<String> typed, boolean lastComplete) {
        if (shift.size() < typed.size()) {
            return false;
        }
        for (int i = 0; i < typed.size(); i++) {
            String word = stripPunctuation(typed.get(i));
            boolean last = i == typed.size() - 1;
            boolean ok = last && !lastComplete ? shift.get(i).startsWith(word) : shift.get(i).equals(word);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private List<SearchResult> searchKeywordMatches(String query, List<KwicEntry> index) {
        List<String> terms = searchTerms(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        // Pass 1: find the entries whose keyword starts with a search word, grouped by line.
        Map<Integer, LineMatch> lines = new HashMap<>();
        for (int position = 0; position < index.size(); position++) {
            KwicEntry entry = index.get(position);
            String keyword = entry.keyword().toLowerCase(Locale.ROOT);
            boolean matched = false;
            for (int t = 0; t < terms.size(); t++) {
                if (keyword.startsWith(terms.get(t))) {
                    LineMatch line = lines.computeIfAbsent(entry.lineNumber(), n -> new LineMatch(terms.size()));
                    line.found[t] = true;
                    line.whole[t] |= stripPunctuation(keyword).equals(stripPunctuation(terms.get(t)));
                    matched = true;
                }
            }
            if (matched) {
                LineMatch line = lines.get(entry.lineNumber());
                line.matches.add(new PlacedEntry(position, entry));
                if (!line.phrase && startsWithPhrase(entry, terms)) {
                    line.phrase = true;
                }
            }
        }
        // Keep the lines with every word; if there are none, the lines with the most words.
        int best = lines.values().stream().mapToInt(LineMatch::foundWords).max().orElse(0);
        lines.values().removeIf(line -> line.foundWords() < best);
        if (lines.isEmpty()) {
            return List.of();
        }

        // Pass 2: recover each matching line's original text, its first-generated shift.
        for (KwicEntry entry : index) {
            LineMatch line = lines.get(entry.lineNumber());
            if (line != null && (line.original == null || entry.id() < line.original.id())) {
                line.original = entry;
            }
        }

        for (LineMatch line : lines.values()) {
            line.exactTitle = title(line.original.text()).equals(terms.stream().map(SearchEngine::stripPunctuation).toList());
            line.leading = startsWithPhrase(line.original, terms);
        }
        List<LineMatch> ranked = new ArrayList<>(lines.values());
        ranked.sort(Comparator.comparing((LineMatch m) -> !m.exactTitle)
                .thenComparing(m -> !m.phrase)
                .thenComparing(Comparator.comparingInt(LineMatch::wholeWords).reversed())
                .thenComparing(m -> !m.leading)
                .thenComparing(m -> m.original.text().toLowerCase(Locale.ROOT))
                .thenComparingInt(m -> m.original.lineNumber()));
        List<SearchResult> results = new ArrayList<>(ranked.size());
        for (LineMatch line : ranked) {
            KwicEntry original = line.original;
            List<String> missing = new ArrayList<>();
            for (int t = 0; t < terms.size(); t++) {
                if (!line.found[t]) {
                    missing.add(terms.get(t));
                }
            }
            results.add(new SearchResult(original.lineNumber(), original.url(), original.text(), line.phrase,
                    List.copyOf(line.matches), List.copyOf(missing)));
        }
        return results;
    }

    private void displayResults(String query, List<SearchResult> results) {
        medium.displayResults(query, List.copyOf(results));
    }

    /** The words to search for: the query's words, without common ones unless only those remain. */
    private static List<String> searchTerms(String query) {
        List<String> words = words(query).stream().distinct().toList();
        List<String> meaningful = words.stream().filter(w -> !STOP_WORDS.contains(stripPunctuation(w))).toList();
        return meaningful.isEmpty() ? words : meaningful;
    }

    /** A line's title: its words before the first word ending in a colon, or all its words. */
    private static List<String> title(String text) {
        List<String> title = new ArrayList<>();
        for (String word : words(text)) {
            title.add(stripPunctuation(word));
            if (word.endsWith(":")) {
                break;
            }
        }
        title.removeIf(String::isEmpty);
        return title;
    }

    /** Whether the entry's shift starts with the search words, each as a word prefix. */
    private static boolean startsWithPhrase(KwicEntry entry, List<String> terms) {
        List<String> words = words(entry.text());
        if (words.size() < terms.size()) {
            return false;
        }
        for (int i = 0; i < terms.size(); i++) {
            if (!words.get(i).startsWith(terms.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Removes punctuation from the start and end of a word, so "sort:" counts as "sort". */
    private static String stripPunctuation(String word) {
        int start = 0;
        int end = word.length();
        while (start < end && !Character.isLetterOrDigit(word.codePointAt(start))) {
            start += Character.charCount(word.codePointAt(start));
        }
        while (end > start && !Character.isLetterOrDigit(word.codePointBefore(end))) {
            end -= Character.charCount(word.codePointBefore(end));
        }
        return word.substring(start, end);
    }

    /** Splits text into lowercase words. */
    private static List<String> words(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(WHITESPACE.split(text.strip()))
                .filter(w -> !w.isEmpty())
                .map(w -> w.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static final class LineMatch {
        final boolean[] found;
        final boolean[] whole;
        final List<PlacedEntry> matches = new ArrayList<>();
        boolean phrase;
        boolean exactTitle;
        boolean leading;
        KwicEntry original;

        LineMatch(int terms) {
            found = new boolean[terms];
            whole = new boolean[terms];
        }

        int foundWords() {
            int count = 0;
            for (boolean f : found) {
                count += f ? 1 : 0;
            }
            return count;
        }

        int wholeWords() {
            int count = 0;
            for (boolean w : whole) {
                count += w ? 1 : 0;
            }
            return count;
        }
    }
}
