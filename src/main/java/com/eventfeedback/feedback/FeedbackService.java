package com.eventfeedback.feedback;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.common.BusinessRuleException;
import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventService;
import com.eventfeedback.event.EventStatus;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedback;
    private final EventService eventService;

    public FeedbackService(FeedbackRepository feedback, EventService eventService) {
        this.feedback = feedback;
        this.eventService = eventService;
    }

    @Transactional
    public FeedbackResponse submit(Long eventId, FeedbackRequest request) {
        Event event = eventService.findEvent(eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Feedback cannot be submitted for a cancelled event");
        }
        Feedback entry = new Feedback(event, request.overallRating(), request.contentRating(),
                request.organizationRating(), request.wouldRecommend());
        entry.setComments(request.comments());
        entry.setAttendeeName(request.attendeeName());
        entry.setAttendeeEmail(request.attendeeEmail());
        return FeedbackResponse.from(feedback.save(entry));
    }

    public List<FeedbackResponse> listForEvent(Long eventId) {
        eventService.findEvent(eventId); // 404 for unknown events
        return feedback.findByEventIdOrderBySubmittedAtDescIdDesc(eventId).stream()
                .map(FeedbackResponse::from)
                .toList();
    }
}
