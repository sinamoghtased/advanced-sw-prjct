package edu.utdallas.quickdex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Keeps every circular shift in global alphabetical order.
 *
 * <p><b>Secret:</b> the sorting algorithm. {@link #setShifts()} copies the shifts that
 * {@link CircularShift} generated since the last call, and the private
 * {@link #alphabetize} merge-sorts them and merges them into the shifts already kept, so
 * earlier lines are never re-sorted (FR4.0, incremental merge).
 *
 * <p>Shifts are compared word by word, in Unicode code-point order. {@link Order#CASE_INSENSITIVE}
 * (the default) compares words after locale-independent lowercasing. Shifts that compare
 * equal keep their generation order, so the same input always gives the same index.
 */
public final class Alphabetizer {

    /** How words are compared. */
    public enum Order {
        /** Ignore case: "apple" and "Apple" compare equal. */
        CASE_INSENSITIVE,
        /** Compare exact code points: uppercase letters sort before lowercase ones. */
        CASE_SENSITIVE
    }

    private final CircularShift shifts;
    private final Order order;
    private final NoiseFilter filter;

    /** How many of the generated shifts have been read, kept or dropped. */
    private int consumed;
    private int lastLine = -1;

    /** The first shift of each line (its original word order), kept even when it is filtered out. */
    private final Map<Integer, KwicEntry> originals = new HashMap<>();

    /** This component's own copy of every shift, in alphabetical order. */
    private List<SortedShift> sorted = new ArrayList<>();

    /** Where the shifts merged in by the last {@link #setShifts()} ended up. */
    private int[] newPositions = new int[0];

    public Alphabetizer(CircularShift shifts) {
        this(shifts, Order.CASE_INSENSITIVE);
    }

    public Alphabetizer(CircularShift shifts, Order order) {
        this(shifts, order, NoiseFilter.NONE);
    }

    public Alphabetizer(CircularShift shifts, Order order, NoiseFilter filter) {
        this.shifts = shifts;
        this.order = order;
        this.filter = filter;
    }

    /**
     * Reads the shifts generated since the last call and merges them into the sorted index,
     * leaving out the shifts that start with a noise word (FR018).
     */
    public void setShifts() {
        int last = shifts.getShiftCount();
        Map<String, int[]> keyCache = new HashMap<>();
        List<SortedShift> incoming = new ArrayList<>(last - consumed);
        for (int shift = consumed; shift < last; shift++) {
            SortedShift copy = copy(shift, keyCache);
            int line = shifts.getShiftedLine(shift);
            if (line != lastLine) {
                originals.put(copy.entry.lineNumber(), copy.entry);
                lastLine = line;
            }
            if (filter.keeps(copy.entry.keyword())) {
                incoming.add(copy);
            }
        }
        consumed = last;
        alphabetize(incoming);
    }

    /** Returns the numbers of the lines in the index, including lines whose shifts were all filtered out. */
    public java.util.Set<Integer> getLineNumbers() {
        return java.util.Set.copyOf(originals.keySet());
    }

    /** Returns the numbers of the lines that have this URL. */
    public List<Integer> findLines(String url) {
        return originals.values().stream()
                .filter(entry -> entry.url() != null && entry.url().equals(url))
                .map(KwicEntry::lineNumber).sorted().toList();
    }

    /**
     * Removes every shift of a line from the sorted index (FR023). The other shifts keep their
     * order and their ids.
     *
     * @return how many shifts were removed
     */
    public int removeLine(int lineNumber) {
        int before = sorted.size();
        sorted.removeIf(shift -> shift.entry.lineNumber() == lineNumber);
        originals.remove(lineNumber);
        newPositions = new int[0];
        return before - sorted.size();
    }

    /**
     * Returns a line's first shift, which has the words in their original order, even if the
     * index left it out as a noise-word shift.
     *
     * @param lineNumber the line's number, counting from 1 as {@link KwicEntry#lineNumber()} does
     */
    public KwicEntry getLineOriginal(int lineNumber) {
        return originals.get(lineNumber);
    }

    /** Returns the number of shifts in the sorted index. */
    public int getShiftCount() {
        return sorted.size();
    }

    /** Returns the entry at a position of the sorted index (0-based). */
    public KwicEntry getAlphabetizedShift(int position) {
        return sorted.get(position).entry;
    }

    /**
     * Returns the positions in the sorted index of the shifts merged in by the last
     * {@link #setShifts()}, in ascending order.
     */
    public int[] getNewShiftPositions() {
        return newPositions.clone();
    }

    private SortedShift copy(int shift, Map<String, int[]> keyCache) {
        int wordCount = shifts.getShiftedWordCount(shift);
        String[] words = new String[wordCount];
        int[][] keys = new int[wordCount][];
        for (int w = 0; w < wordCount; w++) {
            StringBuilder word = new StringBuilder();
            for (int c = 0; ; c++) {
                int ch = shifts.getShiftedCharacter(shift, w, c);
                if (ch == CircularShift.END) {
                    break;
                }
                word.appendCodePoint(ch);
            }
            words[w] = word.toString();
            // Rotations of a line share their words, so each word's key is built once.
            keys[w] = keyCache.computeIfAbsent(words[w], this::key);
        }
        String context = String.join(" ", Arrays.asList(words).subList(1, wordCount));
        KwicEntry entry = new KwicEntry(shift, shifts.getShiftedLine(shift) + 1, words[0], context,
                shifts.getShiftedUrl(shift));
        return new SortedShift(entry, keys);
    }

    private int[] key(String word) {
        String folded = order == Order.CASE_INSENSITIVE ? word.toLowerCase(Locale.ROOT) : word;
        return folded.codePoints().toArray();
    }

    /**
     * Merge-sorts the new shifts, then merges them into the shifts already kept. The kept
     * shifts are copied in runs between the insertion points, found by binary search.
     */
    private void alphabetize(List<SortedShift> incoming) {
        List<SortedShift> fresh = mergeSort(incoming);
        List<SortedShift> merged = new ArrayList<>(sorted.size() + fresh.size());
        int[] positions = new int[fresh.size()];
        int from = 0;
        for (int i = 0; i < fresh.size(); i++) {
            SortedShift shift = fresh.get(i);
            int to = insertionPoint(shift, from);
            merged.addAll(sorted.subList(from, to));
            positions[i] = merged.size();
            merged.add(shift);
            from = to;
        }
        merged.addAll(sorted.subList(from, sorted.size()));
        sorted = merged;
        newPositions = positions;
    }

    /** The first position at or after {@code from} whose kept shift sorts after {@code shift}. */
    private int insertionPoint(SortedShift shift, int from) {
        int low = from;
        int high = sorted.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sorted.get(middle).compareTo(shift) <= 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static List<SortedShift> mergeSort(List<SortedShift> list) {
        if (list.size() <= 1) {
            return list;
        }
        int middle = list.size() / 2;
        List<SortedShift> a = mergeSort(list.subList(0, middle));
        List<SortedShift> b = mergeSort(list.subList(middle, list.size()));
        List<SortedShift> out = new ArrayList<>(list.size());
        int i = 0;
        int j = 0;
        while (i < a.size() && j < b.size()) {
            out.add(a.get(i).compareTo(b.get(j)) <= 0 ? a.get(i++) : b.get(j++));
        }
        out.addAll(a.subList(i, a.size()));
        out.addAll(b.subList(j, b.size()));
        return out;
    }

    private record SortedShift(KwicEntry entry, int[][] keys) implements Comparable<SortedShift> {

        @Override
        public int compareTo(SortedShift other) {
            int words = Math.min(keys.length, other.keys.length);
            for (int w = 0; w < words; w++) {
                int c = Arrays.compare(keys[w], other.keys[w]);
                if (c != 0) {
                    return c;
                }
            }
            int c = Integer.compare(keys.length, other.keys.length);
            return c != 0 ? c : Integer.compare(entry.id(), other.entry.id());
        }
    }
}
