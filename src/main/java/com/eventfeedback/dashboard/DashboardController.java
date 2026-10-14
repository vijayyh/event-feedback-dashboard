package com.eventfeedback.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Organiser dashboard data (see SecurityConfig). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardService.SummaryIndicators summary() {
        return dashboardService.summary();
    }

    @GetMapping("/status-breakdown")
    public DashboardService.StatusBreakdown statusBreakdown() {
        return dashboardService.statusBreakdown();
    }
}
