package edu.utdallas.quickdex;

/**
 * An Input Medium that reads lines from a block of text, such as the contents of the web
 * page's text box or an uploaded file.
 */
public final class TextInputMedium implements InputMedium {

    private final String[] lines;
    private final long delayMillis;
    private int next;

    /** Reads the lines of {@code text}. */
    public TextInputMedium(String text) {
        this(text, 0);
    }

    /**
     * Reads the lines of {@code text}, waiting {@code delayMillis} before each line after the
     * first. The delay slows processing down so that a demo can show the index being built
     * line by line.
     */
    public TextInputMedium(String text, long delayMillis) {
        this.lines = Input.splitLines(text);
        this.delayMillis = Math.max(0, delayMillis);
    }

    /** Returns the number of lines in the text, including blank ones. */
    public int lineCount() {
        return lines.length;
    }

    @Override
    public String readLine() {
        if (next >= lines.length) {
            return null;
        }
        if (delayMillis > 0 && next > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return lines[next++];
    }
}
