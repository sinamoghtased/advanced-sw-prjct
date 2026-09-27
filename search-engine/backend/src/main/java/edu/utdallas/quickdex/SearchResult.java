package edu.utdallas.quickdex;

import java.util.List;

/**
 * One page found by a search: an input line and the index entries that matched it.
 *
 * @param lineNumber   the line that matched (1-based, counting only non-blank lines)
 * @param url          the line's URL, or {@code null} if it has none
 * @param text         the line's words in their original order
 * @param phraseMatch  whether the search words appear together, in order, in the line
 * @param matches      the index entries whose keyword starts with a search word, with their
 *                     positions in the index, in index order
 * @param missingWords the search words the line does not contain; empty unless no line
 *                     contains every word
 */
public record SearchResult(int lineNumber, String url, String text, boolean phraseMatch,
                           List<PlacedEntry> matches, List<String> missingWords) {
}
