package edu.utdallas.quickdex;

/**
 * Where input lines come from: the web page, a file, or the console.
 *
 * <p>The Input Medium sits outside the KWIC system and is reached through system I/O.
 *
 * <p><b>Pattern: Bridge.</b> The pipeline stays the same while the medium varies:
 * {@link TextInputMedium} for a file or the console, {@link QueueInputMedium} for pages that
 * arrive over time.
 */
public interface InputMedium {

    /**
     * Returns the next raw line of input, or {@code null} when there are no more lines.
     */
    String readLine();
}
