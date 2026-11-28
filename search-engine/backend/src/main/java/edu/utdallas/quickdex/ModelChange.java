package edu.utdallas.quickdex;

/**
 * What changed in the engine, sent to every {@link ModelListener}.
 *
 * @param kind what happened
 * @param url  the page it happened to, or {@code null} when it covers several pages
 */
public record ModelChange(Kind kind, String url) {

    /** The ways the engine's data can change after it is built. */
    public enum Kind {
        PAGES_ADDED, PAGE_DELETED, CLICKED, BID_CHANGED
    }
}
