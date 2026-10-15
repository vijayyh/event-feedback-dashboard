package com.eventfeedback.web;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Basic application information, used by the landing page and by pipeline smoke tests.
 */
@RestController
@RequestMapping("/api")
public class InfoController {

    /** @param environment deployment environment set by the pipeline (app.environment), e.g. staging */
    public record AppInfo(String name, String version, String environment, String javaVersion,
                          OffsetDateTime serverTime) {
    }

    private final String version;
    private final String environment;

    public InfoController(ObjectProvider<BuildProperties> buildProperties,
                          @Value("${app.environment}") String environment) {
        BuildProperties props = buildProperties.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
        this.environment = environment;
    }

    @GetMapping("/info")
    public AppInfo info() {
        return new AppInfo("Event Feedback Dashboard", version, environment,
                System.getProperty("java.version"), OffsetDateTime.now());
    }
}
