package com.eventfeedback.alert;

import java.time.Instant;

import com.eventfeedback.feedback.Feedback;

/** An alert as returned by the API, with the event and (if any) the triggering feedback. */
public record AlertResponse(
        Long id,
        AlertType type,
        AlertSeverity severity,
        AlertStatus status,
        String message,
        Long eventId,
        String eventTitle,
        Long feedbackId,
        Integer feedbackRating,
        String feedbackComment,
        Instant createdAt,
        Instant acknowledgedAt,
        Instant resolvedAt) {

    public static AlertResponse from(Alert a) {
        Feedback f = a.getFeedback();
        return new AlertResponse(a.getId(), a.getType(), a.getSeverity(), a.getStatus(), a.getMessage(),
                a.getEvent().getId(), a.getEvent().getTitle(),
                f != null ? f.getId() : null,
                f != null ? f.getOverallRating() : null,
                f != null ? f.getComments() : null,
                a.getCreatedAt(), a.getAcknowledgedAt(), a.getResolvedAt());
    }
}
