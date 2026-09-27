package edu.utdallas.quickdex.api;

import edu.utdallas.quickdex.OutputMedium;
import edu.utdallas.quickdex.PlacedEntry;
import edu.utdallas.quickdex.SearchResult;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Output Medium for the web page: delivers index updates and search results to whichever
 * HTTP request is currently being served for an index.
 *
 * <p>While an index is being built, each update is written to the streaming response as one
 * line of JSON. A search stores its results for the search request to return.
 */
final class WebOutputMedium implements OutputMedium {

    private Consumer<String> updates = line -> { };
    private SearchResults results;

    /** The pages found by the last search. */
    record SearchResults(String query, List<SearchResult> results) {
    }

    /** Sends index updates to {@code sink}, one JSON object per call. */
    void streamUpdatesTo(Consumer<String> sink) {
        this.updates = sink;
    }

    /** Stops sending index updates. */
    void stopStreaming() {
        this.updates = line -> { };
    }

    /** Returns and clears the results of the last search. */
    SearchResults takeResults() {
        SearchResults r = results;
        results = null;
        return r;
    }

    @Override
    public void displayOutput(int lineNumber, List<PlacedEntry> inserted, int indexSize) {
        StringBuilder json = new StringBuilder(128 * inserted.size())
                .append("{\"type\":\"update\",\"line\":").append(lineNumber)
                .append(",\"size\":").append(indexSize)
                .append(",\"inserted\":[");
        for (int i = 0; i < inserted.size(); i++) {
            PlacedEntry p = inserted.get(i);
            json.append(i == 0 ? "{" : ",{").append("\"pos\":").append(p.position()).append(',');
            Json.entryFields(json, p.entry()).append('}');
        }
        updates.accept(json.append("]}").toString());
    }

    @Override
    public void displayResults(String query, List<SearchResult> found) {
        results = new SearchResults(query, found);
    }
}
