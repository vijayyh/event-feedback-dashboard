package com.eventfeedback.alert;

import java.time.Instant;

import com.eventfeedback.event.Event;
import com.eventfeedback.feedback.Feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** An exception raised automatically by {@link AlertRuleEngine} for an organiser to follow up. */
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** The feedback that triggered the alert; null for event-level alerts such as LOW_AVERAGE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id")
    private Feedback feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant acknowledgedAt;

    private Instant resolvedAt;

    protected Alert() {
        // for JPA
    }

    public Alert(Event event, Feedback feedback, AlertType type, AlertSeverity severity, String message) {
        this.event = event;
        this.feedback = feedback;
        this.type = type;
        this.severity = severity;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    void acknowledge() {
        status = AlertStatus.ACKNOWLEDGED;
        acknowledgedAt = Instant.now();
    }

    void resolve() {
        status = AlertStatus.RESOLVED;
        resolvedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Feedback getFeedback() { return feedback; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public AlertStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
