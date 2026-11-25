package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** FR017 Plain-text and CSV export through the command-line interface. */
class CommandLineTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureOutput() {
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    private String run(Path dir, String text, String... options) throws Exception {
        Path file = dir.resolve("pages.txt");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        String[] args = new String[options.length + 1];
        args[0] = file.toString();
        System.arraycopy(options, 0, args, 1, options.length);
        MasterControl.main(args);
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printsThePlainTextIndexByDefault(@TempDir Path dir) throws Exception {
        String out = run(dir, "http://a.example.com Descent of Man\nThe Ascent of Man\n");
        assertEquals("""
                Ascent\tof Man The\t2\t
                Descent\tof Man\t1\thttp://a.example.com
                Man\tDescent of\t1\thttp://a.example.com
                Man\tThe Ascent of\t2\t
                """, out);
    }

    @Test
    void printsTheIndexAsCsv(@TempDir Path dir) throws Exception {
        String out = run(dir, "http://a.example.com Descent of Man\n", "--csv");
        assertTrue(out.startsWith("keyword,context,line,url\r\n"), out);
        assertTrue(out.contains("\"Descent\",\"of Man\",1,\"http://a.example.com\"\r\n"), out);
        assertTrue(!out.contains("\"of\","), "noise-word shifts are not in the index: " + out);
    }

    @Test
    void printsSearchResultsInsteadOfTheIndex(@TempDir Path dir) throws Exception {
        String out = run(dir, "http://a.example.com Descent of Man\nKey Word in Context\n", "--search", "man");
        assertTrue(out.startsWith("1 results for \"man\""), out);
        assertTrue(out.contains("Descent of Man\thttp://a.example.com"), out);
        assertTrue(!out.contains("Context"), out);
    }
}
