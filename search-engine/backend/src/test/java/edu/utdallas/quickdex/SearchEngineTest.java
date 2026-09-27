package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR9.0 search. */
class SearchEngineTest {

    private RecordingMedium medium;
    private MasterControl control;

    @BeforeEach
    void buildIndex() {
        medium = new RecordingMedium();
        control = new MasterControl(new TextInputMedium("""
                https://a.example Descent of Man
                The Ascent of Man
                Key Word in Context
                Man in the Middle
                https://b.example Software design and architecture
                Designing software systems
                """), medium);
        control.run();
    }

    private List<String> search(String words) {
        control.search(words);
        return medium.results.stream().map(SearchResult::text).toList();
    }

    @Test
    void findsEveryLineWithAKeywordStartingWithTheWord() {
        assertEquals(List.of("Man in the Middle", "Descent of Man", "The Ascent of Man"), search("ma"));
    }

    @Test
    void listsTheEntriesThatMatchedInIndexOrder() {
        control.search("man");
        SearchResult descent = medium.results.stream()
                .filter(r -> r.text().equals("Descent of Man")).findFirst().orElseThrow();
        assertEquals(1, descent.matches().size());
        assertEquals("Man", descent.matches().get(0).entry().keyword());
        assertEquals("Descent of", descent.matches().get(0).entry().context());
    }

    @Test
    void ranksWholeWordMatchesAboveWordStarts() {
        MasterControl sorts = new MasterControl(new TextInputMedium(
                "Binary search in a sorted list\nMerge sort: a stable sort\nSorting algorithms"), medium);
        sorts.run();
        sorts.search("sort");
        assertEquals(List.of("Merge sort: a stable sort", "Sorting algorithms", "Binary search in a sorted list"),
                medium.results.stream().map(SearchResult::text).toList());
    }

    @Test
    void ignoresCaseAndExtraSpaces() {
        assertEquals(List.of("Descent of Man"), search("  DESC "));
    }

    @Test
    void requiresEveryWordAndListsPhraseMatchesFirst() {
        // "software design" appears as a phrase only in the first line.
        assertEquals(List.of("Software design and architecture", "Designing software systems"),
                search("software design"));
        assertTrue(medium.results.get(0).phraseMatch());
        assertFalse(medium.results.get(1).phraseMatch());
        assertEquals(List.of("Software design and architecture"), search("design architecture"));
    }

    @Test
    void findsNothingForNoWordsOrUnknownWords() {
        assertEquals(List.of(), search(""));
        assertEquals(List.of(), search("zebra"));
        assertEquals("zebra", medium.query);
    }

    @Test
    void neverSuggestsWordsThatWrapAroundTheLine() {
        // "Descent of Man" has the shift "Man Descent of", but "Descent" does not follow "Man".
        assertEquals(List.of(), control.suggest("man d", 8));
    }

    @Test
    void ranksAnExactTitleFirstThenLinesThatStartWithTheWords() {
        MasterControl pages = new MasterControl(new TextInputMedium("""
                Binary black hole: two black holes in orbit
                Black hole cosmology: a model of the universe
                Black hole: region of spacetime
                Supermassive black hole: the largest type
                """), medium);
        pages.run();
        pages.search("black hole");
        assertEquals(List.of("Black hole: region of spacetime", "Black hole cosmology: a model of the universe",
                        "Binary black hole: two black holes in orbit", "Supermassive black hole: the largest type"),
                medium.results.stream().map(SearchResult::text).toList());
    }

    @Test
    void ignoresCommonWords() {
        assertEquals(List.of("Man in the Middle", "Descent of Man", "The Ascent of Man"), search("the man"));
        assertEquals(List.of("The Ascent of Man", "Man in the Middle"), search("the"));
    }

    @Test
    void showsLinesWithMostWordsWhenNoneHasAll() {
        assertEquals(List.of("Descent of Man", "Man in the Middle", "The Ascent of Man"), search("man zebra"));
        assertEquals(List.of("zebra"), medium.results.get(0).missingWords());
        search("man");
        assertEquals(List.of(), medium.results.get(0).missingWords());
    }

    @Test
    void suggestsCompletionsFromTheIndex() {
        assertEquals(List.of("software", "software design", "software systems"), control.suggest("soft", 8));
        assertEquals(List.of("software design", "software design and architecture"), control.suggest("software d", 8));
        assertEquals(List.of("designing", "designing software", "design", "design and architecture"), control.suggest("desig", 8));
        assertEquals(List.of(), control.suggest("zeb", 8));
        assertEquals(List.of(), control.suggest("  ", 8));
    }

    @Test
    void suggestsTheNextWordAfterASpace() {
        assertEquals(List.of("man in the middle"), control.suggest("man ", 8));
    }

    @Test
    void returnsEachLinesUrlAndNumber() {
        control.search("descent");
        SearchResult result = medium.results.get(0);
        assertEquals("https://a.example", result.url());
        assertEquals(1, result.lineNumber());
    }
}
