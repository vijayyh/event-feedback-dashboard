package com.eventfeedback.feedback;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/events/{eventId}/feedback}. Name and e-mail are optional (anonymous feedback). */
public record FeedbackRequest(
        @NotNull @Min(1) @Max(5) Integer overallRating,
        @NotNull @Min(1) @Max(5) Integer contentRating,
        @NotNull @Min(1) @Max(5) Integer organizationRating,
        @NotNull Boolean wouldRecommend,
        @Size(max = 1000) String comments,
        @Size(max = 100) String attendeeName,
        @Email @Size(max = 150) String attendeeEmail) {
}
