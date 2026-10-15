package com.eventfeedback.event;

import static com.eventfeedback.common.Text.LIKE_ESCAPE;
import static com.eventfeedback.common.Text.containsPattern;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Optional search criteria for {@code GET /api/events}; every null field is ignored.
 *
 * @param q        keyword matched (case-insensitive) in title, description, venue and organiser
 * @param status   exact status
 * @param category exact category
 * @param from     events on or after this date
 * @param to       events on or before this date
 */
public record EventFilter(String q, EventStatus status, EventCategory category, LocalDate from, LocalDate to) {

    public static EventFilter none() {
        return new EventFilter(null, null, null, null, null);
    }

    Specification<Event> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = containsPattern(q);
                where.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("venue")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("organizer")), pattern, LIKE_ESCAPE)));
            }
            if (status != null) {
                where.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                where.add(cb.equal(root.get("category"), category));
            }
            if (from != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("eventDate"), from));
            }
            if (to != null) {
                where.add(cb.lessThanOrEqualTo(root.get("eventDate"), to));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
