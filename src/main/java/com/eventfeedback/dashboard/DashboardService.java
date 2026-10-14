package com.eventfeedback.dashboard;

import static com.eventfeedback.common.Numbers.percent;
import static com.eventfeedback.common.Numbers.round2;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.event.EventRepository;
import com.eventfeedback.event.EventStatus;
import com.eventfeedback.feedback.FeedbackRepository;
import com.eventfeedback.feedback.FeedbackStatus;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** A response with an overall rating of 4 or 5 counts as "satisfied". */
    static final int SATISFIED_FROM = 4;

    /**
     * Summary indicators for the dashboard (T05).
     *
     * @param satisfactionPercent share of responses rated {@value DashboardService#SATISFIED_FROM} or higher
     */
    public record SummaryIndicators(long totalEvents, long totalResponses, Double averageRating,
                                    Integer satisfactionPercent, Integer recommendPercent, Instant generatedAt) {
    }

    /** Counts per status for the drill-down (T07); every status is present, also with 0. */
    public record StatusBreakdown(Map<EventStatus, Long> events, Map<FeedbackStatus, Long> feedback) {
    }

    private final EventRepository events;
    private final FeedbackRepository feedback;

    public DashboardService(EventRepository events, FeedbackRepository feedback) {
        this.events = events;
        this.feedback = feedback;
    }

    public SummaryIndicators summary() {
        long responses = feedback.count();
        return new SummaryIndicators(
                events.count(),
                responses,
                round2(feedback.averageOverallRating()),
                percent(feedback.countByOverallRatingGreaterThanEqual(SATISFIED_FROM), responses),
                percent(feedback.countByWouldRecommendTrue(), responses),
                Instant.now());
    }

    public StatusBreakdown statusBreakdown() {
        Map<EventStatus, Long> eventCounts = new LinkedHashMap<>();
        for (EventStatus status : EventStatus.values()) {
            eventCounts.put(status, events.countByStatus(status));
        }
        Map<FeedbackStatus, Long> feedbackCounts = new LinkedHashMap<>();
        for (FeedbackStatus status : FeedbackStatus.values()) {
            feedbackCounts.put(status, feedback.countByStatus(status));
        }
        return new StatusBreakdown(eventCounts, feedbackCounts);
    }
}
