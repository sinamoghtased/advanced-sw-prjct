package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LineStorageTest {

    private static void store(LineStorage storage, int line, String... words) {
        for (int w = 0; w < words.length; w++) {
            int[] cps = words[w].codePoints().toArray();
            for (int c = 0; c < cps.length; c++) {
                storage.setCharacter(line, w, c, cps[c]);
            }
        }
    }

    @Test
    void storesCharactersWordsAndUrls() {
        LineStorage storage = new LineStorage();
        store(storage, 0, "Descent", "of", "Man");
        storage.setUrl(0, "https://example.com");

        assertEquals(1, storage.getLineCount());
        assertEquals(3, storage.getWordCount(0));
        assertEquals('o', storage.getCharacter(0, 1, 0));
        assertEquals('f', storage.getCharacter(0, 1, 1));
        assertEquals(LineStorage.END, storage.getCharacter(0, 1, 2));
        assertEquals("https://example.com", storage.getUrl(0));
    }

    @Test
    void keepsUnicodeCodePoints() {
        LineStorage storage = new LineStorage();
        store(storage, 0, "naïve", "𝄞clef");
        assertEquals('ï', storage.getCharacter(0, 0, 2));
        assertEquals("𝄞".codePointAt(0), storage.getCharacter(0, 1, 0));
        assertEquals('c', storage.getCharacter(0, 1, 1));
        assertNull(storage.getUrl(0));
    }

    @Test
    void rejectsPositionsThatSkipAhead() {
        LineStorage storage = new LineStorage();
        assertThrows(IndexOutOfBoundsException.class, () -> storage.setCharacter(1, 0, 0, 'a'));
        storage.setCharacter(0, 0, 0, 'a');
        assertThrows(IllegalArgumentException.class, () -> storage.setCharacter(0, 0, 2, 'b'));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.setCharacter(0, 2, 0, 'b'));
    }
}
