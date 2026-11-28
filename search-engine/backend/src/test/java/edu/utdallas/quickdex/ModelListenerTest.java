package edu.utdallas.quickdex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The engine tells its listeners when its data changes. */
class ModelListenerTest {

    private static final String URL = "http://old.example.com";

    private QueueInputMedium queue;
    private MasterControl control;
    private final List<ModelChange> heard = new ArrayList<>();

    @BeforeEach
    void buildIndex() {
        queue = new QueueInputMedium();
        control = new MasterControl(queue, new RecordingMedium());
        queue.add(List.of(URL + " Quokka habitats of Western Australia"));
        control.run();
        control.addListener(heard::add);
    }

    @Test
    void tellsListenersWhenPagesAreAdded() {
        queue.add(List.of("http://new.example.org Quokka diet and behavior"));
        control.run();
        assertEquals(List.of(new ModelChange(ModelChange.Kind.PAGES_ADDED, null)), heard);
    }

    @Test
    void staysQuietWhenARunAddsNothing() {
        control.run();
        assertTrue(heard.isEmpty());
    }

    @Test
    void tellsListenersAboutClicksAndBids() {
        control.recordClick(URL);
        control.setBid(URL, 5);
        assertEquals(List.of(new ModelChange(ModelChange.Kind.CLICKED, URL),
                new ModelChange(ModelChange.Kind.BID_CHANGED, URL)), heard);
    }

    @Test
    void tellsListenersOnlyWhenADeleteRemovedAPage() {
        control.delete("http://missing.example.com");
        assertTrue(heard.isEmpty());
        control.delete(URL);
        assertEquals(List.of(new ModelChange(ModelChange.Kind.PAGE_DELETED, URL)), heard);
    }

    @Test
    void stopsTellingARemovedListener() {
        ModelListener extra = change -> heard.add(change);
        control.addListener(extra);
        control.removeListener(extra);
        control.recordClick(URL);
        assertEquals(1, heard.size());
    }

    @Test
    void aFailingListenerDoesNotStopTheOthers() {
        control.addListener(change -> {
            throw new IllegalStateException("boom");
        });
        control.recordClick(URL);
        assertEquals(1, heard.size());
    }
}
