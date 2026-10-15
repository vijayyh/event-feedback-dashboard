package com.eventfeedback.common;

/** Thrown when a valid request breaks a business rule; mapped to HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
