package com.eventfeedback.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.feedback.FeedbackRepository;

/**
 * Feedback stored before the alert rules existed (or inserted by the demo seeder) is evaluated once,
 * when the alert table is still empty. Runs after DemoDataSeeder.
 */
@Component
@Order(2)
public class AlertBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlertBackfill.class);

    private final AlertRepository alerts;
    private final FeedbackRepository feedback;
    private final AlertRuleEngine engine;
    private final AlertProperties properties;

    public AlertBackfill(AlertRepository alerts, FeedbackRepository feedback, AlertRuleEngine engine,
                         AlertProperties properties) {
        this.alerts = alerts;
        this.feedback = feedback;
        this.engine = engine;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.backfillOnStartup() || alerts.count() > 0 || feedback.count() == 0) {
            return;
        }
        int raised = feedback.findAll(Sort.by("submittedAt", "id")).stream()
                .mapToInt(f -> engine.evaluate(f).size())
                .sum();
        log.info("Alert backfill: evaluated existing feedback, raised {} alerts", raised);
    }
}
