package com.eventfeedback.feedback;

import java.time.Instant;

/** Feedback as returned by the API. The attendee's e-mail is never exposed. */
public record FeedbackResponse(
        Long id,
        Long eventId,
        String eventTitle,
        int overallRating,
        int contentRating,
        int organizationRating,
        boolean wouldRecommend,
        String comments,
        String attendeeName,
        FeedbackStatus status,
        Instant submittedAt,
        Instant statusChangedAt) {

    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(f.getId(), f.getEvent().getId(), f.getEvent().getTitle(),
                f.getOverallRating(), f.getContentRating(), f.getOrganizationRating(), f.isWouldRecommend(),
                f.getComments(), f.getAttendeeName(), f.getStatus(), f.getSubmittedAt(), f.getStatusChangedAt());
    }
}
