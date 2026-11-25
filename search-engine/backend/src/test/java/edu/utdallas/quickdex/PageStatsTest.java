package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** FR030 click and bid counts. */
class PageStatsTest {

    @Test
    void countsClicksPerUrl() {
        PageStats stats = new PageStats();
        stats.recordClick("http://a.example.com");
        stats.recordClick("http://a.example.com");
        stats.recordClick("http://b.example.com");
        stats.recordClick(null);
        assertEquals(2, stats.clicks("http://a.example.com"));
        assertEquals(1, stats.clicks("http://b.example.com"));
        assertEquals(0, stats.clicks("http://c.example.com"));
        assertEquals(0, stats.clicks(null));
    }

    @Test
    void keepsTheLatestBidPerUrl() {
        PageStats stats = new PageStats();
        stats.setBid("http://a.example.com", 5);
        stats.setBid("http://a.example.com", 3);
        assertEquals(3, stats.bid("http://a.example.com"));
        assertEquals(0, stats.bid("http://b.example.com"));
    }

    @Test
    void rejectsANegativeBid() {
        assertThrows(IllegalArgumentException.class, () -> new PageStats().setBid("http://a.example.com", -1));
    }
}
