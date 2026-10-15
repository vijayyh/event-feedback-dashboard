package com.eventfeedback.alert;

/** OPEN -> ACKNOWLEDGED -> RESOLVED (OPEN may also go straight to RESOLVED). RESOLVED is final. */
public enum AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED
}
