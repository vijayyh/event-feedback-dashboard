package com.eventfeedback.alert;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/** T09 automatic alert rules and T10 alert/exception view. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertRulesAndViewTest {

    private static final RequestPostProcessor ORGANISER = user("organiser").roles("ORGANISER");

    @Autowired
    private MockMvc mockMvc;

    private long eventId;

    @BeforeEach
    void event() throws Exception {
        String body = mockMvc.perform(post("/api/events").with(ORGANISER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Alert test event", "category": "WORKSHOP", "eventDate": "2026-10-10", "status": "COMPLETED"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        eventId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // ---------- T09 rules ----------

    @Test
    void lowRatingRaisesAHighSeverityAlert() throws Exception {
        submit(2, null);
        alerts("").andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("LOW_RATING"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].eventTitle").value("Alert test event"))
                .andExpect(jsonPath("$[0].message").value("Rated 2/5 for \"Alert test event\""));
    }

    @Test
    void negativeKeywordRaisesAMediumAlertEvenWithAGoodRating() throws Exception {
        submit(4, "Great speaker but the sound was NOT AUDIBLE at the back");
        alerts("").andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("NEGATIVE_COMMENT"))
                .andExpect(jsonPath("$[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$[0].message", startsWith("Negative comment (\"not audible\")")));
    }

    @Test
    void keywordsMatchWholeWordsOnly() throws Exception {
        submit(5, "Loved the slowly paced intro and the waste-free lunch");   // "slowly" is not "slow"
        alerts("").andExpect(jsonPath("$[*].type", containsInAnyOrder("NEGATIVE_COMMENT")));  // "waste" is a word
        submit(5, "Badminton finals were thrilling");
        alerts("").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void goodFeedbackRaisesNoAlert() throws Exception {
        submit(5, "Excellent, very well organised");
        submit(4, null);
        alerts("").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void lowAverageIsRaisedOnceWhenEnoughResponsesAreBelowThreshold() throws Exception {
        for (int rating : new int[] {3, 3, 3, 3}) {
            submit(rating, null);
        }
        alerts("?severity=CRITICAL").andExpect(jsonPath("$", hasSize(0)));   // only 4 responses so far

        submit(2, null);                                                        // 5th response, average 2.8
        alerts("?severity=CRITICAL").andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("LOW_AVERAGE"))
                .andExpect(jsonPath("$[0].message").value("Average rating of \"Alert test event\" dropped to 2.80 after 5 responses"));

        submit(1, null);                                                        // still low: no duplicate
        alerts("?severity=CRITICAL").andExpect(jsonPath("$", hasSize(1)));
    }

    // ---------- T10 alert view ----------

    @Test
    void organiserAcknowledgesThenResolvesAnAlert() throws Exception {
        submit(1, null);
        long id = alertId();

        changeStatus(id, "ACKNOWLEDGED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedAt").isNotEmpty());
        mockMvc.perform(get("/api/alerts/count").with(ORGANISER)).andExpect(jsonPath("$.unresolved").value(1));

        changeStatus(id, "RESOLVED").andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        mockMvc.perform(get("/api/alerts/count").with(ORGANISER)).andExpect(jsonPath("$.unresolved").value(0));
        alerts("?status=RESOLVED").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void resolvedAlertsCannotBeReopened() throws Exception {
        submit(1, null);
        long id = alertId();
        changeStatus(id, "RESOLVED").andExpect(status().isOk());
        changeStatus(id, "OPEN").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Resolved alerts cannot change status"));
    }

    @Test
    void alertsAreForOrganisersOnly() throws Exception {
        mockMvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/alerts.html")).andExpect(status().is3xxRedirection());
    }

    private void submit(int overall, String comment) throws Exception {
        mockMvc.perform(post("/api/events/{id}/feedback", eventId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"overallRating": %d, "contentRating": 3, "organizationRating": 3,
                                 "wouldRecommend": false, "comments": %s}"""
                                .formatted(overall, comment == null ? "null" : "\"" + comment + "\"")))
                .andExpect(status().isCreated());
    }

    private ResultActions alerts(String query) throws Exception {
        return mockMvc.perform(get("/api/alerts" + query).with(ORGANISER)).andExpect(status().isOk());
    }

    private long alertId() throws Exception {
        String body = alerts("").andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    private ResultActions changeStatus(long id, String status) throws Exception {
        return mockMvc.perform(patch("/api/alerts/{id}", id).with(ORGANISER).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"" + status + "\"}"));
    }
}
