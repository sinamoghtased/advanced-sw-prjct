package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR2.0 Circular Shifting. */
class CircularShiftTest {

    private static String shiftText(CircularShift shifts, int shift) {
        List<String> words = new ArrayList<>();
        for (int w = 0; w < shifts.getShiftedWordCount(shift); w++) {
            StringBuilder word = new StringBuilder();
            for (int c = 0; shifts.getShiftedCharacter(shift, w, c) != CircularShift.END; c++) {
                word.appendCodePoint(shifts.getShiftedCharacter(shift, w, c));
            }
            words.add(word.toString());
        }
        return String.join(" ", words);
    }

    private static CircularShift shiftsOf(String text) {
        LineStorage storage = new LineStorage();
        Input input = new Input(new TextInputMedium(text), storage);
        CircularShift shifts = new CircularShift(storage);
        for (int line = input.setInput(); line != Input.END_OF_INPUT; line = input.setInput()) {
            if (line != Input.SKIPPED) {
                shifts.setLines(line);
            }
        }
        return shifts;
    }

    @Test
    void aLineOfNWordsYieldsNShiftsStartingWithTheOriginal() {
        CircularShift shifts = shiftsOf("Descent of Man");
        assertEquals(3, shifts.getShiftCount());
        assertEquals("Descent of Man", shiftText(shifts, 0));
        assertEquals("of Man Descent", shiftText(shifts, 1));
        assertEquals("Man Descent of", shiftText(shifts, 2));
    }

    @Test
    void numbersShiftsAcrossLinesAndRemembersTheirLine() {
        CircularShift shifts = shiftsOf("https://a.example one two\n\nthree");
        assertEquals(3, shifts.getShiftCount());
        assertEquals("three", shiftText(shifts, 2));
        assertEquals(0, shifts.getShiftedLine(1));
        assertEquals(1, shifts.getShiftedLine(2));
        assertEquals("https://a.example", shifts.getShiftedUrl(0));
        assertEquals(null, shifts.getShiftedUrl(2));
    }

    @Test
    void keepsRepeatedWordsAsSeparateShifts() {
        CircularShift shifts = shiftsOf("a rose is a rose");
        assertEquals(5, shifts.getShiftCount());
        assertEquals("a rose a rose is", shiftText(shifts, 3));
    }
}
