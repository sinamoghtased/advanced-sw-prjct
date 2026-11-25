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
 * @param order          how the matching pages are ordered (FR030)
 */
public record SearchOptions(SearchMode mode, boolean caseSensitive, String ignoredSymbols, ResultOrder order) {

    /** OR search that ignores case and no symbols, with the best match first. */
    public static final SearchOptions DEFAULT = new SearchOptions(SearchMode.OR, false, "");

    public SearchOptions {
        ignoredSymbols = ignoredSymbols == null ? "" : ignoredSymbols;
    }

    public SearchOptions(SearchMode mode, boolean caseSensitive, String ignoredSymbols) {
        this(mode, caseSensitive, ignoredSymbols, ResultOrder.RELEVANCE);
    }

    public SearchOptions withMode(SearchMode newMode) {
        return new SearchOptions(newMode, caseSensitive, ignoredSymbols, order);
    }

    public SearchOptions withCaseSensitive(boolean sensitive) {
        return new SearchOptions(mode, sensitive, ignoredSymbols, order);
    }

    public SearchOptions withIgnoredSymbols(String symbols) {
        return new SearchOptions(mode, caseSensitive, symbols, order);
    }

    public SearchOptions withOrder(ResultOrder newOrder) {
        return new SearchOptions(mode, caseSensitive, ignoredSymbols, newOrder);
    }

    /** The ignored symbols as code points, without whitespace. */
    public Set<Integer> ignoredCodePoints() {
        return ignoredSymbols.codePoints().filter(c -> !Character.isWhitespace(c))
                .boxed().collect(Collectors.toUnmodifiableSet());
    }
}
