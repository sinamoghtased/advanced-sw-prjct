package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR023 Deleting an out-of-date page. */
class DeleteTest {

    private RecordingMedium medium;
    private QueueInputMedium queue;
    private MasterControl control;

    @BeforeEach
    void buildIndex() {
        medium = new RecordingMedium();
        queue = new QueueInputMedium();
        control = new MasterControl(queue, medium);
        queue.add(List.of(
                "http://old.example.com Quokka habitats of Western Australia",
                "http://new.example.org Quokka diet and behavior",
                "http://other.example.net Okapi forest giraffe"));
        control.run();
    }

    private List<String> found(String words) {
        control.search(words);
        return medium.results.stream().map(SearchResult::url).toList();
    }

    @Test
    void removesThePageAndAllItsShiftsFromTheIndex() {
        int before = control.index().size();
        assertEquals(List.of("http://new.example.org", "http://old.example.com"), found("quokka"));

        assertEquals(1, control.delete("http://old.example.com"));

        assertEquals(List.of("http://new.example.org"), found("quokka"));
        assertEquals(List.of(), found("habitats"));
        assertEquals(List.of(), found("western"));
        // The page has 5 shifts; the one that starts with "of" was never in the index, so 4 leave it.
        assertEquals(before - 4, control.index().size());
        assertTrue(control.index().stream().noneMatch(e -> "http://old.example.com".equals(e.url())));
    }

    @Test
    void leavesTheOtherPagesAndTheirOrderAlone() {
        List<String> others = control.index().stream()
                .filter(e -> !"http://old.example.com".equals(e.url())).map(KwicEntry::text).toList();
        control.delete("http://old.example.com");
        assertEquals(others, control.index().stream().map(KwicEntry::text).toList());
        assertEquals(List.of("http://other.example.net"), found("okapi"));
    }

    @Test
    void deletingAnUnknownUrlChangesNothing() {
        int before = control.index().size();
        assertEquals(0, control.delete("http://nobody.example.com"));
        assertEquals(0, control.delete(null));
        assertEquals(before, control.index().size());
    }

    @Test
    void forgetsTheDeletedPagesClicksAndBid() {
        control.recordClick("http://old.example.com");
        control.setBid("http://old.example.com", 7);
        control.delete("http://old.example.com");
        assertEquals(0, control.clicks("http://old.example.com"));
        assertEquals(0, control.bid("http://old.example.com"));
    }

    @Test
    void pagesAddedAfterADeleteStillGoIntoTheIndex() {
        control.delete("http://old.example.com");
        queue.add(List.of("http://newest.example.com Quokka selfies"));
        control.run();
        assertEquals(List.of("http://new.example.org", "http://newest.example.com"), found("quokka"));
        control.delete("http://new.example.org");
        assertEquals(List.of("http://newest.example.com"), found("quokka"));
    }

    @Test
    void aNotSearchOfOneWordSkipsDeletedPages() {
        control.delete("http://old.example.com");
        control.search("quokka", SearchOptions.DEFAULT.withMode(SearchMode.NOT));
        assertEquals(List.of("http://other.example.net"), medium.results.stream().map(SearchResult::url).toList());
    }
}
