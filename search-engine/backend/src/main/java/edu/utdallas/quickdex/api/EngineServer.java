package edu.utdallas.quickdex.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.utdallas.quickdex.Input;
import edu.utdallas.quickdex.MasterControl;
import edu.utdallas.quickdex.Output;
import edu.utdallas.quickdex.PlacedEntry;
import edu.utdallas.quickdex.QueueInputMedium;
import edu.utdallas.quickdex.SearchResult;
import edu.utdallas.quickdex.TextInputMedium;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * An HTTP server that connects the web page (the Input and Output Medium) to the engine.
 *
 * <ul>
 *   <li>{@code GET /search?q=WORDS} searches the built-in web index and returns the matching
 *       pages, {@code limit} at a time from {@code offset}. Add {@code index=ID} to search an
 *       index built with {@code POST /index} instead.</li>
 *   <li>{@code GET /suggest?q=TYPED} suggests searches that complete what the user typed.</li>
 *   <li>{@code POST /pages} with lines of "URL description" as the body adds new pages to the
 *       web index, one line at a time; pages whose URL is already indexed are skipped.</li>
 *   <li>{@code GET /stats} returns the number of pages and entries in an index.</li>
 *   <li>{@code POST /index} with the input text as the body builds a new index and streams an
 *       update after every line, as newline-delimited JSON. The optional {@code delay}
 *       parameter slows processing to that many milliseconds per line, for demos.</li>
 *   <li>{@code GET /export?index=ID&format=csv|txt} downloads an index as a file.</li>
 *   <li>{@code GET /health} reports that the server is running.</li>
 * </ul>
 *
 * <p>The built-in web index is built at startup from {@code corpus.txt} (one page per line:
 * a URL and its description), or from the file named by {@code QUICKDEX_CORPUS}. Indexes
 * built with {@code POST /index} are kept for 30 minutes after their last use, and only the
 * most recent {@value #MAX_INDEXES} are kept.
 */
public final class EngineServer {

    private static final Logger LOG = System.getLogger("quickdex.api");

    static final int MAX_INDEXES = 64;
    static final long INDEX_TTL_MILLIS = 30 * 60 * 1000;
    static final int MAX_LIMIT = 1_000;
    static final int MAX_SUGGESTIONS = 10;
    static final int MAX_WEB_PAGES = 50_000;
    static final int MAX_DELAY_MILLIS = 2_000;

    private final HttpServer server;
    private final IndexSession webIndex;
    private final Map<String, IndexSession> indexes = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, IndexSession> eldest) {
            return size() > MAX_INDEXES;
        }
    };

    /** One index being built or searched, with the medium its requests are served through. */
    private static final class IndexSession {
        final MasterControl control;
        final WebOutputMedium medium;
        volatile boolean ready;
        volatile long lastUsed = System.currentTimeMillis();
        volatile int pages;
        volatile int entries;
        /** For the web index: where new pages are queued, and the URLs already indexed. */
        QueueInputMedium queue;
        final Set<String> urls = new HashSet<>();

        IndexSession(MasterControl control, WebOutputMedium medium) {
            this.control = control;
            this.medium = medium;
        }
    }

    private EngineServer(HttpServer server, IndexSession webIndex) {
        this.server = server;
        this.webIndex = webIndex;
        server.createContext("/health", this::health);
        server.createContext("/stats", this::stats);
        server.createContext("/suggest", this::suggest);
        server.createContext("/pages", this::addPages);
        server.createContext("/index", this::index);
        server.createContext("/search", this::search);
        server.createContext("/export", this::export);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    /** Builds the web index and starts a server on {@code port}; port 0 picks a free port. */
    public static EngineServer start(int port) throws IOException {
        IndexSession web = buildWebIndex(loadCorpus());
        EngineServer engine = new EngineServer(HttpServer.create(new InetSocketAddress(port), 0), web);
        engine.server.start();
        LOG.log(Level.INFO, "web index ready: {0} pages, {1} entries", web.pages, web.entries);
        return engine;
    }

    private static String loadCorpus() throws IOException {
        String file = System.getenv("QUICKDEX_CORPUS");
        if (file != null && !file.isBlank()) {
            return Files.readString(Path.of(file), StandardCharsets.UTF_8);
        }
        try (InputStream in = EngineServer.class.getResourceAsStream("/corpus.txt")) {
            if (in == null) {
                throw new IOException("corpus.txt is missing from the engine's resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static IndexSession buildWebIndex(String corpus) {
        Optional<String> problem = Input.validate(corpus);
        if (problem.isPresent()) {
            throw new IllegalArgumentException("The web corpus is not valid: " + problem.get());
        }
        // Building the web index at startup would log every line; keep only warnings.
        java.util.logging.Logger engineLog = java.util.logging.Logger.getLogger("quickdex");
        java.util.logging.Level level = engineLog.getLevel();
        engineLog.setLevel(java.util.logging.Level.WARNING);
        try {
            WebOutputMedium medium = new WebOutputMedium();
            QueueInputMedium queue = new QueueInputMedium();
            IndexSession web = new IndexSession(new MasterControl(queue, medium), medium);
            web.queue = queue;
            List<String> lines = Arrays.asList(Input.splitLines(corpus));
            for (String line : lines) {
                String url = Input.urlOf(line);
                if (url != null) {
                    web.urls.add(url);
                }
            }
            queue.add(lines);
            MasterControl.RunSummary summary = web.control.run();
            web.pages = summary.linesIndexed();
            web.entries = summary.shifts();
            web.ready = true;
            return web;
        } finally {
            engineLog.setLevel(level);
        }
    }

    /** Returns the port the server is listening on. */
    public int port() {
        return server.getAddress().getPort();
    }

    /** Stops the server. */
    public void stop() {
        server.stop(0);
    }

    public static void main(String[] args) throws IOException {
        if (System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$s %5$s%6$s%n");
        }
        String portSetting = args.length > 0 ? args[0] : System.getenv().getOrDefault("PORT", "8080");
        EngineServer engine = start(Integer.parseInt(portSetting));
        System.out.println("Quickdex engine listening on http://localhost:" + engine.port());
    }

    private void health(HttpExchange exchange) throws IOException {
        send(exchange, 200, "application/json", "{\"status\":\"ok\"}");
    }

    private void stats(HttpExchange exchange) throws IOException {
        IndexSession session = find(exchange, query(exchange).get("index"));
        if (session != null) {
            send(exchange, 200, "application/json",
                    "{\"pages\":" + session.pages + ",\"entries\":" + session.entries + "}");
        }
    }

    private void suggest(HttpExchange exchange) throws IOException {
        Map<String, String> params = query(exchange);
        IndexSession session = find(exchange, params.get("index"));
        if (session == null) {
            return;
        }
        String typed = params.getOrDefault("q", "");
        int limit = (int) Math.min(MAX_SUGGESTIONS, Math.max(1, parseLong(params.get("limit"), 8)));
        List<String> suggestions;
        synchronized (session) {
            suggestions = session.control.suggest(typed, limit);
        }
        StringBuilder json = new StringBuilder("{\"query\":").append(Json.string(typed)).append(",\"suggestions\":[");
        for (int i = 0; i < suggestions.size(); i++) {
            json.append(i == 0 ? "" : ",").append(Json.string(suggestions.get(i)));
        }
        send(exchange, 200, "application/json", json.append("]}").toString());
    }

    /** Adds pages fetched from the web to the web index, one line at a time. */
    private void addPages(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", Json.error("Use POST to add pages."));
            return;
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(Input.MAX_BYTES + 1);
        String text = new String(bytes, StandardCharsets.UTF_8);
        Optional<String> problem = bytes.length > Input.MAX_BYTES
                ? Optional.of("The pages are over the 1 MB limit.")
                : Input.validate(text);
        if (problem.isPresent()) {
            send(exchange, 400, "application/json", Json.error(problem.get()));
            return;
        }
        IndexSession web = webIndex;
        int added = 0;
        synchronized (web) {
            List<String> fresh = new ArrayList<>();
            for (String line : Input.splitLines(text)) {
                String url = Input.urlOf(line);
                if (url != null && web.pages + fresh.size() < MAX_WEB_PAGES && web.urls.add(url)) {
                    fresh.add(line);
                }
            }
            if (!fresh.isEmpty()) {
                web.queue.add(fresh);
                MasterControl.RunSummary summary = web.control.run();
                web.pages += summary.linesIndexed();
                web.entries = summary.shifts();
                added = summary.linesIndexed();
            }
        }
        send(exchange, 200, "application/json",
                "{\"added\":" + added + ",\"pages\":" + web.pages + ",\"entries\":" + web.entries + "}");
    }

    private void index(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", Json.error("Use POST to create an index."));
            return;
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(Input.MAX_BYTES + 1);
        if (bytes.length > Input.MAX_BYTES) {
            send(exchange, 413, "application/json",
                    Json.error("The input is over the 1 MB limit. Remove some text and try again."));
            return;
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            send(exchange, 400, "application/json",
                    Json.error("The input is not UTF-8 plain text. Save it as UTF-8 and try again."));
            return;
        }
        Optional<String> problem = Input.validate(text);
        if (problem.isPresent()) {
            send(exchange, 400, "application/json", Json.error(problem.get()));
            return;
        }

        Map<String, String> params = query(exchange);
        long delay = Math.min(MAX_DELAY_MILLIS, Math.max(0, parseLong(params.get("delay"), 0)));
        TextInputMedium inputMedium = new TextInputMedium(text, delay);
        WebOutputMedium outputMedium = new WebOutputMedium();
        IndexSession session = new IndexSession(new MasterControl(inputMedium, outputMedium), outputMedium);
        String id = UUID.randomUUID().toString();
        synchronized (indexes) {
            expireOldIndexes();
            indexes.put(id, session);
        }

        exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-transform");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream body = exchange.getResponseBody()) {
            Consumer<String> sink = line -> {
                try {
                    body.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    body.flush();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            synchronized (session) {
                try {
                    sink.accept("{\"type\":\"start\",\"index\":" + Json.string(id)
                            + ",\"lines\":" + inputMedium.lineCount() + "}");
                    outputMedium.streamUpdatesTo(sink);
                    MasterControl.RunSummary summary = session.control.run();
                    outputMedium.stopStreaming();
                    session.pages = summary.linesIndexed();
                    session.entries = summary.shifts();
                    session.ready = true;
                    sink.accept("{\"type\":\"done\",\"linesIndexed\":" + summary.linesIndexed()
                            + ",\"linesSkipped\":" + summary.linesSkipped()
                            + ",\"shifts\":" + summary.shifts()
                            + ",\"elapsedMillis\":" + summary.elapsedMillis() + "}");
                } catch (UncheckedIOException e) {
                    LOG.log(Level.INFO, "client stopped reading index {0}; discarding it", id);
                    forget(id);
                } catch (RuntimeException e) {
                    LOG.log(Level.ERROR, "building index " + id + " failed", e);
                    forget(id);
                    outputMedium.stopStreaming();
                    try {
                        sink.accept("{\"type\":\"error\",\"message\":"
                                + Json.string("Quickdex could not finish the index. Try again.") + "}");
                    } catch (UncheckedIOException ignored) {
                        // The client has gone.
                    }
                } finally {
                    outputMedium.stopStreaming();
                }
            }
        }
    }

    private void search(HttpExchange exchange) throws IOException {
        Map<String, String> params = query(exchange);
        IndexSession session = find(exchange, params.get("index"));
        if (session == null) {
            return;
        }
        String words = params.getOrDefault("q", "");
        int offset = (int) Math.max(0, parseLong(params.get("offset"), 0));
        int limit = (int) Math.min(MAX_LIMIT, Math.max(1, parseLong(params.get("limit"), 10)));

        long start = System.nanoTime();
        WebOutputMedium.SearchResults found;
        synchronized (session) {
            session.control.search(words);
            found = session.medium.takeResults();
        }
        long micros = (System.nanoTime() - start) / 1_000;

        List<SearchResult> results = found.results();
        int from = Math.min(offset, results.size());
        int to = Math.min(results.size(), from + limit);
        StringBuilder json = new StringBuilder(512 * (to - from) + 128)
                .append("{\"query\":").append(Json.string(found.query()))
                .append(",\"total\":").append(results.size())
                .append(",\"offset\":").append(from)
                .append(",\"elapsedMicros\":").append(micros)
                .append(",\"pages\":").append(session.pages)
                .append(",\"entries\":").append(session.entries)
                .append(",\"results\":[");
        for (int i = from; i < to; i++) {
            SearchResult r = results.get(i);
            json.append(i == from ? "{" : ",{")
                    .append("\"line\":").append(r.lineNumber())
                    .append(",\"url\":").append(Json.string(r.url()))
                    .append(",\"text\":").append(Json.string(r.text()))
                    .append(",\"phrase\":").append(r.phraseMatch())
                    .append(",\"missing\":[")
                    .append(String.join(",", r.missingWords().stream().map(Json::string).toList()))
                    .append(']')
                    .append(",\"matches\":[");
            for (int m = 0; m < r.matches().size(); m++) {
                PlacedEntry p = r.matches().get(m);
                json.append(m == 0 ? "{" : ",{").append("\"pos\":").append(p.position()).append(',');
                Json.entryFields(json, p.entry()).append('}');
            }
            json.append("]}");
        }
        send(exchange, 200, "application/json", json.append("]}").toString());
    }

    private void export(HttpExchange exchange) throws IOException {
        Map<String, String> params = query(exchange);
        IndexSession session = find(exchange, params.get("index"));
        if (session == null) {
            return;
        }
        boolean csv = !"txt".equalsIgnoreCase(params.getOrDefault("format", "csv"));
        String file;
        synchronized (session) {
            file = session.control.export(csv ? Output.Format.CSV : Output.Format.TEXT);
        }
        exchange.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=\"quickdex-index." + (csv ? "csv" : "txt") + "\"");
        // The byte-order mark lets spreadsheet programs detect UTF-8.
        send(exchange, 200, csv ? "text/csv" : "text/plain", csv ? "﻿" + file : file);
    }

    /** Finds a finished index, or sends an error response and returns {@code null}. */
    private IndexSession find(HttpExchange exchange, String id) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", Json.error("Use GET."));
            return null;
        }
        if (id == null || id.isBlank()) {
            return webIndex;
        }
        IndexSession session;
        synchronized (indexes) {
            expireOldIndexes();
            session = indexes.get(id);
        }
        if (session == null) {
            send(exchange, 404, "application/json",
                    Json.error("This index has expired. Create the index again."));
            return null;
        }
        if (!session.ready) {
            send(exchange, 409, "application/json",
                    Json.error("The index is still being built. Try again when it is finished."));
            return null;
        }
        session.lastUsed = System.currentTimeMillis();
        return session;
    }

    private void forget(String id) {
        synchronized (indexes) {
            indexes.remove(id);
        }
    }

    private void expireOldIndexes() {
        long cutoff = System.currentTimeMillis() - INDEX_TTL_MILLIS;
        indexes.values().removeIf(s -> s.ready && s.lastUsed < cutoff);
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null) {
            return params;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private static long parseLong(String value, long fallback) {
        try {
            return value == null ? fallback : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
