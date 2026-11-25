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
 * nothing else. The {@link SearchMode} says how the words combine: OR (the default) shows
 * lines with any of them, with the missing words listed; AND shows lines with all of them;
 * NOT shows lines with the first word and none of the others. Lines are ranked by these
 * rules, in order:
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

    /** Words shorter than this are never corrected: too many index words are one typo away. */
    private static final int MIN_TYPO_LENGTH = 3;

    /** Words with this many letters or more may have two typos; shorter ones may have one. */
    private static final int LONG_WORD = 6;

    /** Words too common to narrow a search; the same words the index leaves out as noise. */
    static final Set<String> STOP_WORDS = NoiseFilter.NOISE_WORDS;

    private final Output output;
    private final OutputMedium medium;
    private final PageStats stats;

    public SearchEngine(Output output, OutputMedium medium) {
        this(output, medium, new PageStats());
    }

    public SearchEngine(Output output, OutputMedium medium, PageStats stats) {
        this.output = output;
        this.medium = medium;
        this.stats = stats;
    }

    /**
     * Searches the index and shows the matching pages on the Output Medium. A search with no
     * words finds nothing.
     */
    public void setSearchEngine(String query) {
        setSearchEngine(query, SearchOptions.DEFAULT);
    }

    /** Searches the index with the user's options: the mode, case, and symbols to ignore. */
    public void setSearchEngine(String query, SearchOptions options) {
        List<KwicEntry> index = output.getOutput();
        displayResults(query, searchKeywordMatches(query, index, options));
    }

    /**
     * Suggests up to {@code limit} searches that complete what the user has typed so far
     * (FR12.0). Each suggestion is the start of a circular shift in the index: the typed words
     * with the last one completed, alone and extended to the next meaningful word, skipping up
     * to two common words such as "of" or "the" ("man" suggests "man in the middle"). Only
     * words that follow each other in the original line are used, never words that wrap
     * around from its end. Suggestions made from more shifts come first, then alphabetically;
     * a shift that starts its line (usually a page title) counts three times.
     *
     * <p>If there are fewer than {@code limit} completions, a mistyped word is also corrected
     * to the closest word in the index (FR028), and the corrected text is completed the same
     * way. "sofware arch" suggests "software architecture".
     */
    public List<String> suggestSearches(String typed, int limit) {
        List<String> suggestions = completions(typed, limit, false);
        if (suggestions.size() >= limit || limit <= 0) {
            return suggestions;
        }
        String corrected = correctTypos(typed);
        if (corrected == null) {
            return suggestions;
        }
        List<String> all = new ArrayList<>(suggestions);
        for (String suggestion : completions(corrected, limit, true)) {
            if (!all.contains(suggestion) && all.size() < limit) {
                all.add(suggestion);
            }
        }
        return List.copyOf(all);
    }

    /** The typed text with each mistyped word replaced by a close word from the index, or null if none was. */
    private String correctTypos(String typed) {
        List<String> words = words(typed);
        if (words.isEmpty()) {
            return null;
        }
        boolean lastComplete = Character.isWhitespace(typed.codePointBefore(typed.length()));
        Map<String, Integer> vocabulary = new HashMap<>();
        for (KwicEntry entry : output.getOutput()) {
            String word = stripPunctuation(entry.keyword().toLowerCase(Locale.ROOT));
            if (!word.isEmpty()) {
                vocabulary.merge(word, 1, Integer::sum);
            }
        }
        boolean changed = false;
        List<String> fixed = new ArrayList<>(words.size());
        for (int i = 0; i < words.size(); i++) {
            String word = stripPunctuation(words.get(i));
            boolean partial = i == words.size() - 1 && !lastComplete;
            String replacement = word.length() < MIN_TYPO_LENGTH || STOP_WORDS.contains(word)
                    ? word : closestWord(word, partial, vocabulary);
            changed |= !replacement.equals(word);
            fixed.add(replacement);
        }
        return changed ? String.join(" ", fixed) + (lastComplete ? " " : "") : null;
    }

    /**
     * The index word closest to a typed word, within the allowed number of typos, or the typed
     * word itself if it is already right. A word still being typed is already right if some
     * index word starts with it, and is compared with the starts of index words otherwise.
     */
    private static String closestWord(String word, boolean partial, Map<String, Integer> vocabulary) {
        if (partial ? vocabulary.keySet().stream().anyMatch(v -> v.startsWith(word)) : vocabulary.containsKey(word)) {
            return word;
        }
        int allowed = word.length() >= LONG_WORD ? 2 : 1;
        String best = word;
        int bestDistance = allowed + 1;
        int bestCount = 0;
        for (Map.Entry<String, Integer> candidate : vocabulary.entrySet()) {
            String v = candidate.getKey();
            int distance = editDistance(word, v);
            if (partial) {
                for (int length = Math.max(1, word.length() - 1); length <= Math.min(v.length(), word.length() + 1); length++) {
                    distance = Math.min(distance, editDistance(word, v.substring(0, length)));
                }
            }
            boolean better = distance < bestDistance
                    || (distance == bestDistance && distance <= allowed
                    && (candidate.getValue() > bestCount
                    || (candidate.getValue() == bestCount && v.compareTo(best) < 0)));
            if (distance <= allowed && better) {
                best = v;
                bestDistance = distance;
                bestCount = candidate.getValue();
            }
        }
        return best;
    }

    /** The number of insertions, deletions, substitutions and swaps of neighbors that turn one word into the other. */
    static int editDistance(String a, String b) {
        int[] x = a.codePoints().toArray();
        int[] y = b.codePoints().toArray();
        int[][] d = new int[x.length + 1][y.length + 1];
        for (int i = 0; i <= x.length; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= y.length; j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= x.length; i++) {
            for (int j = 1; j <= y.length; j++) {
                int cost = x[i - 1] == y[j - 1] ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && x[i - 1] == y[j - 2] && x[i - 2] == y[j - 1]) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[x.length][y.length];
    }

    /** @param includeTyped whether the typed text itself, once completed, may be suggested */
    private List<String> completions(String typed, int limit, boolean includeTyped) {
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
            firstId.computeIfAbsent(entry.lineNumber(), n -> output.getLineOriginal(n).id());
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
            if (includeTyped || !completed.equals(typedText)) {
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

    private List<SearchResult> searchKeywordMatches(String query, List<KwicEntry> index, SearchOptions options) {
        SearchMode mode = options.mode();
        List<String> terms = searchTerms(query, options);
        if (terms.isEmpty()) {
            return List.of();
        }

        // Pass 1: find the entries whose keyword starts with a search word, grouped by line.
        Map<Integer, LineMatch> lines = new HashMap<>();
        for (int position = 0; position < index.size(); position++) {
            KwicEntry entry = index.get(position);
            String keyword = norm(entry.keyword(), options);
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
                if (!line.phrase && startsWithPhrase(entry, terms, options)) {
                    line.phrase = true;
                }
            }
        }
        // Keep the lines the mode accepts. Every line in the map so far has at least one word.
        switch (mode) {
            case OR -> { }
            case AND -> lines.values().removeIf(line -> line.foundWords() < terms.size());
            case NOT -> {
                if (terms.size() > 1) {
                    lines.values().removeIf(line -> !line.found[0] || line.foundWords() > 1);
                } else {
                    // One word: every line that does not have it.
                    Map<Integer, LineMatch> without = new HashMap<>();
                    for (int n = 1; n <= output.getLineCount(); n++) {
                        if (!lines.containsKey(n)) {
                            without.put(n, new LineMatch(1));
                        }
                    }
                    lines.clear();
                    lines.putAll(without);
                }
            }
        }
        if (lines.isEmpty()) {
            return List.of();
        }

        // Pass 2: recover each matching line's original text, its first-generated shift.
        for (Map.Entry<Integer, LineMatch> line : lines.entrySet()) {
            line.getValue().original = output.getLineOriginal(line.getKey());
        }

        for (LineMatch line : lines.values()) {
            line.exactTitle = title(line.original.text(), options).equals(terms.stream().map(SearchEngine::stripPunctuation).toList());
            line.leading = startsWithPhrase(line.original, terms, options);
        }
        List<LineMatch> ranked = new ArrayList<>(lines.values());
        ranked.sort(Comparator.comparing((LineMatch m) -> !m.exactTitle)
                .thenComparing(m -> !m.phrase)
                .thenComparing(Comparator.comparingInt(LineMatch::foundWords).reversed())
                .thenComparing(Comparator.comparingInt(LineMatch::wholeWords).reversed())
                .thenComparing(m -> !m.leading)
                .thenComparing(m -> m.original.text().toLowerCase(Locale.ROOT))
                .thenComparingInt(m -> m.original.lineNumber()));
        // The order only rearranges the matching pages; ties keep their relevance order.
        Comparator<LineMatch> alphabetical = Comparator.comparing(m -> m.original.text().toLowerCase(Locale.ROOT));
        switch (options.order()) {
            case RELEVANCE -> { }
            case ALPHABETICAL -> ranked.sort(alphabetical);
            case CLICKS -> ranked.sort(Comparator.comparingInt((LineMatch m) -> stats.clicks(m.original.url()))
                    .reversed().thenComparing(alphabetical));
            case BID -> ranked.sort(Comparator.comparingInt((LineMatch m) -> stats.bid(m.original.url()))
                    .reversed().thenComparing(alphabetical));
        }
        List<SearchResult> results = new ArrayList<>(ranked.size());
        for (LineMatch line : ranked) {
            KwicEntry original = line.original;
            List<String> missing = new ArrayList<>();
            for (int t = 0; mode == SearchMode.OR && t < terms.size(); t++) {
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
    private static List<String> searchTerms(String query, SearchOptions options) {
        List<String> words = words(query, options).stream().distinct().toList();
        List<String> meaningful = words.stream()
                .filter(w -> !STOP_WORDS.contains(stripPunctuation(w).toLowerCase(Locale.ROOT))).toList();
        return meaningful.isEmpty() ? words : meaningful;
    }

    /** A line's title: its words before the first word ending in a colon, or all its words. */
    private static List<String> title(String text, SearchOptions options) {
        List<String> title = new ArrayList<>();
        for (String word : words(text, options)) {
            title.add(stripPunctuation(word));
            if (word.endsWith(":")) {
                break;
            }
        }
        title.removeIf(String::isEmpty);
        return title;
    }

    /** Whether the entry's shift starts with the search words, each as a word prefix. */
    private static boolean startsWithPhrase(KwicEntry entry, List<String> terms, SearchOptions options) {
        List<String> words = words(entry.text(), options);
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

    /** Removes the symbols the user ignores, then lowercases the text unless case matters. */
    private static String norm(String text, SearchOptions options) {
        String kept = text;
        Set<Integer> ignored = options.ignoredCodePoints();
        if (!ignored.isEmpty()) {
            kept = text.codePoints().filter(c -> !ignored.contains(c))
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        }
        return options.caseSensitive() ? kept : kept.toLowerCase(Locale.ROOT);
    }

    /** Splits text into lowercase words. */
    private static List<String> words(String text) {
        return words(text, SearchOptions.DEFAULT);
    }

    /** Splits text into words, lowercased unless case matters. */
    private static List<String> words(String text, SearchOptions options) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(WHITESPACE.split(text.strip()))
                .filter(w -> !w.isEmpty())
                .map(w -> norm(w, options))
                .filter(w -> !w.isEmpty())
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
