package com.eventfeedback.common;

/** Small helpers for user-entered text. */
public final class Text {

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
}
