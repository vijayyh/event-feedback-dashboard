package com.eventfeedback.feedback;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Organiser-only feedback endpoints across all events (see SecurityConfig). */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackSearchController {

    /** Body of {@code PATCH /api/feedback/{id}/status}. */
    public record StatusChange(@NotNull FeedbackStatus status) {
    }

    private final FeedbackService feedbackService;

    public FeedbackSearchController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public List<FeedbackResponse> search(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Long eventId,
                                         @RequestParam(required = false) FeedbackStatus status,
                                         @RequestParam(required = false) Integer minRating,
                                         @RequestParam(required = false) Integer maxRating) {
        return feedbackService.search(new FeedbackFilter(q, eventId, status, minRating, maxRating));
    }

    @PatchMapping("/{id}/status")
    public FeedbackResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChange request) {
        return feedbackService.changeStatus(id, request.status());
    }
}
