package com.eventfeedback.feedback;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedbackApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validFeedbackIsStoredAsNew() throws Exception {
        long eventId = createEvent("COMPLETED");

        submit(eventId, feedbackJson(5, 4, 4, true, "\"Great session\"", "\"Asha\"", "\"asha@example.com\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.overallRating").value(5))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty())
                .andExpect(jsonPath("$.attendeeEmail").doesNotExist());
    }

    @Test
    void anonymousFeedbackIsAccepted() throws Exception {
        long eventId = createEvent("ONGOING");

        submit(eventId, feedbackJson(4, 4, 4, true, "null", "null", "null"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attendeeName").doesNotExist());
    }

    @Test
    void ratingOutsideOneToFiveIsRejectedAndNothingIsStored() throws Exception {
        long eventId = createEvent("COMPLETED");

        submit(eventId, feedbackJson(6, 0, 3, true, "null", "null", "null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.overallRating").exists())
                .andExpect(jsonPath("$.fieldErrors.contentRating").exists());

        mockMvc.perform(get("/api/events/{id}/feedback", eventId))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void commentLongerThan1000CharactersIsRejected() throws Exception {
        long eventId = createEvent("COMPLETED");
        String longComment = "\"" + "a".repeat(1001) + "\"";

        submit(eventId, feedbackJson(3, 3, 3, false, longComment, "null", "null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.comments").exists());
    }

    @Test
    void feedbackForCancelledEventIsRefused() throws Exception {
        long eventId = createEvent("CANCELLED");

        submit(eventId, feedbackJson(4, 4, 4, true, "null", "null", "null"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Feedback cannot be submitted for a cancelled event"));
    }

    @Test
    void feedbackForUnknownEventReturns404() throws Exception {
        submit(999999L, feedbackJson(4, 4, 4, true, "null", "null", "null"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventShowsResponseCountAndAverageAndListIsNewestFirst() throws Exception {
        long eventId = createEvent("COMPLETED");
        submit(eventId, feedbackJson(5, 5, 5, true, "\"first\"", "null", "null")).andExpect(status().isCreated());
        submit(eventId, feedbackJson(2, 3, 2, false, "\"second\"", "null", "null")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/{id}", eventId))
                .andExpect(jsonPath("$.responseCount").value(2))
                .andExpect(jsonPath("$.averageRating").value(3.5));

        mockMvc.perform(get("/api/events/{id}/feedback", eventId))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].comments").value("second"))
                .andExpect(jsonPath("$[1].comments").value("first"));
    }

    private long createEvent(String status) throws Exception {
        String body = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Test event", "category": "SEMINAR", "eventDate": "2026-10-10", "status": "%s"}"""
                                .formatted(status)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions submit(long eventId, String json) throws Exception {
        return mockMvc.perform(post("/api/events/{id}/feedback", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String feedbackJson(int overall, int content, int organization, boolean recommend,
                                       String comments, String name, String email) {
        return """
                {"overallRating": %d, "contentRating": %d, "organizationRating": %d, "wouldRecommend": %s,
                 "comments": %s, "attendeeName": %s, "attendeeEmail": %s}"""
                .formatted(overall, content, organization, recommend, comments, name, email);
    }
}
