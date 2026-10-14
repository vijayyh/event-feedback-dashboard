package com.eventfeedback.event;

import static com.eventfeedback.common.Text.trimToNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.common.ResourceNotFoundException;
import com.eventfeedback.feedback.FeedbackRepository;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository events;
    private final FeedbackRepository feedback;

    public EventService(EventRepository events, FeedbackRepository feedback) {
        this.events = events;
        this.feedback = feedback;
    }

    public List<EventResponse> listEvents() {
        Map<Long, EventStats> stats = feedback.statsPerEvent().stream()
                .collect(Collectors.toMap(EventStats::eventId, Function.identity()));
        return events.findAllByOrderByEventDateDescIdDesc().stream()
                .map(e -> EventResponse.from(e, stats.get(e.getId())))
                .toList();
    }

    public EventResponse getEvent(Long id) {
        Event event = findEvent(id);
        return EventResponse.from(event, feedback.statsForEvent(id).orElse(null));
    }

    /** Loads the entity or fails with 404; shared with other services. */
    public Event findEvent(Long id) {
        return events.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = new Event(request.title().trim(), request.category(), request.eventDate());
        event.setDescription(trimToNull(request.description()));
        event.setVenue(trimToNull(request.venue()));
        event.setOrganizer(trimToNull(request.organizer()));
        event.setExpectedAttendees(request.expectedAttendees());
        if (request.status() != null) {
            event.setStatus(request.status());
        }
        return EventResponse.from(events.save(event), null);
    }
}
