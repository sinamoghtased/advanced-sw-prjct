package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** FR018 Noise-word filtering. */
class NoiseFilterTest {

    private final NoiseFilter filter = NoiseFilter.standard();

    @Test
    void treatsTheWordsFromTheSpecAsNoise() {
        assertTrue(filter.isNoise("a"));
        assertTrue(filter.isNoise("the"));
        assertTrue(filter.isNoise("of"));
        assertFalse(filter.isNoise("Descent"));
    }

    @Test
    void ignoresCaseAndSurroundingPunctuation() {
        assertTrue(filter.isNoise("The"));
        assertTrue(filter.isNoise("OF,"));
        assertTrue(filter.isNoise("(a)"));
        assertFalse(filter.isNoise("theory"));
    }

    @Test
    void noneKeepsEveryShift() {
        assertTrue(NoiseFilter.NONE.keeps("the"));
    }

    @Test
    void dropsNoiseShiftsFromTheIndexButKeepsTheLineOriginal() {
        RecordingMedium medium = new RecordingMedium();
        MasterControl control = new MasterControl(new TextInputMedium("The Ascent of Man"), medium);
        control.run();

        assertEquals(List.of("Ascent of Man The", "Man The Ascent of"), RecordingMedium.texts(control.index()));
        RecordingMedium.Update update = medium.updates.get(0);
        assertEquals(4, update.generated().size());
        assertEquals(List.of("The Ascent of Man", "Ascent of Man The", "of Man The Ascent", "Man The Ascent of"),
                RecordingMedium.texts(update.generated()));
        assertEquals(List.of("Ascent of Man The", "Man The Ascent of"), RecordingMedium.texts(update.filtered()));
    }

    @Test
    void stillReportsALineMadeOnlyOfNoiseWords() {
        RecordingMedium medium = new RecordingMedium();
        MasterControl control = new MasterControl(new TextInputMedium("the of a\nKey Word"), medium);
        control.run();

        assertEquals(2, medium.updates.size());
        assertEquals(List.of(), medium.updates.get(0).inserted());
        assertEquals(List.of(), medium.updates.get(0).filtered());
        assertEquals(3, medium.updates.get(0).generated().size());
        assertEquals(List.of("Key Word", "Word Key"), RecordingMedium.texts(control.index()));
    }
}
