package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** FR9.0 display (output after every line), FR11.0 Export, NFR11.0 Security. */
class OutputTest {

    private static MasterControl run(String text, RecordingMedium medium) {
        MasterControl control = new MasterControl(new TextInputMedium(text), medium);
        control.run();
        return control;
    }

    @Test
    void showsAnUpdateAfterEveryLineThatRebuildsTheIndex() {
        RecordingMedium medium = new RecordingMedium();
        MasterControl control = run("Descent of Man\n\nThe Ascent of Man\nKey Word in Context", medium);

        assertEquals(3, medium.updates.size());
        assertEquals(List.of(1, 2, 3), medium.updates.stream().map(RecordingMedium.Update::lineNumber).toList());
        assertEquals(List.of(3, 7, 11), medium.updates.stream().map(RecordingMedium.Update::indexSize).toList());
        // Inserting each update's entries at their positions gives exactly the final index.
        assertEquals(RecordingMedium.texts(control.index()), RecordingMedium.texts(medium.shown));
    }

    @Test
    void doesNotShowTheSameEntriesTwice() {
        RecordingMedium medium = new RecordingMedium();
        MasterControl control = run("one two", medium);
        control.index();
        control.search("one");
        assertEquals(1, medium.updates.size());
    }

    @Test
    void exportsPlainText() {
        MasterControl control = run("https://a.example Descent of Man", new RecordingMedium());
        assertEquals("""
                Descent\tof Man\t1\thttps://a.example
                Man\tDescent of\t1\thttps://a.example
                of\tMan Descent\t1\thttps://a.example
                """, control.export(Output.Format.TEXT));
    }

    @Test
    void exportsCsvWithQuotedCells() {
        MasterControl control = run("say \"hi\", there", new RecordingMedium());
        String csv = control.export(Output.Format.CSV);
        assertTrue(csv.startsWith("keyword,context,line,url\r\n"), csv);
        assertTrue(csv.contains("\"say\",\"\"\"hi\"\", there\",1,\"\"\r\n"), csv);
    }

    @Test
    void stopsCsvCellsFromRunningAsFormulas() {
        assertEquals("\"'=SUM(A1)\"", Output.csvCell("=SUM(A1)"));
        assertEquals("\"'+1\"", Output.csvCell("+1"));
        assertEquals("\"'-1\"", Output.csvCell("-1"));
        assertEquals("\"'@cmd\"", Output.csvCell("@cmd"));
        assertEquals("\"plain\"", Output.csvCell("plain"));
    }
}
