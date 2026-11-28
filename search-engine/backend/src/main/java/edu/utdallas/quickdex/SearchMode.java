package edu.utdallas.quickdex;

import java.util.Locale;
import java.util.Optional;

/**
 * How a search combines its words (FR025).
 *
 * <p><b>Pattern: Strategy</b>, in its enum form: {@link SearchEngine} switches on the chosen
 * mode to decide which pages are kept.
 */
public enum SearchMode {
    /** Pages that have any of the words. This is the default. */
    OR,
    /** Pages that have every one of the words. */
    AND,
    /**
     * Pages that have the first word and none of the others. With a single word, the pages
     * that do not have it.
     */
    NOT;

    /** Reads a mode name in any case, or returns empty if it is not one of the three. */
    public static Optional<SearchMode> parse(String name) {
        try {
            return Optional.of(valueOf(name.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
