package com.eventfeedback.feedback;

import static com.eventfeedback.common.Text.trimToNull;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.alert.AlertRuleEngine;
import com.eventfeedback.common.BusinessRuleException;
import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventService;
import com.eventfeedback.event.EventStatus;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedback;
    private final EventService eventService;
    private final AlertRuleEngine alertRules;

    public FeedbackService(FeedbackRepository feedback, EventService eventService, AlertRuleEngine alertRules) {
        this.feedback = feedback;
        this.eventService = eventService;
        this.alertRules = alertRules;
    }

    @Transactional
    public FeedbackResponse submit(Long eventId, FeedbackRequest request) {
        Event event = eventService.findEvent(eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Feedback cannot be submitted for a cancelled event");
        }
        if (event.getStatus() == EventStatus.UPCOMING) {
            throw new BusinessRuleException("Feedback opens when the event starts");
        }
        Feedback entry = new Feedback(event, request.overallRating(), request.contentRating(),
                request.organizationRating(), request.wouldRecommend());
        entry.setComments(trimToNull(request.comments()));
        entry.setAttendeeName(trimToNull(request.attendeeName()));
        entry.setAttendeeEmail(trimToNull(request.attendeeEmail()));
        Feedback saved = feedback.save(entry);
        alertRules.evaluate(saved); // T09: raise alerts automatically
        return FeedbackResponse.from(saved);
    }

    public List<FeedbackResponse> listForEvent(Long eventId) {
        eventService.findEvent(eventId); // 404 for unknown events
        return feedback.findByEventIdOrderBySubmittedAtDescIdDesc(eventId).stream()
                .map(FeedbackResponse::from)
                .toList();
    }
}
