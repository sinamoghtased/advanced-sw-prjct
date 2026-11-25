package edu.utdallas.quickdex;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The user's choices for one search.
 *
 * @param mode           how the search words combine (FR025)
 * @param caseSensitive  whether "Man" and "man" are different words (FR026)
 * @param ignoredSymbols the symbols to leave out when matching query words to descriptions, such
 *                       as {@code "-&"}; empty for none (FR027)
 */
public record SearchOptions(SearchMode mode, boolean caseSensitive, String ignoredSymbols) {

    /** OR search that ignores case and no symbols. */
    public static final SearchOptions DEFAULT = new SearchOptions(SearchMode.OR, false, "");

    public SearchOptions {
        ignoredSymbols = ignoredSymbols == null ? "" : ignoredSymbols;
    }

    public SearchOptions withMode(SearchMode newMode) {
        return new SearchOptions(newMode, caseSensitive, ignoredSymbols);
    }

    public SearchOptions withCaseSensitive(boolean sensitive) {
        return new SearchOptions(mode, sensitive, ignoredSymbols);
    }

    public SearchOptions withIgnoredSymbols(String symbols) {
        return new SearchOptions(mode, caseSensitive, symbols);
    }

    /** The ignored symbols as code points, without whitespace. */
    public Set<Integer> ignoredCodePoints() {
        return ignoredSymbols.codePoints().filter(c -> !Character.isWhitespace(c))
                .boxed().collect(Collectors.toUnmodifiableSet());
    }
}
