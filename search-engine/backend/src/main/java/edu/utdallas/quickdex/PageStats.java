package edu.utdallas.quickdex;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts the clicks on each page's URL and holds each page's bid, for ordering results by
 * "most clicks" or "highest bid" (FR030).
 *
 * <p><b>Secret:</b> how the counts are kept. A URL nobody has clicked or bid on has 0 of each.
 */
public final class PageStats {

    private final Map<String, Integer> clicks = new HashMap<>();
    private final Map<String, Integer> bids = new HashMap<>();

    /** Counts one click on a URL. A {@code null} URL is ignored. */
    public void recordClick(String url) {
        if (url != null) {
            clicks.merge(url, 1, Integer::sum);
        }
    }

    /** Returns how many times a URL has been clicked. */
    public int clicks(String url) {
        return url == null ? 0 : clicks.getOrDefault(url, 0);
    }

    /**
     * Sets what a URL bids to be shown first.
     *
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    public void setBid(String url, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("A bid cannot be negative.");
        }
        if (url != null) {
            bids.put(url, amount);
        }
    }

    /** Returns a URL's bid. */
    public int bid(String url) {
        return url == null ? 0 : bids.getOrDefault(url, 0);
    }
}
