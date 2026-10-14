package com.eventfeedback.event;

import static com.eventfeedback.common.Numbers.round2;

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
        Double average = stats != null ? round2(stats.averageRating()) : null;
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(), e.getCategory(),
                e.getVenue(), e.getOrganizer(), e.getEventDate(), e.getExpectedAttendees(),
                e.getStatus(), e.getCreatedAt(), count, average);
    }
}
