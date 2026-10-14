package com.eventfeedback.event;

import java.time.Instant;
import java.time.LocalDate;

/** An event as returned by the API, including its feedback statistics. */
public record EventResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        String venue,
        String organizer,
        LocalDate eventDate,
        Integer expectedAttendees,
        EventStatus status,
        Instant createdAt,
        long responseCount,
        Double averageRating) {

    public static EventResponse from(Event e, EventStats stats) {
        long count = stats != null ? stats.responseCount() : 0;
        Double average = stats != null && stats.averageRating() != null
                ? Math.round(stats.averageRating() * 100) / 100.0
                : null;
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(), e.getCategory(),
                e.getVenue(), e.getOrganizer(), e.getEventDate(), e.getExpectedAttendees(),
                e.getStatus(), e.getCreatedAt(), count, average);
    }
}
