package com.eventfeedback.feedback;

import java.time.Instant;

import com.eventfeedback.event.Event;

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

/** One attendee's feedback for one event. */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private int overallRating;

    @Column(nullable = false)
    private int contentRating;

    @Column(nullable = false)
    private int organizationRating;

    @Column(nullable = false)
    private boolean wouldRecommend;

    @Column(length = 1000)
    private String comments;

    @Column(length = 100)
    private String attendeeName;

    @Column(length = 150)
    private String attendeeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.NEW;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    /** When an organiser last changed {@link #status}; null while the feedback is still NEW. */
    private Instant statusChangedAt;

    protected Feedback() {
        // for JPA
    }

    public Feedback(Event event, int overallRating, int contentRating, int organizationRating, boolean wouldRecommend) {
        this.event = event;
        this.overallRating = overallRating;
        this.contentRating = contentRating;
        this.organizationRating = organizationRating;
        this.wouldRecommend = wouldRecommend;
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public int getOverallRating() { return overallRating; }
    public int getContentRating() { return contentRating; }
    public int getOrganizationRating() { return organizationRating; }
    public boolean isWouldRecommend() { return wouldRecommend; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

    public String getAttendeeEmail() { return attendeeEmail; }
    public void setAttendeeEmail(String attendeeEmail) { this.attendeeEmail = attendeeEmail; }

    public FeedbackStatus getStatus() { return status; }

    /** Records the new follow-up status and when it was set. */
    public void changeStatus(FeedbackStatus newStatus) {
        this.status = newStatus;
        this.statusChangedAt = Instant.now();
    }

    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
}
