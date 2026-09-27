package edu.utdallas.quickdex.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The web page's view of the engine: streaming updates, search, export, and errors. */
class EngineServerTest {

    private static EngineServer server;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws IOException {
        Logger.getLogger("quickdex").setLevel(Level.WARNING);
        server = EngineServer.start(0);
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    private static URI uri(String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }

    private static HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri("/index"))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String indexId(String ndjson) {
        Matcher m = Pattern.compile("\"index\":\"([^\"]+)\"").matcher(ndjson);
        assertTrue(m.find(), ndjson);
        return m.group(1);
    }

    @Test
    void streamsAnUpdateForEveryLineThenFinishes() throws Exception {
        HttpResponse<String> response = post("https://a.example Descent of Man\n\nThe Ascent of Man\n");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/x-ndjson"));

        List<String> events = response.body().lines().toList();
        assertEquals(4, events.size(), response.body());
        assertTrue(events.get(0).startsWith("{\"type\":\"start\""), events.get(0));
        assertTrue(events.get(0).contains("\"lines\":3"), events.get(0));
        assertTrue(events.get(1).startsWith("{\"type\":\"update\",\"line\":1,\"size\":3,"), events.get(1));
        assertTrue(events.get(1).contains("\"keyword\":\"Descent\",\"context\":\"of Man\",\"url\":\"https://a.example\""),
                events.get(1));
        assertTrue(events.get(2).startsWith("{\"type\":\"update\",\"line\":2,\"size\":7,"), events.get(2));
        assertTrue(events.get(3).startsWith("{\"type\":\"done\",\"linesIndexed\":2,\"linesSkipped\":1,\"shifts\":7"),
                events.get(3));
    }

    @Test
    void searchesAFinishedIndex() throws Exception {
        String id = indexId(post("Descent of Man\nThe Ascent of Man").body());
        HttpResponse<String> response = get("/search?index=" + id + "&q=" + URLEncoder.encode("MA", StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("{\"query\":\"MA\",\"total\":2,\"offset\":0,"), body);
        assertTrue(body.contains("\"pages\":2,\"entries\":7,\"results\":[{\"line\":1,\"url\":null,"
                + "\"text\":\"Descent of Man\",\"phrase\":true,\"missing\":[],\"matches\":[{\"pos\":2,"), body);
        assertTrue(body.contains("\"keyword\":\"Man\",\"context\":\"Descent of\""), body);
    }

    @Test
    void searchesTheBuiltInWebIndexByDefault() throws Exception {
        HttpResponse<String> stats = get("/stats");
        assertEquals(200, stats.statusCode());
        assertTrue(stats.body().matches("\\{\"pages\":\\d{3},\"entries\":\\d{4}\\}"), stats.body());

        String body = get("/search?q=" + URLEncoder.encode("merge sort", StandardCharsets.UTF_8)).body();
        assertTrue(body.contains("\"url\":\"https://en.wikipedia.org/wiki/Merge_sort\""), body);
        assertTrue(body.contains("\"phrase\":true"), body);
    }

    @Test
    void suggestsSearches() throws Exception {
        String body = get("/suggest?q=merge").body();
        assertTrue(body.startsWith("{\"query\":\"merge\",\"suggestions\":[\"merge sort"), body);
    }

    @Test
    void addsNewWebPagesAndSkipsKnownOnes() throws Exception {
        String pages = "https://en.wikipedia.org/wiki/Merge_sort Merge sort again\n"
                + "https://example.org/quokka Quokka: a small wallaby from Western Australia\n";
        HttpResponse<String> added = client.send(HttpRequest.newBuilder(uri("/pages"))
                .POST(HttpRequest.BodyPublishers.ofString(pages, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, added.statusCode());
        assertTrue(added.body().startsWith("{\"added\":1,"), added.body());

        String body = get("/search?q=quokka").body();
        assertTrue(body.contains("\"url\":\"https://example.org/quokka\""), body);
        assertTrue(body.contains("\"total\":1,"), body);
    }

    @Test
    void pagesThroughResults() throws Exception {
        String all = get("/search?q=software&limit=1000").body();
        String second = get("/search?q=software&offset=1&limit=1").body();
        Matcher total = Pattern.compile("\"total\":(\\d+)").matcher(all);
        assertTrue(total.find(), all);
        assertTrue(Integer.parseInt(total.group(1)) > 10, all);
        assertTrue(second.contains("\"offset\":1,"), second);
        assertEquals(1, second.split("\"text\":").length - 1, second);
    }

    @Test
    void exportsAFinishedIndex() throws Exception {
        String id = indexId(post("=cmd one").body());
        HttpResponse<String> csv = get("/export?index=" + id + "&format=csv");
        assertEquals(200, csv.statusCode());
        assertEquals("attachment; filename=\"quickdex-index.csv\"", csv.headers().firstValue("Content-Disposition").orElse(""));
        assertTrue(csv.body().contains("\"'=cmd\",\"one\",1,\"\""), csv.body());

        HttpResponse<String> txt = get("/export?index=" + id + "&format=txt");
        assertEquals("=cmd\tone\t1\t\none\t=cmd\t1\t\n", txt.body());
    }

    @Test
    void rejectsInvalidInputWithAMessage() throws Exception {
        HttpResponse<String> response = post("   ");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("The input is empty"), response.body());
    }

    @Test
    void rejectsInputOverOneMegabyte() throws Exception {
        HttpResponse<String> response = post("a".repeat(1_000_001));
        assertEquals(413, response.statusCode());
    }

    @Test
    void reportsAnUnknownIndex() throws Exception {
        HttpResponse<String> response = get("/search?index=nope&q=a");
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("expired"), response.body());
    }

    @Test
    void escapesUserTextInJson() throws Exception {
        String body = post("<script>\"x\"\\</script>").body();
        assertTrue(body.contains("\"keyword\":\"<script>\\\"x\\\"\\\\</script>\""), body);
    }
}
