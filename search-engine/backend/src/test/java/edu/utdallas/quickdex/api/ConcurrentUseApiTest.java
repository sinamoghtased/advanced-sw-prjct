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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Deleted pages in later requests, and many users at once (FR023, FR029 and the concurrency feature). */
class ConcurrentUseApiTest {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static EngineServer server;

    @BeforeAll
    static void start() throws Exception {
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

    private static HttpResponse<String> call(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(uri(path)).method(method, publisher)
                .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String enc(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static int total(String query, String settings) throws Exception {
        String body = call("GET", "/search?q=" + enc(query) + settings, null).body();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"total\":(\\d+)").matcher(body);
        assertTrue(m.find(), body);
        return Integer.parseInt(m.group(1));
    }

    @Test
    void aDeletedPageIsGoneFromEveryLaterRequest() throws Exception {
        String keep = "https://keep.example.org/page";
        String drop = "https://drop.example.org/page";
        assertEquals(200, call("POST", "/pages", keep + " Zorblax keeps its place\n" + drop + " Zorblax is deleted soon\n")
                .statusCode());
        call("POST", "/click?url=" + enc(drop), null);
        call("POST", "/click?url=" + enc(drop), null);
        call("POST", "/bid?amount=9&url=" + enc(drop), null);
        assertEquals(2, total("zorblax", "&order=clicks"));

        assertEquals(200, call("DELETE", "/pages?url=" + enc(drop), null).statusCode());

        assertEquals(1, total("zorblax", "&order=clicks"));
        assertEquals(1, total("zorblax", "&order=bid"));
        assertEquals(1, total("zorblax", "&order=alphabetical"));
        assertEquals(404, call("POST", "/click?url=" + enc(drop), null).statusCode());
        assertEquals(404, call("POST", "/bid?amount=1&url=" + enc(drop), null).statusCode());
        assertEquals(1, total("zorblax", "&mode=and"));

        // Adding the page again starts from zero clicks.
        assertEquals(200, call("POST", "/pages", drop + " Zorblax is deleted soon\n").statusCode());
        assertTrue(call("POST", "/click?url=" + enc(drop), null).body().contains("\"clicks\":1"));
    }

    @Test
    void manyUsersCanAddSearchClickAndDeleteAtTheSameTime() throws Exception {
        int users = 8;
        int rounds = 15;
        String pagesBefore = call("GET", "/stats", null).body();
        ExecutorService pool = Executors.newFixedThreadPool(users);
        List<Future<String>> futures = new ArrayList<>();
        for (int user = 0; user < users; user++) {
            int u = user;
            futures.add(pool.submit(() -> {
                for (int round = 0; round < rounds; round++) {
                    String word = "w" + u + "k" + round + "z";
                    String url = "https://user" + u + "round" + round + ".example.org/p";
                    String added = call("POST", "/pages", url + " Wombat " + word + " habitat\n").body();
                    if (!added.startsWith("{\"added\":1,")) {
                        return "add failed: " + added;
                    }
                    if (total(word, "") != 1) {
                        return "search did not find " + word;
                    }
                    // Other users' requests mix in: suggestions and a search with settings.
                    call("GET", "/suggest?q=" + enc("wombat " + word.substring(0, 3)), null);
                    total("wombat", "&order=alphabetical&limit=5");
                    if (call("POST", "/click?url=" + enc(url), null).statusCode() != 200) {
                        return "click failed for " + url;
                    }
                    if (call("DELETE", "/pages?url=" + enc(url), null).statusCode() != 200) {
                        return "delete failed for " + url;
                    }
                    if (total(word, "") != 0) {
                        return "deleted page still found: " + word;
                    }
                }
                return "";
            }));
        }
        pool.shutdown();
        for (Future<String> future : futures) {
            assertEquals("", future.get(120, java.util.concurrent.TimeUnit.SECONDS));
        }
        // Everything added was deleted, so the index is the size it was.
        assertEquals(pagesBefore, call("GET", "/stats", null).body());
    }
}
