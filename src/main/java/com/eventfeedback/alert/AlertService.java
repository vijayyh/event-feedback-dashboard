package com.eventfeedback.alert;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.common.BusinessRuleException;
import com.eventfeedback.common.ResourceNotFoundException;

/** Alert/exception view (T10): list, acknowledge and resolve alerts. */
@Service
@Transactional(readOnly = true)
public class AlertService {

    private static final List<AlertStatus> UNRESOLVED = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private final AlertRepository alerts;

    public AlertService(AlertRepository alerts) {
        this.alerts = alerts;
    }

    /** Newest first; null filters are ignored. */
    public List<AlertResponse> list(AlertStatus status, AlertSeverity severity) {
        return alerts.search(status, severity).stream().map(AlertResponse::from).toList();
    }

    /** Alerts still needing attention (open or acknowledged). */
    public long countUnresolved() {
        return alerts.countByStatusIn(UNRESOLVED);
    }

    /** OPEN -> ACKNOWLEDGED -> RESOLVED; alerts are never reopened and RESOLVED is final. */
    @Transactional
    public AlertResponse changeStatus(Long id, AlertStatus target) {
        Alert alert = alerts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + id + " not found"));
        if (alert.getStatus() == target) {
            return AlertResponse.from(alert);
        }
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessRuleException("Resolved alerts cannot change status");
        }
        switch (target) {
            case OPEN -> throw new BusinessRuleException("Alerts cannot be reopened");
            case ACKNOWLEDGED -> alert.acknowledge();
            case RESOLVED -> alert.resolve();
        }
        return AlertResponse.from(alert);
    }
}
