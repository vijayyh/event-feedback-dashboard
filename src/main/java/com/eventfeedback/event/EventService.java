package com.eventfeedback.event;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.common.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository events;

    public EventService(EventRepository events) {
        this.events = events;
    }

    public List<EventResponse> listEvents() {
        return events.findAllByOrderByEventDateDescIdDesc().stream()
                .map(e -> EventResponse.from(e, null))
                .toList();
    }

    public EventResponse getEvent(Long id) {
        return EventResponse.from(findEvent(id), null);
    }

    /** Loads the entity or fails with 404; shared with other services. */
    public Event findEvent(Long id) {
        return events.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = new Event(request.title().trim(), request.category(), request.eventDate());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setOrganizer(request.organizer());
        event.setExpectedAttendees(request.expectedAttendees());
        if (request.status() != null) {
            event.setStatus(request.status());
        }
        return EventResponse.from(events.save(event), null);
    }
}
