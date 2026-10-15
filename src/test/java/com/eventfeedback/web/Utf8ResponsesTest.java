package com.eventfeedback.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression test for bug #32: on an external Tomcat the pages were sent as ISO-8859-1, so "·", "…" and the
 * rating stars were garbled. Every text response must state UTF-8 explicitly, whatever the container defaults are.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Utf8ResponsesTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/index.html", "/event.html", "/js/common.js", "/js/home.js", "/css/app.css", "/api/events"})
    void textResponsesDeclareUtf8(String path) throws Exception {
        String contentType = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentType();

        assertThat(contentType).containsIgnoringCase("charset=UTF-8");
    }
}
