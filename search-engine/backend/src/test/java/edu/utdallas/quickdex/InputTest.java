package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR1.0 Line Input and FR7.0 Input Validation. */
class InputTest {

    private static List<String> wordsOf(LineStorage storage, int line) {
        List<String> words = new ArrayList<>();
        for (int w = 0; w < storage.getWordCount(line); w++) {
            StringBuilder word = new StringBuilder();
            for (int c = 0; storage.getCharacter(line, w, c) != LineStorage.END; c++) {
                word.appendCodePoint(storage.getCharacter(line, w, c));
            }
            words.add(word.toString());
        }
        return words;
    }

    @Test
    void readsLinesInOrderIgnoringBlankLinesAndExtraSpaces() {
        LineStorage storage = new LineStorage();
        Input input = new Input(new TextInputMedium("  Descent   of\tMan \n\n   \nThe Ascent of Man"), storage);

        assertEquals(0, input.setInput());
        assertEquals(Input.SKIPPED, input.setInput());
        assertEquals(Input.SKIPPED, input.setInput());
        assertEquals(1, input.setInput());
        assertEquals(Input.END_OF_INPUT, input.setInput());

        assertEquals(List.of("Descent", "of", "Man"), wordsOf(storage, 0));
        assertEquals(List.of("The", "Ascent", "of", "Man"), wordsOf(storage, 1));
    }

    @Test
    void keepsWordsExactlyAsEntered() {
        LineStorage storage = new LineStorage();
        new Input(new TextInputMedium("KWIC, café's naïve-Test 🚀"), storage).setInput();
        assertEquals(List.of("KWIC,", "café's", "naïve-Test", "🚀"), wordsOf(storage, 0));
    }

    @Test
    void takesAUrlFromTheStartOrEndOfALine() {
        LineStorage storage = new LineStorage();
        Input input = new Input(new TextInputMedium(
                "https://a.example/x Descent of Man\nThe Ascent of Man http://b.example\nno url here"), storage);
        input.setInput();
        input.setInput();
        input.setInput();

        assertEquals("https://a.example/x", storage.getUrl(0));
        assertEquals(List.of("Descent", "of", "Man"), wordsOf(storage, 0));
        assertEquals("http://b.example", storage.getUrl(1));
        assertEquals(List.of("The", "Ascent", "of", "Man"), wordsOf(storage, 1));
        assertNull(storage.getUrl(2));
    }

    @Test
    void skipsALineThatIsOnlyAUrl() {
        Input input = new Input(new TextInputMedium("https://a.example"), new LineStorage());
        assertEquals(Input.SKIPPED, input.setInput());
    }

    @Test
    void acceptsInputWithinTheLimits() {
        assertTrue(Input.validate("Descent of Man").isEmpty());
        assertTrue(Input.validate("a".repeat(Input.MAX_CHARACTERS)).isEmpty());
    }

    @Test
    void rejectsEmptyInput() {
        assertTrue(Input.validate("").orElseThrow().contains("empty"));
        assertTrue(Input.validate(" \n\t\n").orElseThrow().contains("empty"));
    }

    @Test
    void rejectsMoreThanTenThousandCharacters() {
        String message = Input.validate("a".repeat(Input.MAX_CHARACTERS + 1)).orElseThrow();
        assertTrue(message.contains("10,001 characters"), message);
    }

    @Test
    void rejectsInputWithOnlyUrls() {
        assertTrue(Input.validate("https://a.example\nhttp://b.example").orElseThrow().contains("only URLs"));
    }
}
