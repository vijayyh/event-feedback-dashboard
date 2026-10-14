package com.eventfeedback.web;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.ObjectProvider;
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

    public record AppInfo(String name, String version, String javaVersion, OffsetDateTime serverTime) {
    }

    private final String version;

    public InfoController(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
    }

    @GetMapping("/info")
    public AppInfo info() {
        return new AppInfo("Event Feedback Dashboard", version,
                System.getProperty("java.version"), OffsetDateTime.now());
    }
}
