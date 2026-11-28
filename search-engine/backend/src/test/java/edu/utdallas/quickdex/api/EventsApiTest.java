package edu.utdallas.quickdex.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** GET /events pushes engine changes to open pages. */
class EventsApiTest {

    private static final String PAGE = "https://en.wikipedia.org/wiki/Merge_sort";

    private static EngineServer server;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        server = EngineServer.start(0);
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    private static URI uri(String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }

    /** Opens the stream and returns once the server has accepted it. */
    private static HttpResponse<Stream<String>> open() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/events")).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofLines()).get(5, TimeUnit.SECONDS);
    }

    private static String firstEvent(HttpResponse<Stream<String>> stream) throws Exception {
        return CompletableFuture.supplyAsync(() -> stream.body().filter(l -> l.startsWith("data: ")).findFirst().orElse(""))
                .get(5, TimeUnit.SECONDS);
    }

    private static void post(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String encoded(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    @Test
    void sendsAnEventStreamWithTheRightType() throws Exception {
        HttpResponse<Stream<String>> stream = open();
        assertEquals(200, stream.statusCode());
        assertTrue(stream.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"));
        stream.body().close();
    }

    @Test
    void tellsAnOpenPageAboutAClick() throws Exception {
        HttpResponse<Stream<String>> stream = open();
        post("/click?url=" + encoded(PAGE));
        assertEquals("data: {\"kind\":\"CLICKED\",\"url\":\"" + PAGE + "\"}", firstEvent(stream));
        stream.body().close();
    }

    @Test
    void tellsEveryOpenPageAboutABid() throws Exception {
        HttpResponse<Stream<String>> first = open();
        HttpResponse<Stream<String>> second = open();
        post("/bid?amount=7&url=" + encoded(PAGE));
        String expected = "data: {\"kind\":\"BID_CHANGED\",\"url\":\"" + PAGE + "\"}";
        assertEquals(expected, firstEvent(first));
        assertEquals(expected, firstEvent(second));
        first.body().close();
        second.body().close();
    }

    @Test
    void onlyAcceptsGet() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/events")).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(405, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
