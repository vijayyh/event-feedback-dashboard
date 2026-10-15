package com.eventfeedback.feedback;

import static com.eventfeedback.common.Text.LIKE_ESCAPE;
import static com.eventfeedback.common.Text.containsPattern;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Optional search criteria for {@code GET /api/feedback}; every null field is ignored.
 *
 * @param q         keyword matched (case-insensitive) in the comment and the attendee name
 * @param eventId   only feedback for this event
 * @param status    follow-up status
 * @param minRating overall rating at least this value
 * @param maxRating overall rating at most this value
 */
public record FeedbackFilter(String q, Long eventId, FeedbackStatus status, Integer minRating, Integer maxRating) {

    Specification<Feedback> toSpecification() {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class) {
                root.fetch("event"); // event title is shown with every row
            }
            List<Predicate> where = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = containsPattern(q);
                where.add(cb.or(
                        cb.like(cb.lower(root.get("comments")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("attendeeName")), pattern, LIKE_ESCAPE)));
            }
            if (eventId != null) {
                where.add(cb.equal(root.get("event").get("id"), eventId));
            }
            if (status != null) {
                where.add(cb.equal(root.get("status"), status));
            }
            if (minRating != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("overallRating"), minRating));
            }
            if (maxRating != null) {
                where.add(cb.lessThanOrEqualTo(root.get("overallRating"), maxRating));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
