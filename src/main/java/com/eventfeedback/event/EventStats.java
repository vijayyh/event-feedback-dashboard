package com.eventfeedback.event;

/** Aggregated feedback numbers for one event, filled by a JPQL constructor expression. */
public record EventStats(Long eventId, Long responseCount, Double averageRating) {
}
