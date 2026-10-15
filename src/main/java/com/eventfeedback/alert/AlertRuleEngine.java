package com.eventfeedback.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.event.Event;
import com.eventfeedback.feedback.Feedback;
import com.eventfeedback.feedback.FeedbackRepository;

/**
 * Raises alerts automatically for every new feedback (T09):
 * <ul>
 *   <li>LOW_RATING (HIGH) – overall rating at or below {@code app.alerts.low-rating-max}</li>
 *   <li>NEGATIVE_COMMENT (MEDIUM) – the comment contains a configured negative keyword</li>
 *   <li>LOW_AVERAGE (CRITICAL) – the event average falls below the threshold; at most one unresolved per event</li>
 * </ul>
 * Each rule fires at most once per feedback, so evaluating the same feedback again is harmless.
 */
@Component
public class AlertRuleEngine {

    private static final List<AlertStatus> UNRESOLVED = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);
    private static final int EXCERPT_LENGTH = 120;

    private final AlertRepository alerts;
    private final FeedbackRepository feedback;
    private final AlertProperties properties;
    private final List<Keyword> keywords;

    private record Keyword(String text, Pattern pattern) {
    }

    public AlertRuleEngine(AlertRepository alerts, FeedbackRepository feedback, AlertProperties properties) {
        this.alerts = alerts;
        this.feedback = feedback;
        this.properties = properties;
        this.keywords = properties.negativeKeywords().stream()
                .map(k -> new Keyword(k, Pattern.compile("\\b" + Pattern.quote(k) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)))
                .toList();
    }

    @Transactional
    public List<Alert> evaluate(Feedback entry) {
        Event event = entry.getEvent();
        List<Alert> raised = new ArrayList<>();

        if (entry.getOverallRating() <= properties.lowRatingMax()
                && !alerts.existsByFeedbackIdAndType(entry.getId(), AlertType.LOW_RATING)) {
            raised.add(new Alert(event, entry, AlertType.LOW_RATING, AlertSeverity.HIGH,
                    "Rated %d/5 for \"%s\"".formatted(entry.getOverallRating(), event.getTitle())));
        }

        String keyword = negativeKeyword(entry.getComments());
        if (keyword != null && !alerts.existsByFeedbackIdAndType(entry.getId(), AlertType.NEGATIVE_COMMENT)) {
            raised.add(new Alert(event, entry, AlertType.NEGATIVE_COMMENT, AlertSeverity.MEDIUM,
                    "Negative comment (\"%s\"): %s".formatted(keyword, excerpt(entry.getComments()))));
        }

        feedback.statsForEvent(event.getId()).ifPresent(stats -> {
            if (stats.responseCount() >= properties.lowAverageMinResponses()
                    && stats.averageRating() < properties.lowAverageThreshold()
                    && !alerts.existsByEventIdAndTypeAndStatusIn(event.getId(), AlertType.LOW_AVERAGE, UNRESOLVED)) {
                raised.add(new Alert(event, null, AlertType.LOW_AVERAGE, AlertSeverity.CRITICAL,
                        String.format(Locale.ROOT, "Average rating of \"%s\" dropped to %.2f after %d responses",
                                event.getTitle(), stats.averageRating(), stats.responseCount())));
            }
        });
        return alerts.saveAll(raised);
    }

    /** First configured keyword found in the comment, or null. */
    String negativeKeyword(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return keywords.stream()
                .filter(k -> k.pattern().matcher(comment).find())
                .map(Keyword::text)
                .findFirst()
                .orElse(null);
    }

    private static String excerpt(String text) {
        return text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH - 1) + "…";
    }
}
