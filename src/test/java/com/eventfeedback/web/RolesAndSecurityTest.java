package com.eventfeedback.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/** Attendees are anonymous; only the signed-in organiser may create events or change their status. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RolesAndSecurityTest {

    private static final RequestPostProcessor ORGANISER = user("organiser").roles("ORGANISER");
    private static final String EVENT_JSON = """
            {"title": "Role test", "category": "SEMINAR", "eventDate": "2026-10-10", "status": "%s"}""";
    private static final String FEEDBACK_JSON = """
            {"overallRating": 4, "contentRating": 4, "organizationRating": 4, "wouldRecommend": true}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void attendeeCannotCreateEvents() throws Exception {
        mockMvc.perform(post("/api/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_JSON.formatted("UPCOMING")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestsWithoutCsrfTokenAreRejected() throws Exception {
        mockMvc.perform(post("/api/events").with(ORGANISER)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_JSON.formatted("UPCOMING")))
                .andExpect(status().isForbidden());
    }

    @Test
    void organiserCanChangeEventStatusButAttendeeCannot() throws Exception {
        long id = createEvent("UPCOMING");

        mockMvc.perform(patch("/api/events/{id}/status", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ONGOING\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/events/{id}/status", id).with(ORGANISER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ONGOING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"));
    }

    @Test
    void organiserCannotSubmitFeedback() throws Exception {
        long id = createEvent("COMPLETED");

        mockMvc.perform(post("/api/events/{id}/feedback", id).with(ORGANISER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(FEEDBACK_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Organisers cannot submit feedback for events"));
    }

    @Test
    void feedbackOpensOnlyWhenTheEventStarts() throws Exception {
        long id = createEvent("UPCOMING");

        mockMvc.perform(post("/api/events/{id}/feedback", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(FEEDBACK_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Feedback opens when the event starts"));
    }

    @Test
    void organiserCanSignInWithTheConfiguredPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "organiser").param("password", "test-only-password"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "organiser").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Wrong username or password"));
    }

    @Test
    void meEndpointReportsTheRole() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(jsonPath("$.organiser").value(false));
        mockMvc.perform(get("/api/auth/me").with(ORGANISER))
                .andExpect(jsonPath("$.organiser").value(true))
                .andExpect(jsonPath("$.username").value("organiser"));
    }

    @Test
    void organiserPageRedirectsAttendeesToSignIn() throws Exception {
        mockMvc.perform(get("/organiser.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login.html"));
    }

    @Test
    void publicPagesAndReadApisNeedNoSignIn() throws Exception {
        mockMvc.perform(get("/api/events")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private long createEvent(String status) throws Exception {
        String body = mockMvc.perform(post("/api/events").with(ORGANISER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_JSON.formatted(status)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
