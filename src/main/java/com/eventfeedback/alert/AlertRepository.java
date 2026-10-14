package com.eventfeedback.alert;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    boolean existsByFeedbackIdAndType(Long feedbackId, AlertType type);

    boolean existsByEventIdAndTypeAndStatusIn(Long eventId, AlertType type, Collection<AlertStatus> statuses);

    long countByStatusIn(Collection<AlertStatus> statuses);

    /** Newest first, with the event (and feedback, if any) loaded for the list view. */
    @Query("""
            select a from Alert a join fetch a.event left join fetch a.feedback
            where (:status is null or a.status = :status)
              and (:severity is null or a.severity = :severity)
            order by a.createdAt desc, a.id desc""")
    List<Alert> search(AlertStatus status, AlertSeverity severity);
}
