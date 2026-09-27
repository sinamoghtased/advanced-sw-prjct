package edu.utdallas.quickdex;

/**
 * A new index entry together with its position in the updated sorted index.
 *
 * <p>Inserting a line's entries into a copy of the previous index, in ascending position
 * order, reproduces the updated index.
 *
 * @param position the entry's 0-based position in the updated index
 * @param entry    the entry
 */
public record PlacedEntry(int position, KwicEntry entry) {
}
