package edu.utdallas.quickdex;

/**
 * Hears about every change to the engine's data, so a view can update itself.
 *
 * <p><b>Pattern: Observer.</b> {@link MasterControl} is the subject: it calls
 * {@link #modelChanged} on each listener added with {@link MasterControl#addListener}. The
 * web server's listener turns the change into a message for every open browser page.
 */
@FunctionalInterface
public interface ModelListener {

    /** Called right after the engine's data changed, on the thread that changed it. */
    void modelChanged(ModelChange change);
}
