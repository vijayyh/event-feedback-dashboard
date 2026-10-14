package com.eventfeedback.feedback;

import java.util.Map;

/**
 * Drill-down numbers for one event ({@code GET /api/events/{id}/summary}).
 *
 * @param distribution number of responses per overall rating, keys 5 down to 1
 */
public record EventFeedbackSummary(
        Long eventId,
        int responseCount,
        Double averageRating,
        Double contentAverage,
        Double organizationAverage,
        Integer recommendPercent,
        Map<Integer, Long> distribution) {
}
