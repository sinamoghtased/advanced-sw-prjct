package edu.utdallas.quickdex;

import java.util.ArrayList;
import java.util.List;

/** An Output Medium that records what it is asked to show, for tests. */
final class RecordingMedium implements OutputMedium {

    record Update(int lineNumber, List<PlacedEntry> inserted, int indexSize) {
    }

    final List<Update> updates = new ArrayList<>();
    final List<KwicEntry> shown = new ArrayList<>();
    List<SearchResult> results;
    String query;

    @Override
    public void displayOutput(int lineNumber, List<PlacedEntry> inserted, int indexSize) {
        updates.add(new Update(lineNumber, inserted, indexSize));
        // Rebuild the index the way the web page does: insert in ascending position order.
        for (PlacedEntry p : inserted) {
            shown.add(p.position(), p.entry());
        }
    }

    @Override
    public void displayResults(String query, List<SearchResult> results) {
        this.query = query;
        this.results = results;
    }

    static List<String> texts(List<KwicEntry> entries) {
        return entries.stream().map(KwicEntry::text).toList();
    }
}
