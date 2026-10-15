package com.eventfeedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Entry point for the Event Feedback Dashboard.
 *
 * <p>Runs on the embedded server during development ({@code mvn spring-boot:run})
 * and, through {@link SpringBootServletInitializer}, as a WAR on an external Tomcat 11.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EventFeedbackDashboardApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(EventFeedbackDashboardApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(EventFeedbackDashboardApplication.class);
    }
}
