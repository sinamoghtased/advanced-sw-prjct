package edu.utdallas.quickdex;

import java.util.Locale;
import java.util.Set;

/**
 * Decides which circular shifts are kept in the index (FR018).
 *
 * <p><b>Secret:</b> which words count as noise. A shift is dropped when its first word is a
 * noise word such as "a", "the" or "of", so no index entry starts with one. Case and
 * punctuation around a word are ignored: "The," is the noise word "the".
 */
public final class NoiseFilter {

    /** Words too common to start an index entry or to narrow a search. */
    public static final Set<String> NOISE_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for", "from",
            "how", "i", "in", "is", "it", "me", "my", "of", "on", "or", "that", "the", "this", "to",
            "was", "what", "when", "where", "which", "who", "why", "with", "you", "your");

    /** A filter that keeps every shift. */
    public static final NoiseFilter NONE = new NoiseFilter(Set.of());

    private final Set<String> noise;

    private NoiseFilter(Set<String> noise) {
        this.noise = noise;
    }

    /** The filter Cyberminer uses: it drops shifts that start with a word in {@link #NOISE_WORDS}. */
    public static NoiseFilter standard() {
        return new NoiseFilter(NOISE_WORDS);
    }

    /** Whether a word is a noise word. */
    public boolean isNoise(String word) {
        return noise.contains(strip(word).toLowerCase(Locale.ROOT));
    }

    /** Whether a shift that starts with {@code firstWord} stays in the index. */
    public boolean keeps(String firstWord) {
        return !isNoise(firstWord);
    }

    /** Removes punctuation from the start and end of a word. */
    static String strip(String word) {
        int start = 0;
        int end = word.length();
        while (start < end && !Character.isLetterOrDigit(word.codePointAt(start))) {
            start += Character.charCount(word.codePointAt(start));
        }
        while (end > start && !Character.isLetterOrDigit(word.codePointBefore(end))) {
            end -= Character.charCount(word.codePointBefore(end));
        }
        return word.substring(start, end);
    }
}
