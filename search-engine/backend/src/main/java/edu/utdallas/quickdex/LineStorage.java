package edu.utdallas.quickdex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores the characters and words of every line, and the URL of each line.
 *
 * <p><b>Secret:</b> how lines are stored. Callers see lines, words, and characters only
 * through {@link #setCharacter}, {@link #getCharacter}, and {@link #getWordCount}; the
 * representation can change without affecting them.
 *
 * <p>Characters are Unicode code points. Lines, words, and characters are numbered from 0
 * and are added in order: a new line, word, or character is created by setting the
 * position just past the current end.
 */
public final class LineStorage {

    /** Returned by {@link #getCharacter} for a position past the end of a word. */
    public static final int END = -1;

    private final List<Line> lines = new ArrayList<>();

    /**
     * Sets one character of a word.
     *
     * @param line      the line number; may be one past the last line to start a new line
     * @param word      the word number; may be one past the last word to start a new word
     * @param position  the character position; may be one past the end to append
     * @param codePoint the character, as a Unicode code point
     * @throws IllegalArgumentException if a position skips ahead of the current end
     */
    public void setCharacter(int line, int word, int position, int codePoint) {
        if (line == lines.size()) {
            lines.add(new Line());
        }
        Line l = lineAt(line);
        if (word == l.words.size()) {
            l.words.add(new Word());
        }
        wordAt(l, word).set(position, codePoint);
    }

    /**
     * Returns one character of a word, or {@link #END} if {@code position} is past the end
     * of the word.
     */
    public int getCharacter(int line, int word, int position) {
        Word w = wordAt(lineAt(line), word);
        if (position < 0) {
            throw new IndexOutOfBoundsException("position " + position);
        }
        return position < w.length ? w.codePoints[position] : END;
    }

    /** Returns the number of words in a line. */
    public int getWordCount(int line) {
        return lineAt(line).words.size();
    }

    /** Returns the number of lines stored. */
    public int getLineCount() {
        return lines.size();
    }

    /** Associates a URL with a line, or removes it when {@code url} is {@code null}. */
    public void setUrl(int line, String url) {
        lineAt(line).url = url;
    }

    /** Returns the URL associated with a line, or {@code null} if it has none. */
    public String getUrl(int line) {
        return lineAt(line).url;
    }

    private Line lineAt(int line) {
        if (line < 0 || line >= lines.size()) {
            throw new IndexOutOfBoundsException("line " + line + " of " + lines.size());
        }
        return lines.get(line);
    }

    private static Word wordAt(Line line, int word) {
        if (word < 0 || word >= line.words.size()) {
            throw new IndexOutOfBoundsException("word " + word + " of " + line.words.size());
        }
        return line.words.get(word);
    }

    private static final class Line {
        final List<Word> words = new ArrayList<>();
        String url;
    }

    private static final class Word {
        int[] codePoints = new int[8];
        int length;

        void set(int position, int codePoint) {
            if (position < 0 || position > length) {
                throw new IllegalArgumentException(
                        "position " + position + " skips ahead of the word's end (" + length + ")");
            }
            if (position == length) {
                if (length == codePoints.length) {
                    codePoints = Arrays.copyOf(codePoints, length * 2);
                }
                length++;
            }
            codePoints[position] = codePoint;
        }
    }
}
