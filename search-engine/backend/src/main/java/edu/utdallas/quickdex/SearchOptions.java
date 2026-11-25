package edu.utdallas.quickdex;

/**
 * The user's choices for one search.
 *
 * @param mode          how the search words combine (FR025)
 * @param caseSensitive whether "Man" and "man" are different words (FR026)
 */
public record SearchOptions(SearchMode mode, boolean caseSensitive) {

    /** OR search that ignores case. */
    public static final SearchOptions DEFAULT = new SearchOptions(SearchMode.OR, false);

    public SearchOptions withMode(SearchMode newMode) {
        return new SearchOptions(newMode, caseSensitive);
    }

    public SearchOptions withCaseSensitive(boolean sensitive) {
        return new SearchOptions(mode, sensitive);
    }
}
