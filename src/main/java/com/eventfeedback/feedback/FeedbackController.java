package com.eventfeedback.feedback;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@PathVariable Long eventId, @Valid @RequestBody FeedbackRequest request,
                                   HttpServletRequest http) {
        if (http.isUserInRole("ORGANISER")) {
            throw new AccessDeniedException("Organisers cannot submit feedback for events");
        }
        return feedbackService.submit(eventId, request);
    }

    @GetMapping
    public List<FeedbackResponse> list(@PathVariable Long eventId) {
        return feedbackService.listForEvent(eventId);
    }
}
