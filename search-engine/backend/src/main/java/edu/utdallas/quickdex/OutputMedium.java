package edu.utdallas.quickdex;

import java.util.List;

/**
 * Where results go: the web page, a file, or the console.
 *
 * <p>The Output Medium sits outside the KWIC system and is reached through system I/O.
 *
 * <p><b>Pattern: Bridge.</b> The pipeline stays the same while the medium varies: the web page
 * ({@code WebOutputMedium}), the console, and the recorder the tests use.
 */
public interface OutputMedium {

    /**
     * Shows the entries that were added to the sorted index by the line just processed.
     *
     * @param lineNumber the line that produced the entries (1-based)
     * @param inserted   the new entries, each with its position in the updated index, in
     *                   ascending position order
     * @param generated  the shifts for the current line in their original generation order
     * @param filtered   the shifts of {@code generated} that remain after noise-word filtering
     * @param indexSize  the number of entries in the index after the update
     */
    void displayOutput(int lineNumber, List<PlacedEntry> inserted, List<KwicEntry> generated,
            List<KwicEntry> filtered, int indexSize);

    /**
     * Shows the pages that match a search.
     *
     * @param query   the search words, as the user entered them
     * @param results the matching pages, best first
     */
    void displayResults(String query, List<SearchResult> results);
}
