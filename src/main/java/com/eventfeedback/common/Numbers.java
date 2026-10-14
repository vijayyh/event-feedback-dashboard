package com.eventfeedback.common;

/** Rounding used for every rating and percentage the API returns. */
public final class Numbers {

    private Numbers() {
    }

    /** Two decimals, e.g. 3.6666 -> 3.67; null stays null. */
    public static Double round2(Double value) {
        return value == null ? null : Math.round(value * 100) / 100.0;
    }

    /** Whole percentage of part in total; null when total is 0. */
    public static Integer percent(long part, long total) {
        return total == 0 ? null : (int) Math.round(part * 100.0 / total);
    }
}
