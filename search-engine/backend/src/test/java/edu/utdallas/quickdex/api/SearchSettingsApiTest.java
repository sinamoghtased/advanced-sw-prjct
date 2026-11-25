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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * NFR014 acceptance check: run the same query before and after changing each search setting,
 * and check the matches, their order and the number of results (FR025 to FR027, FR030, FR031).
 */
class SearchSettingsApiTest {

    private static final String A = "http://a.example.com";
    private static final String B = "http://b.example.org";
    private static final String C = "http://c.example.net";
    private static final String D = "http://d.example.com";
    private static final String E = "http://e.example.edu";

    private static final HttpClient client = HttpClient.newHttpClient();
    private static EngineServer server;
    private static String index;

    @BeforeAll
    static void start() throws Exception {
        Logger.getLogger("quickdex").setLevel(Level.WARNING);
        server = EngineServer.start(0);
        String pages = A + " Java the programming language\\n"
                + B + " java coffee beans from Indonesia\\n"
                + C + " JAVA acronym for Just Another Vague Acronym\\n"
                + D + " Self-driving cars and Java apps\\n"
                + E + " Quokka diet\\n";
        HttpResponse<String> built = send(HttpRequest.newBuilder(uri("/index"))
                .POST(HttpRequest.BodyPublishers.ofString(pages.replace("\\n", "\n"), StandardCharsets.UTF_8)));
        Matcher id = Pattern.compile("\"index\":\"([^\"]+)\"").matcher(built.body());
        assertTrue(id.find(), built.body());
        index = id.group(1);
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    private static URI uri(String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }

    private static HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String enc(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /** The URLs a search returns, in order. */
    private static List<String> urls(String query, String settings) throws Exception {
        String body = send(HttpRequest.newBuilder(uri("/search?index=" + index + "&q=" + enc(query) + settings)).GET())
                .body();
        List<String> urls = new ArrayList<>();
        Matcher m = Pattern.compile("\"line\":\\d+,\"url\":\"([^\"]+)\"").matcher(body);
        while (m.find()) {
            urls.add(m.group(1));
        }
        return urls;
    }

    private static void post(String path) throws Exception {
        assertEquals(200, send(HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody())).statusCode());
    }

    @Test
    void searchModeChangesWhichPagesMatch() throws Exception {
        assertEquals(List.of(A, B, C, D), urls("java", "").stream().sorted().toList());
        assertEquals(List.of(A, B, C, D), urls("java coffee", "&mode=or").stream().sorted().toList());
        assertEquals(List.of(B), urls("java coffee", "&mode=and"));
        assertEquals(List.of(A, C, D), urls("java coffee", "&mode=not").stream().sorted().toList());
    }

    @Test
    void caseSettingChangesWhichPagesMatch() throws Exception {
        assertEquals(4, urls("java", "").size());
        assertEquals(List.of(B), urls("java", "&case=true"));
        assertEquals(List.of(A, D), urls("Java", "&case=true").stream().sorted().toList());
        assertEquals(List.of(C), urls("JAVA", "&case=true"));
    }

    @Test
    void ignoredSymbolsChangeWhichPagesMatch() throws Exception {
        assertEquals(List.of(), urls("selfdriving", ""));
        assertEquals(List.of(D), urls("selfdriving", "&symbols=" + enc("-")));
        assertEquals(List.of(D), urls("self-driving", "&symbols=" + enc("-")));
    }

    @Test
    void pageSizeChangesHowManyResultsShow() throws Exception {
        String all = send(HttpRequest.newBuilder(uri("/search?index=" + index + "&q=java&limit=10")).GET()).body();
        assertTrue(all.contains("\"total\":4,"), all);
        List<String> first = urls("java", "&limit=2");
        List<String> second = urls("java", "&limit=2&offset=2");
        assertEquals(2, first.size());
        assertEquals(2, second.size());
        assertEquals(urls("java", "&limit=10"), concat(first, second));
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    @Test
    void orderChangesTheSequenceButNeverTheMatches() throws Exception {
        String enc = "&order=";
        List<String> relevance = urls("java", enc + "relevance");
        List<String> alphabetical = urls("java", enc + "alphabetical");
        // The pages' texts, A to Z: "JAVA acronym…", "java coffee…", "Java the…", "Self-driving…".
        assertEquals(List.of(C, B, A, D), alphabetical);

        post("/click?index=" + index + "&url=" + enc(D));
        post("/click?index=" + index + "&url=" + enc(D));
        post("/click?index=" + index + "&url=" + enc(A));
        List<String> clicks = urls("java", enc + "clicks");
        assertEquals(List.of(D, A, C, B), clicks);

        post("/bid?index=" + index + "&amount=5&url=" + enc(B));
        List<String> bids = urls("java", enc + "bid");
        assertEquals(List.of(B, C, A, D), bids);

        for (List<String> order : List.of(alphabetical, clicks, bids)) {
            assertEquals(relevance.stream().sorted().toList(), order.stream().sorted().toList());
        }
    }

    @Test
    void settingsWorkTogether() throws Exception {
        // Case-sensitive, ignoring "-", all words: only the page with both words as typed.
        assertEquals(List.of(D), urls("Java Selfdriving", "&mode=and&case=true&symbols=" + enc("-")));
        // Wrong case finds nothing, and so does leaving the symbol in.
        assertEquals(List.of(), urls("Java selfdriving", "&mode=and&case=true&symbols=" + enc("-")));
        assertEquals(List.of(), urls("Java Selfdriving", "&mode=and&case=true"));
        assertEquals(List.of(D), urls("selfdriving", "&mode=and&symbols=" + enc("-") + "&order=alphabetical&limit=1"));
    }

    @Test
    void growingTheWebIndexKeepsEarlierEntriesAndTheirOrder() throws Exception {
        List<String> before = exported();
        assertTrue(before.size() > 100, "the built-in index should be big: " + before.size());

        HttpResponse<String> added = send(HttpRequest.newBuilder(uri("/pages")).POST(HttpRequest.BodyPublishers.ofString(
                "https://grow.example.org/one Zebras graze on grass\nhttps://grow.example.org/two Apples grow on trees\n",
                StandardCharsets.UTF_8)));
        assertTrue(added.body().startsWith("{\"added\":2,"), added.body());

        List<String> after = exported();
        assertEquals(before.size() + 6, after.size(), "two pages of 3 words each, none a noise word start: 'on' is noise");
        assertEquals(before, after.stream().filter(before::contains).toList());
    }

    private static List<String> exported() throws Exception {
        return send(HttpRequest.newBuilder(uri("/export?format=txt")).GET()).body().lines().toList();
    }
}
