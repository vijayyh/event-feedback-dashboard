package com.eventfeedback;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/** Common setup for API tests: in-memory database, rolled back after every test, plus data helpers. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

    protected static final RequestPostProcessor ORGANISER = user("organiser").roles("ORGANISER");

    @Autowired
    protected MockMvc mockMvc;

    /** Creates an event as the organiser and returns its id. */
    protected long createEvent(String title, String category, String date, String status, String venue) throws Exception {
        String body = mockMvc.perform(post("/api/events").with(ORGANISER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "category": "%s", "eventDate": "%s", "status": "%s", "venue": %s}"""
                                .formatted(title, category, date, status, venue == null ? "null" : "\"" + venue + "\"")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected long createEvent(String status) throws Exception {
        return createEvent("Test event", "SEMINAR", "2026-10-10", status, null);
    }

    /** Submits feedback as an anonymous attendee and returns its id. */
    protected long submitFeedback(long eventId, int overall, boolean recommend, String comment) throws Exception {
        String body = mockMvc.perform(post("/api/events/{id}/feedback", eventId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"overallRating": %d, "contentRating": 3, "organizationRating": 3,
                                 "wouldRecommend": %s, "comments": %s}"""
                                .formatted(overall, recommend, comment == null ? "null" : "\"" + comment + "\"")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
