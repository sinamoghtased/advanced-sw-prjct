package edu.utdallas.quickdex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates the circular shifts of each line.
 *
 * <p><b>Secret:</b> how shifts are generated and stored. {@link #setLines} copies a line out
 * of {@link LineStorage}; the private {@link #generateShifts} then records one shift per
 * word as a (line, offset) pair over that copy. Callers read shifts only through
 * {@link #getShiftedCharacter} and {@link #getShiftedWordCount}.
 *
 * <p>A line of <i>n</i> words yields <i>n</i> shifts, starting with the original order
 * (FR2.0). Shifts are numbered from 0 in the order they are generated.
 */
public final class CircularShift {

    /** Returned by {@link #getShiftedCharacter} for a position past the end of a word. */
    public static final int END = LineStorage.END;

    private final LineStorage storage;

    /** This component's own copy of each line it has been given, as code points per word. */
    private final List<int[][]> lines = new ArrayList<>();
    private final List<Integer> lineNumbers = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();

    /** Each shift, as an index into {@link #lines} and a word offset. */
    private int[] shiftLine = new int[64];
    private int[] shiftOffset = new int[64];
    private int shiftCount;

    public CircularShift(LineStorage storage) {
        this.storage = storage;
    }

    /**
     * Reads a stored line and generates its circular shifts.
     *
     * @param line the line's number in {@link LineStorage}
     */
    public void setLines(int line) {
        int wordCount = storage.getWordCount(line);
        int[][] words = new int[wordCount][];
        for (int w = 0; w < wordCount; w++) {
            int length = 0;
            while (storage.getCharacter(line, w, length) != LineStorage.END) {
                length++;
            }
            words[w] = new int[length];
            for (int c = 0; c < length; c++) {
                words[w][c] = storage.getCharacter(line, w, c);
            }
        }
        lines.add(words);
        lineNumbers.add(line);
        urls.add(storage.getUrl(line));
        generateShifts(lines.size() - 1);
    }

    private void generateShifts(int copy) {
        int wordCount = lines.get(copy).length;
        for (int offset = 0; offset < wordCount; offset++) {
            if (shiftCount == shiftLine.length) {
                shiftLine = Arrays.copyOf(shiftLine, shiftCount * 2);
                shiftOffset = Arrays.copyOf(shiftOffset, shiftCount * 2);
            }
            shiftLine[shiftCount] = copy;
            shiftOffset[shiftCount] = offset;
            shiftCount++;
        }
    }

    /** Returns the number of shifts generated so far, across all lines. */
    public int getShiftCount() {
        return shiftCount;
    }

    /** Returns the number of words in a shift. */
    public int getShiftedWordCount(int shift) {
        return lines.get(shiftLine[check(shift)]).length;
    }

    /**
     * Returns one character of a shift, or {@link #END} if {@code position} is past the end
     * of the word.
     *
     * @param shift    the shift number
     * @param word     the word's position within the shift
     * @param position the character's position within the word
     */
    public int getShiftedCharacter(int shift, int word, int position) {
        int[][] words = lines.get(shiftLine[check(shift)]);
        if (word < 0 || word >= words.length) {
            throw new IndexOutOfBoundsException("word " + word + " of " + words.length);
        }
        int[] chars = words[(shiftOffset[shift] + word) % words.length];
        if (position < 0) {
            throw new IndexOutOfBoundsException("position " + position);
        }
        return position < chars.length ? chars[position] : END;
    }

    /** Returns the {@link LineStorage} line number that a shift came from. */
    public int getShiftedLine(int shift) {
        return lineNumbers.get(shiftLine[check(shift)]);
    }

    /** Returns the URL of the line that a shift came from, or {@code null} if it has none. */
    public String getShiftedUrl(int shift) {
        return urls.get(shiftLine[check(shift)]);
    }

    private int check(int shift) {
        if (shift < 0 || shift >= shiftCount) {
            throw new IndexOutOfBoundsException("shift " + shift + " of " + shiftCount);
        }
        return shift;
    }
}
