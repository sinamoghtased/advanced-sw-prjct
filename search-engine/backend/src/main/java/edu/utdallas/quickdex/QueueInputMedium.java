package edu.utdallas.quickdex;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * An Input Medium that lines can be added to over time, such as pages fetched from the web.
 *
 * <p>{@link MasterControl#run()} processes the lines added so far; after more lines are
 * added, calling it again adds them to the same index, one line at a time.
 */
public final class QueueInputMedium implements InputMedium {

    private final Deque<String> lines = new ArrayDeque<>();

    /** Adds lines to be read. */
    public synchronized void add(Collection<String> newLines) {
        lines.addAll(newLines);
    }

    @Override
    public synchronized String readLine() {
        return lines.poll();
    }
}
