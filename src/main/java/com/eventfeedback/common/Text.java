package com.eventfeedback.common;

import java.util.Locale;

/** Small helpers for user-entered text. */
public final class Text {

    /** Escape character used in LIKE patterns built by {@link #containsPattern(String)}. */
    public static final char LIKE_ESCAPE = '\\';

    private Text() {
    }

    /** Trims the value and returns {@code null} when nothing is left, so blank optional fields are stored as null. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Lower-case "contains" pattern for a SQL LIKE, with {@code %}, {@code _} and the escape character
     * escaped so that user input is matched literally. Use with {@link #LIKE_ESCAPE}.
     */
    public static String containsPattern(String query) {
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
