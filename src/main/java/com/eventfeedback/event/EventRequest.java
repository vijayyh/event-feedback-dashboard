package com.eventfeedback.event;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/events}. */
public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull EventCategory category,
        @Size(max = 150) String venue,
        @Size(max = 100) String organizer,
        @NotNull LocalDate eventDate,
        @PositiveOrZero Integer expectedAttendees,
        EventStatus status) {
}
