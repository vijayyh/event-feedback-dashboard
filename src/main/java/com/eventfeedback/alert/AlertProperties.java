package com.eventfeedback.alert;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Alert rule settings ({@code app.alerts.*} in application.properties).
 *
 * @param lowRatingMax           an overall rating at or below this raises LOW_RATING
 * @param lowAverageThreshold    an event average below this raises LOW_AVERAGE ...
 * @param lowAverageMinResponses ... once the event has at least this many responses
 * @param negativeKeywords       whole words/phrases that raise NEGATIVE_COMMENT (case-insensitive)
 * @param backfillOnStartup      evaluate existing feedback once when the alert table is empty
 */
@ConfigurationProperties(prefix = "app.alerts")
public record AlertProperties(
        @DefaultValue("2") int lowRatingMax,
        @DefaultValue("3.0") double lowAverageThreshold,
        @DefaultValue("5") int lowAverageMinResponses,
        @DefaultValue({"worst", "waste", "boring", "not audible", "rude", "poor", "terrible", "useless",
                "disappointed", "crowded", "too long", "too fast", "slow"}) List<String> negativeKeywords,
        @DefaultValue("true") boolean backfillOnStartup) {
}
