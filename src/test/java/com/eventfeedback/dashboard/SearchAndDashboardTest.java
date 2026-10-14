package com.eventfeedback.dashboard;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.eventfeedback.ApiTestSupport;

/** T04 search, T05 summary indicators, T07 status drill-down and T08 feedback workflow. */
class SearchAndDashboardTest extends ApiTestSupport {

    private long workshop;
    private long hackathon;

    @BeforeEach
    void data() throws Exception {
        workshop = createEvent("Jenkins Workshop", "WORKSHOP", "2026-10-01", "COMPLETED", "Lab 3");
        hackathon = createEvent("Smart Campus Hackathon", "HACKATHON", "2026-10-14", "ONGOING", "Innovation Lab");
        createEvent("Cultural Fest", "CULTURAL", "2026-11-20", "UPCOMING", "Open Air Theatre");
        createEvent("100% Attendance Drive", "OTHER", "2026-09-01", "CANCELLED", null);

        submitFeedback(workshop, 5, true, "Excellent hands-on session");
        submitFeedback(workshop, 4, true, null);
        submitFeedback(workshop, 2, false, "Sound was not audible");
        submitFeedback(hackathon, 3, true, "Great mentors, slow wifi");
    }

    // ---------- T04 search ----------

    @Test
    void keywordSearchIsCaseInsensitiveAndCoversTitleAndVenue() throws Exception {
        events("?q=jenkins").andExpect(jsonPath("$[*].title", contains("Jenkins Workshop")));
        events("?q=innovation").andExpect(jsonPath("$[*].title", contains("Smart Campus Hackathon")));
    }

    @Test
    void filtersCombineStatusCategoryAndDateRange() throws Exception {
        events("?status=COMPLETED&category=WORKSHOP").andExpect(jsonPath("$", hasSize(1)));
        events("?from=2026-10-01&to=2026-10-31")
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Jenkins Workshop", "Smart Campus Hackathon")));
        events("?status=UPCOMING&category=WORKSHOP").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void wildcardCharactersAreMatchedLiterally() throws Exception {
        mockMvc.perform(get("/api/events").param("q", "100%"))
                .andExpect(jsonPath("$[*].title", contains("100% Attendance Drive")));
        mockMvc.perform(get("/api/events").param("q", "_"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void organiserCanSearchFeedbackAcrossEvents() throws Exception {
        mockMvc.perform(get("/api/feedback?maxRating=2").with(ORGANISER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].comments", contains("Sound was not audible")))
                .andExpect(jsonPath("$[0].eventTitle").value("Jenkins Workshop"));
        mockMvc.perform(get("/api/feedback?q=MENTORS").with(ORGANISER))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/feedback?eventId=" + workshop + "&minRating=4").with(ORGANISER))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void feedbackSearchIsForOrganisersOnly() throws Exception {
        mockMvc.perform(get("/api/feedback")).andExpect(status().isUnauthorized());
    }

    // ---------- T05 summary indicators ----------

    @Test
    void summaryIndicatorsMatchTheData() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").with(ORGANISER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(4))
                .andExpect(jsonPath("$.totalResponses").value(4))
                .andExpect(jsonPath("$.averageRating").value(3.5))      // (5+4+2+3)/4
                .andExpect(jsonPath("$.satisfactionPercent").value(50))  // 5 and 4 out of 4
                .andExpect(jsonPath("$.recommendPercent").value(75));    // 3 of 4
    }

    @Test
    void dashboardIsForOrganisersOnly() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/status-breakdown")).andExpect(status().isUnauthorized());
    }

    // ---------- T07 status drill-down ----------

    @Test
    void statusBreakdownListsEveryStatus() throws Exception {
        mockMvc.perform(get("/api/dashboard/status-breakdown").with(ORGANISER))
                .andExpect(jsonPath("$.events.UPCOMING").value(1))
                .andExpect(jsonPath("$.events.ONGOING").value(1))
                .andExpect(jsonPath("$.events.COMPLETED").value(1))
                .andExpect(jsonPath("$.events.CANCELLED").value(1))
                .andExpect(jsonPath("$.feedback.NEW").value(4))
                .andExpect(jsonPath("$.feedback.RESOLVED").value(0));
    }

    @Test
    void eventSummaryShowsDistributionAndAverages() throws Exception {
        mockMvc.perform(get("/api/events/{id}/summary", workshop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCount").value(3))
                .andExpect(jsonPath("$.averageRating").value(3.67))
                .andExpect(jsonPath("$.recommendPercent").value(67))
                .andExpect(jsonPath("$.distribution.5").value(1))
                .andExpect(jsonPath("$.distribution.4").value(1))
                .andExpect(jsonPath("$.distribution.2").value(1))
                .andExpect(jsonPath("$.distribution.1").value(0));
    }

    // ---------- T08 feedback workflow ----------

    @Test
    void organiserMovesFeedbackThroughTheWorkflow() throws Exception {
        long id = submitFeedback(hackathon, 1, false, "Too crowded");

        changeStatus(id, "ACTION_REQUIRED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTION_REQUIRED"))
                .andExpect(jsonPath("$.statusChangedAt").isNotEmpty());
        changeStatus(id, "RESOLVED").andExpect(status().isOk());

        changeStatus(id, "NEW")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Resolved feedback cannot change status"));
    }

    @Test
    void feedbackCannotGoBackToNew() throws Exception {
        long id = submitFeedback(hackathon, 4, true, null);
        changeStatus(id, "REVIEWED").andExpect(status().isOk());
        changeStatus(id, "NEW")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Feedback cannot be moved back to NEW"));
    }

    @Test
    void attendeesCannotChangeFeedbackStatus() throws Exception {
        long id = submitFeedback(hackathon, 4, true, null);
        mockMvc.perform(patch("/api/feedback/{id}/status", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"REVIEWED\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions events(String query) throws Exception {
        return mockMvc.perform(get("/api/events" + query)).andExpect(status().isOk());
    }

    private ResultActions changeStatus(long id, String status) throws Exception {
        return mockMvc.perform(patch("/api/feedback/{id}/status", id).with(ORGANISER).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"" + status + "\"}"));
    }
}
