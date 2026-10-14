package com.eventfeedback.alert;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Organiser-only alert endpoints (see SecurityConfig). */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    /** Body of {@code PATCH /api/alerts/{id}}. */
    public record StatusChange(@NotNull AlertStatus status) {
    }

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status,
                                    @RequestParam(required = false) AlertSeverity severity) {
        return alertService.list(status, severity);
    }

    /** Number of open + acknowledged alerts, for the badge in the navigation. */
    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of("unresolved", alertService.countUnresolved());
    }

    @PatchMapping("/{id}")
    public AlertResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChange request) {
        return alertService.changeStatus(id, request.status());
    }
}
