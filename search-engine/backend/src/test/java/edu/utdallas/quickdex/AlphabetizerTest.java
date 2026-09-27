package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** FR4.0 Incremental Alphabetical Sorting. */
class AlphabetizerTest {

    /** Runs the pipeline up to the Alphabetizer, one line at a time. */
    private static Alphabetizer alphabetize(String text, Alphabetizer.Order order) {
        LineStorage storage = new LineStorage();
        Input input = new Input(new TextInputMedium(text), storage);
        CircularShift shifts = new CircularShift(storage);
        Alphabetizer alphabetizer = new Alphabetizer(shifts, order);
        for (int line = input.setInput(); line != Input.END_OF_INPUT; line = input.setInput()) {
            if (line != Input.SKIPPED) {
                shifts.setLines(line);
                alphabetizer.setShifts();
            }
        }
        return alphabetizer;
    }

    private static List<String> index(Alphabetizer alphabetizer) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < alphabetizer.getShiftCount(); i++) {
            texts.add(alphabetizer.getAlphabetizedShift(i).text());
        }
        return texts;
    }

    @Test
    void sortsTheShiftsOfALine() {
        Alphabetizer a = alphabetize("Descent of Man", Alphabetizer.Order.CASE_INSENSITIVE);
        assertEquals(List.of("Descent of Man", "Man Descent of", "of Man Descent"), index(a));
        assertEquals("Descent", a.getAlphabetizedShift(0).keyword());
        assertEquals("of Man", a.getAlphabetizedShift(0).context());
    }

    @Test
    void mergesEachLineIntoTheShiftsAlreadySorted() {
        Alphabetizer a = alphabetize("Descent of Man\nThe Ascent of Man", Alphabetizer.Order.CASE_INSENSITIVE);
        assertEquals(List.of(
                "Ascent of Man The",
                "Descent of Man",
                "Man Descent of",
                "Man The Ascent of",
                "of Man Descent",
                "of Man The Ascent",
                "The Ascent of Man"), index(a));
    }

    @Test
    void ignoresCaseByDefaultAndKeepsGenerationOrderForTies() {
        Alphabetizer a = alphabetize("banana\nApple\napple\nCherry", Alphabetizer.Order.CASE_INSENSITIVE);
        assertEquals(List.of("Apple", "apple", "banana", "Cherry"), index(a));
    }

    @Test
    void canSortCaseSensitively() {
        Alphabetizer a = alphabetize("banana\napple\nCherry", Alphabetizer.Order.CASE_SENSITIVE);
        assertEquals(List.of("Cherry", "apple", "banana"), index(a));
    }

    @Test
    void comparesWordByWord() {
        Alphabetizer a = alphabetize("a bc\nab c\na", Alphabetizer.Order.CASE_INSENSITIVE);
        assertEquals("a", a.getAlphabetizedShift(0).text());
        assertEquals("a bc", a.getAlphabetizedShift(1).text());
        assertEquals("ab c", a.getAlphabetizedShift(2).text());
    }

    @Test
    void reportsWhereTheNewShiftsLanded() {
        Alphabetizer a = alphabetize("m n", Alphabetizer.Order.CASE_INSENSITIVE);
        assertArrayEquals(new int[] {0, 1}, a.getNewShiftPositions());
    }

    @Test
    void incrementalMergingMatchesSortingEverythingAtOnce() {
        Random random = new Random(6362);
        String[] vocabulary = {"alpha", "Beta", "gamma", "delta", "Alpha", "beta", "é", "z", "Zeta", "a"};
        StringBuilder text = new StringBuilder();
        List<String[]> lines = new ArrayList<>();
        for (int l = 0; l < 300; l++) {
            String[] words = new String[1 + random.nextInt(8)];
            for (int w = 0; w < words.length; w++) {
                words[w] = vocabulary[random.nextInt(vocabulary.length)];
            }
            lines.add(words);
            text.append(String.join(" ", words)).append('\n');
        }

        // Expected: all shifts, generated in order, then stably sorted in one go.
        List<String[]> all = new ArrayList<>();
        for (String[] words : lines) {
            for (int offset = 0; offset < words.length; offset++) {
                String[] shift = new String[words.length];
                for (int w = 0; w < words.length; w++) {
                    shift[w] = words[(offset + w) % words.length];
                }
                all.add(shift);
            }
        }
        Comparator<String[]> byWords = (x, y) -> {
            for (int w = 0; w < Math.min(x.length, y.length); w++) {
                int c = compareCodePoints(x[w].toLowerCase(Locale.ROOT), y[w].toLowerCase(Locale.ROOT));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(x.length, y.length);
        };
        List<String> expected = all.stream().sorted(byWords).map(s -> String.join(" ", s)).toList();

        assertEquals(expected, index(alphabetize(text.toString(), Alphabetizer.Order.CASE_INSENSITIVE)));
    }

    private static int compareCodePoints(String a, String b) {
        int[] x = a.codePoints().toArray();
        int[] y = b.codePoints().toArray();
        return java.util.Arrays.compare(x, y);
    }
}
