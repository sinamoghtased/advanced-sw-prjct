package edu.utdallas.quickdex;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

/**
 * Retrieves generated shifts from {@link CircularShift} and the sorted index from
 * {@link Alphabetizer}, then shows both on the Output Medium.
 *
 * <p><b>Secret:</b> how results are formatted. Through the private {@link #displayOutput},
 * {@link #getOutput()} shows only the entries added since the last call, with their
 * positions, so the page keeps updating after each line without receiving the whole index
 * again (output timing constraint).
 */
public final class Output {

    /** File formats for {@link #export}. */
    public enum Format {
        /** One entry per line: keyword, context, line number, and URL, separated by tabs. */
        TEXT,
        /** Comma-separated values with a header row. */
        CSV
    }

    private final Alphabetizer alphabetizer;
    private final CircularShift circularShift;
    private final OutputMedium medium;
    private final List<KwicEntry> index = new IndexView();

    /** The index size when entries were last shown. */
    private int shown;
    private int shownGenerated;

    public Output(Alphabetizer alphabetizer, CircularShift circularShift, OutputMedium medium) {
        this.alphabetizer = alphabetizer;
        this.circularShift = circularShift;
        this.medium = medium;
    }

    /**
     * Retrieves the sorted index and shows any entries added since the last call.
     *
     * @return the whole sorted index, as a read-only list
     */
    public List<KwicEntry> getOutput() {
        int size = alphabetizer.getShiftCount();
        if (size > shown) {
            int[] positions = alphabetizer.getNewShiftPositions();
            List<PlacedEntry> inserted = new ArrayList<>(positions.length);
            for (int position : positions) {
                inserted.add(new PlacedEntry(position, alphabetizer.getAlphabetizedShift(position)));
            }
            List<KwicEntry> generated = generatedSinceLastOutput();
            shown = size;
            displayOutput(inserted, generated, size);
        }
        return index;
    }

    private List<KwicEntry> generatedSinceLastOutput() {
        int end = circularShift.getShiftCount();
        List<KwicEntry> generated = new ArrayList<>(end - shownGenerated);
        for (int shift = shownGenerated; shift < end; shift++) {
            int wordCount = circularShift.getShiftedWordCount(shift);
            List<String> words = new ArrayList<>(wordCount);
            for (int word = 0; word < wordCount; word++) {
                StringBuilder value = new StringBuilder();
                for (int character = 0; ; character++) {
                    int codePoint = circularShift.getShiftedCharacter(shift, word, character);
                    if (codePoint == CircularShift.END) {
                        break;
                    }
                    value.appendCodePoint(codePoint);
                }
                words.add(value.toString());
            }
            generated.add(new KwicEntry(shift, circularShift.getShiftedLine(shift) + 1,
                    words.get(0), String.join(" ", words.subList(1, words.size())),
                    circularShift.getShiftedUrl(shift)));
        }
        shownGenerated = end;
        return List.copyOf(generated);
    }

    private void displayOutput(List<PlacedEntry> inserted, List<KwicEntry> generated, int size) {
        int lineNumber = generated.get(0).lineNumber();
        medium.displayOutput(lineNumber, List.copyOf(inserted), generated, size);
    }

    /** Formats the whole index as a file (FR11.0). User text is never treated as code. */
    public String export(Format format) {
        StringBuilder out = new StringBuilder();
        if (format == Format.CSV) {
            out.append("keyword,context,line,url\r\n");
        }
        for (KwicEntry e : index) {
            String url = e.url() == null ? "" : e.url();
            if (format == Format.CSV) {
                out.append(csvCell(e.keyword())).append(',')
                        .append(csvCell(e.context())).append(',')
                        .append(e.lineNumber()).append(',')
                        .append(csvCell(url)).append("\r\n");
            } else {
                out.append(e.keyword()).append('\t')
                        .append(e.context()).append('\t')
                        .append(e.lineNumber()).append('\t')
                        .append(url).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Quotes a CSV cell, and stops spreadsheets from reading it as a formula by prefixing
     * cells that start with a formula character with an apostrophe (NFR11.0).
     */
    static String csvCell(String value) {
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** A read-only view of the sorted index. */
    private final class IndexView extends AbstractList<KwicEntry> implements RandomAccess {
        @Override
        public KwicEntry get(int position) {
            return alphabetizer.getAlphabetizedShift(position);
        }

        @Override
        public int size() {
            return alphabetizer.getShiftCount();
        }
    }
}
