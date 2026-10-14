package com.eventfeedback.alert;

public enum AlertType {
    /** One response with a very low overall rating. */
    LOW_RATING,
    /** A comment containing one of the configured negative keywords. */
    NEGATIVE_COMMENT,
    /** The event's average rating dropped below the threshold. */
    LOW_AVERAGE
}
