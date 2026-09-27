package edu.utdallas.quickdex.api;

import edu.utdallas.quickdex.KwicEntry;

/** Minimal JSON writing, so the engine needs nothing beyond the Java standard library. */
final class Json {

    private Json() {
    }

    /** Returns {@code value} as a JSON string literal, or {@code null}. */
    static String string(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** Appends an entry's fields, without the surrounding braces. */
    static StringBuilder entryFields(StringBuilder out, KwicEntry e) {
        return out.append("\"id\":").append(e.id())
                .append(",\"line\":").append(e.lineNumber())
                .append(",\"keyword\":").append(string(e.keyword()))
                .append(",\"context\":").append(string(e.context()))
                .append(",\"url\":").append(string(e.url()));
    }

    /** Returns {@code {"error": message}}. */
    static String error(String message) {
        return "{\"error\":" + string(message) + "}";
    }
}
