package edu.utdallas.quickdex.api;

import com.sun.net.httpserver.HttpExchange;
import edu.utdallas.quickdex.ModelChange;
import edu.utdallas.quickdex.ModelListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Sends the engine's changes to every open browser page as server-sent events.
 *
 * <p>It is a {@link ModelListener}: the engine calls it when its data changes, and it passes
 * each change to the streams that are open. A page that is too slow to keep up is dropped and
 * reconnects on its own, so one slow page cannot hold up the engine.
 */
final class ChangeStream implements ModelListener {

    /** How many changes may wait for one slow page before it is dropped. */
    static final int MAX_WAITING = 256;
    static final long KEEP_ALIVE_SECONDS = 15;

    private static final ModelChange CLOSE = new ModelChange(null, null);

    private final Set<BlockingQueue<ModelChange>> open = ConcurrentHashMap.newKeySet();

    @Override
    public void modelChanged(ModelChange change) {
        for (BlockingQueue<ModelChange> queue : open) {
            if (!queue.offer(change)) {
                open.remove(queue);
                queue.clear();
                queue.offer(CLOSE);
            }
        }
    }

    /** Ends every open stream, so the server can stop. */
    void close() {
        for (BlockingQueue<ModelChange> queue : open) {
            queue.clear();
            queue.offer(CLOSE);
        }
        open.clear();
    }

    /** Keeps the response open and writes each change until the page leaves or the stream closes. */
    void serve(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);
        BlockingQueue<ModelChange> queue = new LinkedBlockingQueue<>(MAX_WAITING);
        open.add(queue);
        try (OutputStream out = exchange.getResponseBody()) {
            write(out, ": connected\n\n");
            while (true) {
                ModelChange change = queue.poll(KEEP_ALIVE_SECONDS, TimeUnit.SECONDS);
                if (change == CLOSE) {
                    return;
                }
                write(out, change == null ? ": keep-alive\n\n" : "data: " + json(change) + "\n\n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // The page closed the stream.
        } finally {
            open.remove(queue);
        }
    }

    static String json(ModelChange change) {
        return "{\"kind\":" + Json.string(change.kind().name())
                + ",\"url\":" + (change.url() == null ? "null" : Json.string(change.url())) + "}";
    }

    private static void write(OutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
