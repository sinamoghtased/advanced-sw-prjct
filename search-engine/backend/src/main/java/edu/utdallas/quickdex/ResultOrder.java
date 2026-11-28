package edu.utdallas.quickdex;

import java.util.Locale;
import java.util.Optional;

/**
 * How search results are ordered (FR015, FR030). The matching pages are the same in every order.
 *
 * <p><b>Pattern: Strategy</b>, in its enum form: {@link SearchEngine} switches on the chosen
 * order to pick the comparison, so a new order is a new constant and one new case.
 */
public enum ResultOrder {
    /** Best match first. This is the default. */
    RELEVANCE,
    /** A to Z by the page's text. */
    ALPHABETICAL,
    /** Most clicked first. */
    CLICKS,
    /** Highest bid first. */
    BID;

    /** Reads an order name in any case, or returns empty if it is not one of the four. */
    public static Optional<ResultOrder> parse(String name) {
        try {
            return Optional.of(valueOf(name.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
