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
    void listsLinesWithMoreWordsAndPhraseMatchesFirst() {
        // "software design" appears as a phrase only in the first line.
        assertEquals(List.of("Software design and architecture", "Designing software systems"),
                search("software design"));
        assertTrue(medium.results.get(0).phraseMatch());
        assertFalse(medium.results.get(1).phraseMatch());
        // OR lists the line with both words first, then the line with only one of them.
        assertEquals(List.of("Software design and architecture", "Designing software systems"),
                search("design architecture"));
        assertEquals(List.of("Software design and architecture"), search("design architecture", SearchMode.AND));
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
        // Shifts that start with a noise word are not in the index, so a query of only those finds nothing.
        assertEquals(List.of(), search("the"));
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

    private List<String> search(String words, SearchMode mode) {
        control.search(words, SearchOptions.DEFAULT.withMode(mode));
        return medium.results.stream().map(SearchResult::text).toList();
    }

    @Test
    void orIsTheDefaultAndFindsLinesWithAnyWord() {
        List<String> either = search("software architecture");
        assertEquals(List.of("Software design and architecture", "Designing software systems"), either);
        assertEquals(either, search("software architecture", SearchMode.OR));
        assertEquals(List.of("architecture"), medium.results.get(1).missingWords());
    }

    @Test
    void andFindsOnlyLinesWithEveryWord() {
        assertEquals(List.of("Software design and architecture"), search("software architecture", SearchMode.AND));
        assertEquals(List.of(), medium.results.get(0).missingWords());
        assertEquals(List.of(), search("man zebra", SearchMode.AND));
    }

    @Test
    void notFindsLinesWithTheFirstWordAndNoneOfTheOthers() {
        assertEquals(List.of("Designing software systems"), search("software architecture", SearchMode.NOT));
        assertEquals(List.of(), medium.results.get(0).missingWords());
        assertEquals(List.of(), search("software systems design", SearchMode.NOT));
    }

    @Test
    void notWithOneWordFindsTheLinesWithoutIt() {
        assertEquals(List.of("Designing software systems", "Key Word in Context", "Software design and architecture"),
                search("man", SearchMode.NOT));
    }

    @Test
    void readsModeNamesInAnyCase() {
        assertEquals(SearchMode.AND, SearchMode.parse("and").orElseThrow());
        assertEquals(SearchMode.NOT, SearchMode.parse(" Not ").orElseThrow());
        assertTrue(SearchMode.parse("xor").isEmpty());
    }

    @Test
    void ignoresCaseByDefault() {
        assertEquals(3, search("MAN").size());
        assertEquals(List.of("Key Word in Context"), search("kEy"));
    }

    @Test
    void caseSensitiveSearchMatchesOnlyTheSameCase() {
        MasterControl control = new MasterControl(new TextInputMedium(
                "Java the language\njava coffee beans\nJAVA acronym"), medium);
        control.run();
        SearchOptions sensitive = SearchOptions.DEFAULT.withCaseSensitive(true);

        control.search("Java", sensitive);
        assertEquals(List.of("Java the language"), medium.results.stream().map(SearchResult::text).toList());
        control.search("java", sensitive);
        assertEquals(List.of("java coffee beans"), medium.results.stream().map(SearchResult::text).toList());
        control.search("JA", sensitive);
        assertEquals(List.of("JAVA acronym"), medium.results.stream().map(SearchResult::text).toList());
        control.search("Java");
        assertEquals(3, medium.results.size());
    }

    @Test
    void caseSensitiveSearchStillIgnoresCommonWords() {
        control.search("The Man", SearchOptions.DEFAULT.withCaseSensitive(true).withMode(SearchMode.AND));
        // "The" is a common word, so only "Man" is searched, with its case.
        assertEquals(3, medium.results.size());
    }

    @Test
    void ignoredSymbolsAreLeftOutOfQueryAndDescriptionWords() {
        MasterControl control = new MasterControl(new TextInputMedium(
                "Self-driving cars\nC++ programming guide\nDrivers & cars"), medium);
        control.run();

        control.search("selfdriving");
        assertEquals(0, medium.results.size());
        control.search("selfdriving", SearchOptions.DEFAULT.withIgnoredSymbols("-"));
        assertEquals(List.of("Self-driving cars"), medium.results.stream().map(SearchResult::text).toList());
        control.search("self driving", SearchOptions.DEFAULT.withIgnoredSymbols("-").withMode(SearchMode.AND));
        assertEquals(0, medium.results.size());
        control.search("c++", SearchOptions.DEFAULT.withIgnoredSymbols("+"));
        assertEquals("C++ programming guide", medium.results.get(0).text());
    }

    @Test
    void aQueryOfOnlyIgnoredSymbolsFindsNothing() {
        control.search("&-", SearchOptions.DEFAULT.withIgnoredSymbols("&-"));
        assertEquals(0, medium.results.size());
    }

    @Test
    void ignoredSymbolsInTheQueryAreDroppedBeforeMatching() {
        control.search("m-an", SearchOptions.DEFAULT.withIgnoredSymbols("-"));
        assertEquals(3, medium.results.size());
    }

    private MasterControl pagesWithUrls() {
        MasterControl pages = new MasterControl(new TextInputMedium("""
                http://zebra.example.com Sorting algorithms
                http://apple.example.com Sorting data
                http://mango.example.com Sorting lists
                """), medium);
        pages.run();
        return pages;
    }

    private List<String> urls(MasterControl pages, ResultOrder order) {
        pages.search("sorting", SearchOptions.DEFAULT.withOrder(order));
        return medium.results.stream().map(SearchResult::url).toList();
    }

    @Test
    void ordersResultsAlphabetically() {
        // A to Z by the page's text, not by its URL.
        assertEquals(List.of("http://zebra.example.com", "http://apple.example.com", "http://mango.example.com"),
                urls(pagesWithUrls(), ResultOrder.ALPHABETICAL));
        assertEquals(List.of("Sorting algorithms", "Sorting data", "Sorting lists"),
                medium.results.stream().map(SearchResult::text).toList());
    }

    @Test
    void ordersResultsByMostClicks() {
        MasterControl pages = pagesWithUrls();
        pages.recordClick("http://mango.example.com");
        pages.recordClick("http://mango.example.com");
        pages.recordClick("http://zebra.example.com");
        assertEquals(List.of("http://mango.example.com", "http://zebra.example.com", "http://apple.example.com"),
                urls(pages, ResultOrder.CLICKS));
    }

    @Test
    void ordersResultsByHighestBid() {
        MasterControl pages = pagesWithUrls();
        pages.setBid("http://apple.example.com", 10);
        pages.setBid("http://zebra.example.com", 25);
        assertEquals(List.of("http://zebra.example.com", "http://apple.example.com", "http://mango.example.com"),
                urls(pages, ResultOrder.BID));
    }

    @Test
    void everyOrderFindsTheSameUrls() {
        MasterControl pages = pagesWithUrls();
        pages.recordClick("http://mango.example.com");
        pages.setBid("http://apple.example.com", 4);
        java.util.Set<String> expected = java.util.Set.copyOf(urls(pages, ResultOrder.RELEVANCE));
        for (ResultOrder order : ResultOrder.values()) {
            assertEquals(expected, java.util.Set.copyOf(urls(pages, order)), order.name());
        }
        assertEquals(3, expected.size());
    }

    @Test
    void readsOrderNamesInAnyCase() {
        assertEquals(ResultOrder.CLICKS, ResultOrder.parse("Clicks").orElseThrow());
        assertEquals(ResultOrder.BID, ResultOrder.parse(" bid ").orElseThrow());
        assertTrue(ResultOrder.parse("newest").isEmpty());
    }

    @Test
    void correctsATypoWhileSuggesting() {
        assertEquals(List.of("software", "software design", "software systems"), control.suggest("sofware", 8));
        assertEquals(List.of("software", "software design", "software systems"), control.suggest("sotfware", 8));
        assertEquals(List.of("software design", "software design and architecture"), control.suggest("sofware d", 8));
    }

    @Test
    void correctsATypoInAnEarlierWord() {
        assertEquals(List.of("software design", "software design and architecture"), control.suggest("sofware desi", 8));
        assertEquals(List.of("software design and architecture"), control.suggest("software desgin ", 8).stream()
                .filter(s -> s.equals("software design and architecture")).toList());
    }

    @Test
    void correctsAWordStillBeingTyped() {
        // "sofw" is not the start of any index word, but is one typo from the start of "softw...".
        assertEquals(List.of("software", "software design", "software systems"), control.suggest("sofw", 8));
    }

    @Test
    void doesNotCorrectWordsThatAreRightOrCommonOrShort() {
        assertEquals(List.of("software", "software design", "software systems"), control.suggest("soft", 8));
        assertEquals(List.of(), control.suggest("zeb", 8));
        assertEquals(List.of(), control.suggest("xq", 8));
        assertEquals(List.of("man in the middle"), control.suggest("man ", 8));
    }

    @Test
    void countsSwapsAndLettersAsTypos() {
        assertEquals(1, SearchEngine.editDistance("sofware", "software"));
        assertEquals(1, SearchEngine.editDistance("sotfware", "software"));
        assertEquals(2, SearchEngine.editDistance("sftwre", "software"));
        assertEquals(0, SearchEngine.editDistance("man", "man"));
    }
}
