package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR5.0 Generation Logging, the SRS example, and NFR3.0 / NFR4.0. */
class MasterControlTest {

    private final Logger log = Logger.getLogger("quickdex");
    private final StringBuilder logged = new StringBuilder();
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logged.append(new java.util.logging.SimpleFormatter().formatMessage(record)).append('\n');
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void captureLog() {
        log.setUseParentHandlers(false);
        log.addHandler(capture);
        log.setLevel(Level.INFO);
    }

    @AfterEach
    void releaseLog() {
        log.removeHandler(capture);
        log.setUseParentHandlers(true);
    }

    @Test
    void buildsTheSrsExample() {
        MasterControl control = new MasterControl(new TextInputMedium("Descent of Man"), new RecordingMedium());
        control.run();
        List<KwicEntry> index = control.index();
        assertEquals(List.of("Descent", "Man", "of"), index.stream().map(KwicEntry::keyword).toList());
        assertEquals(List.of("of Man", "Descent of", "Man Descent"), index.stream().map(KwicEntry::context).toList());
    }

    @Test
    void logsEachLineAndTheFinalIndex() {
        MasterControl.RunSummary summary = new MasterControl(
                new TextInputMedium("Descent of Man\n\nThe Ascent of Man"), new RecordingMedium()).run();

        assertEquals(2, summary.linesIndexed());
        assertEquals(1, summary.linesSkipped());
        assertEquals(7, summary.shifts());
        String text = logged.toString();
        assertTrue(text.contains("input line 1 (line 1): 3 shifts [\"Descent of Man\", \"of Man Descent\", "
                + "\"Man Descent of\"]; index size 3;"), text);
        assertTrue(text.contains("input line 2: skipped (blank)"), text);
        assertTrue(text.contains("input line 3 (line 2): 4 shifts"), text);
        assertTrue(text.contains("done: 2 lines indexed, 1 skipped, 7 shifts"), text);
        assertTrue(text.contains("1. Ascent | of Man The | line 2"), text);
    }

    @Test
    void indexesOneThousandLinesOfTenWordsInUnderTwoSeconds() {
        log.setLevel(Level.WARNING);
        String[] vocabulary = ("software architecture keyword context index search engine shift line word "
                + "alpha beta gamma delta abstract data type module interface design").split(" ");
        java.util.Random random = new java.util.Random(4);
        StringBuilder text = new StringBuilder();
        for (int l = 0; l < 1_000; l++) {
            for (int w = 0; w < 10; w++) {
                text.append(vocabulary[random.nextInt(vocabulary.length)]).append(' ');
            }
            text.append('\n');
        }
        long start = System.nanoTime();
        MasterControl.RunSummary summary = new MasterControl(
                new TextInputMedium(text.toString()), new RecordingMedium()).run();
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(10_000, summary.shifts());
        assertTrue(millis < 2_000, "took " + millis + " ms");
    }
}
