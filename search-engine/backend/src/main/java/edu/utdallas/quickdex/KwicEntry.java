package edu.utdallas.quickdex;

/**
 * One entry of the KWIC index: a circular shift split into its keyword and context.
 *
 * @param id         the shift's position in generation order, starting at 0; ties in the
 *                   alphabetical order are broken by this value
 * @param lineNumber the line the shift came from (1-based, counting only non-blank lines)
 * @param keyword    the first word of the shift
 * @param context    the remaining words of the shift, separated by single spaces
 * @param url        the URL associated with the line, or {@code null} if it has none
 */
public record KwicEntry(int id, int lineNumber, String keyword, String context, String url) {

    /** The whole shift as text: the keyword followed by its context. */
    public String text() {
        return context.isEmpty() ? keyword : keyword + " " + context;
    }
}
