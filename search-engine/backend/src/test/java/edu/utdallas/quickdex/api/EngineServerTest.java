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
        assertTrue(events.get(1).startsWith("{\"type\":\"update\",\"line\":1,\"size\":2,"), events.get(1));
        assertTrue(events.get(1).contains("\"keyword\":\"Descent\",\"context\":\"of Man\",\"url\":\"https://a.example\""),
                events.get(1));
        // The filtered shifts go from shift 0 straight to shift 2: "of Man Descent" is left out.
        assertTrue(events.get(1).contains("\"filtered\":[{\"id\":0,\"line\":1,\"keyword\":\"Descent\","
                + "\"context\":\"of Man\",\"url\":\"https://a.example\"},{\"id\":2,"), events.get(1));
        assertTrue(events.get(2).startsWith("{\"type\":\"update\",\"line\":2,\"size\":4,"), events.get(2));
        assertTrue(events.get(3).startsWith("{\"type\":\"done\",\"linesIndexed\":2,\"linesSkipped\":1,\"shifts\":4"),
                events.get(3));
    }

    @Test
    void searchesAFinishedIndex() throws Exception {
        String id = indexId(post("Descent of Man\nThe Ascent of Man").body());
        HttpResponse<String> response = get("/search?index=" + id + "&q=" + URLEncoder.encode("MA", StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("{\"query\":\"MA\",\"total\":2,\"offset\":0,"), body);
        assertTrue(body.contains("\"pages\":2,\"entries\":4,\"results\":[{\"line\":1,\"url\":null,"
                + "\"text\":\"Descent of Man\",\"phrase\":true,\"missing\":[],\"matches\":[{\"pos\":2,"), body);
        assertTrue(body.contains("\"keyword\":\"Man\",\"context\":\"Descent of\""), body);
    }

    @Test
    void searchesByOrAndNotModes() throws Exception {
        String id = indexId(post("Descent of Man\nThe Ascent of Man\nMan in the Middle").body());
        String base = "/search?index=" + id + "&q=";
        String words = URLEncoder.encode("descent ascent", StandardCharsets.UTF_8);
        assertTrue(get(base + words).body().contains("\"total\":2,"), "OR is the default");
        assertTrue(get(base + words + "&mode=or").body().contains("\"total\":2,"));
        assertTrue(get(base + words + "&mode=AND").body().contains("\"total\":0,"));
        String notWords = URLEncoder.encode("man descent", StandardCharsets.UTF_8);
        String not = get(base + notWords + "&mode=not").body();
        assertTrue(not.contains("\"total\":2,"), not);
        assertTrue(!not.contains("Descent of Man"), not);
    }

    @Test
    void searchesCaseSensitivelyWhenAsked() throws Exception {
        String id = indexId(post("Java the language\njava coffee beans").body());
        String base = "/search?index=" + id + "&q=";
        assertTrue(get(base + "java").body().contains("\"total\":2,"));
        assertTrue(get(base + "java&case=true").body().contains("\"total\":1,"));
        assertTrue(get(base + "Java&case=true").body().contains("\"text\":\"Java the language\""));
    }

    @Test
    void ignoresTheSymbolsTheUserListsWhenSearching() throws Exception {
        String id = indexId(post("Self-driving cars\nDriver assistance").body());
        String base = "/search?index=" + id + "&q=selfdriving";
        assertTrue(get(base).body().contains("\"total\":0,"));
        assertTrue(get(base + "&symbols=-").body().contains("\"total\":1,"));
    }

    private static HttpResponse<String> postTo(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void ordersResultsByClicksAndBids() throws Exception {
        String pages = "https://alpha.example.org/p Quolls are small marsupials\n"
                + "https://beta.example.org/p Quolls live in forests\n"
                + "https://gamma.example.org/p Quolls hunt at night\n";
        assertEquals(200, postTo("/pages?bid=0", pages).statusCode());
        String base = "/search?q=quolls&order=";
        assertTrue(get(base + "alphabetical").body().indexOf("alpha.example") < get(base + "alphabetical").body().indexOf("beta.example"));

        assertEquals(200, postTo("/click?url=" + URLEncoder.encode("https://gamma.example.org/p", StandardCharsets.UTF_8), "").statusCode());
        HttpResponse<String> clicked = postTo("/click?url=" + URLEncoder.encode("https://gamma.example.org/p", StandardCharsets.UTF_8), "");
        assertTrue(clicked.body().contains("\"clicks\":2"), clicked.body());
        String byClicks = get(base + "clicks").body();
        assertTrue(byClicks.indexOf("gamma.example") < byClicks.indexOf("alpha.example"), byClicks);

        HttpResponse<String> bid = postTo("/bid?amount=9&url=" + URLEncoder.encode("https://beta.example.org/p", StandardCharsets.UTF_8), "");
        assertTrue(bid.body().contains("\"bid\":9"), bid.body());
        String byBid = get(base + "bid").body();
        assertTrue(byBid.indexOf("beta.example") < byBid.indexOf("alpha.example"), byBid);
        assertTrue(byBid.contains("\"total\":3,"), byBid);
    }

    @Test
    void rejectsBadClicksBidsAndOrders() throws Exception {
        assertEquals(400, get("/search?q=sort&order=newest").statusCode());
        assertEquals(405, get("/click?url=x").statusCode());
        assertEquals(400, postTo("/click", "").statusCode());
        assertEquals(404, postTo("/click?url=" + URLEncoder.encode("https://nobody.example.org", StandardCharsets.UTF_8), "").statusCode());
        assertEquals(400, postTo("/bid?amount=-5&url=" + URLEncoder.encode("https://en.wikipedia.org/wiki/Merge_sort", StandardCharsets.UTF_8), "").statusCode());
        assertEquals(400, postTo("/pages?bid=-1", "https://x.example.org X page").statusCode());
    }

    private static HttpResponse<String> delete(String path, String adminKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(path.startsWith("http") ? URI.create(path) : uri(path))
                .DELETE();
        if (adminKey != null) {
            request.header("X-Admin-Key", adminKey);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void deletesAnOutOfDatePageAndItsShifts() throws Exception {
        String url = "https://dodo.example.org/bird";
        assertEquals(200, postTo("/pages", url + " Dodo: an extinct flightless bird\n").statusCode());
        assertTrue(get("/search?q=dodo").body().contains("\"total\":1,"));
        String before = get("/stats").body();

        HttpResponse<String> deleted = delete("/pages?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8), null);
        assertEquals(200, deleted.statusCode());
        assertTrue(deleted.body().startsWith("{\"deleted\":1,"), deleted.body());
        assertTrue(get("/search?q=dodo").body().contains("\"total\":0,"));
        assertTrue(!get("/stats").body().equals(before));

        assertEquals(404, delete("/pages?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8), null).statusCode());
        assertEquals(400, delete("/pages", null).statusCode());
        // The same URL can be added again after it was deleted.
        assertTrue(postTo("/pages", url + " Dodo is back\n").body().startsWith("{\"added\":1,"));
    }

    @Test
    void adminActionsNeedTheKeyWhenTheServerHasOne() throws Exception {
        EngineServer locked = EngineServer.start(0, "s3cret");
        try {
            String base = "http://localhost:" + locked.port();
            String url = URLEncoder.encode("https://en.wikipedia.org/wiki/Merge_sort", StandardCharsets.UTF_8);
            assertEquals(401, delete(base + "/pages?url=" + url, null).statusCode());
            assertEquals(401, delete(base + "/pages?url=" + url, "wrong").statusCode());
            HttpResponse<String> badBid = client.send(HttpRequest.newBuilder(URI.create(base + "/bid?amount=3&url=" + url))
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, badBid.statusCode());
            HttpResponse<String> goodBid = client.send(HttpRequest.newBuilder(URI.create(base + "/bid?amount=3&url=" + url))
                    .header("X-Admin-Key", "s3cret").POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, goodBid.statusCode());
            assertEquals(200, delete(base + "/pages?url=" + url, "s3cret").statusCode());
            // Searching and clicking never need the key.
            assertTrue(client.send(HttpRequest.newBuilder(URI.create(base + "/search?q=merge")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body().contains("\"total\":"));
        } finally {
            locked.stop();
        }
    }

    @Test
    void rejectsAnUnknownSearchMode() throws Exception {
        HttpResponse<String> response = get("/search?q=sort&mode=xor");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("OR, AND or NOT"), response.body());
    }

    @Test
    void searchesTheBuiltInWebIndexByDefault() throws Exception {
        HttpResponse<String> stats = get("/stats");
        assertEquals(200, stats.statusCode());
        assertTrue(stats.body().matches("\\{\"pages\":\\d{3},\"entries\":\\d{3,4}\\}"), stats.body());

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
    void rejectsPagesWithoutAValidUrlOrDescription() throws Exception {
        String pages = "https://example.gov Gnu habitat\n"
                + "https://example.org/nodescription\n"
                + "no url on this line\n"
                + "https://example.net/okapi Okapi: a forest giraffe\n";
        HttpResponse<String> added = client.send(HttpRequest.newBuilder(uri("/pages"))
                .POST(HttpRequest.BodyPublishers.ofString(pages, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, added.statusCode());
        assertTrue(added.body().startsWith("{\"added\":1,"), added.body());
        assertTrue(added.body().endsWith("\"rejected\":3}"), added.body());
        assertTrue(get("/search?q=gnu").body().contains("\"total\":0,"));
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
