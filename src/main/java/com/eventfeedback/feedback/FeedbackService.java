package com.eventfeedback.feedback;

import static com.eventfeedback.common.Numbers.percent;
import static com.eventfeedback.common.Numbers.round2;
import static com.eventfeedback.common.Text.trimToNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.alert.AlertRuleEngine;
import com.eventfeedback.common.BusinessRuleException;
import com.eventfeedback.common.ResourceNotFoundException;
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

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "submittedAt", "id");

    /** Feedback across all events matching the filter, newest first (organiser dashboard, T04). */
    public List<FeedbackResponse> search(FeedbackFilter filter) {
        return feedback.findAll(filter.toSpecification(), NEWEST_FIRST).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    /**
     * Follow-up workflow (T08): NEW -> REVIEWED / ACTION_REQUIRED / RESOLVED. Feedback never goes back to NEW
     * and RESOLVED is final.
     */
    @Transactional
    public FeedbackResponse changeStatus(Long id, FeedbackStatus newStatus) {
        Feedback entry = feedback.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback " + id + " not found"));
        if (entry.getStatus() == newStatus) {
            return FeedbackResponse.from(entry);
        }
        if (entry.getStatus() == FeedbackStatus.RESOLVED) {
            throw new BusinessRuleException("Resolved feedback cannot change status");
        }
        if (newStatus == FeedbackStatus.NEW) {
            throw new BusinessRuleException("Feedback cannot be moved back to NEW");
        }
        entry.changeStatus(newStatus);
        return FeedbackResponse.from(entry);
    }

    /** Rating distribution and averages for one event (status drill-down, T07). */
    public EventFeedbackSummary summarize(Long eventId) {
        eventService.findEvent(eventId); // 404 for unknown events
        List<Feedback> items = feedback.findByEventIdOrderBySubmittedAtDescIdDesc(eventId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            distribution.put(rating, 0L);
        }
        items.forEach(f -> distribution.merge(f.getOverallRating(), 1L, Long::sum));
        if (items.isEmpty()) {
            return new EventFeedbackSummary(eventId, 0, null, null, null, null, distribution);
        }
        long recommend = items.stream().filter(Feedback::isWouldRecommend).count();
        return new EventFeedbackSummary(eventId, items.size(),
                average(items, Feedback::getOverallRating),
                average(items, Feedback::getContentRating),
                average(items, Feedback::getOrganizationRating),
                percent(recommend, items.size()),
                distribution);
    }

    private static Double average(List<Feedback> items, ToIntFunction<Feedback> rating) {
        return round2(items.stream().mapToInt(rating).average().orElse(0));
    }
}
