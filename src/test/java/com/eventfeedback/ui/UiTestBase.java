package com.eventfeedback.ui;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.eventfeedback.alert.AlertRepository;
import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventCategory;
import com.eventfeedback.event.EventRepository;
import com.eventfeedback.event.EventStatus;
import com.eventfeedback.feedback.FeedbackRepository;
import com.eventfeedback.feedback.FeedbackRequest;
import com.eventfeedback.feedback.FeedbackService;

/**
 * Base class for the Selenium journeys: starts the whole application on a random port with the
 * in-memory test database, opens a fresh Chrome for every test and cleans the data afterwards.
 *
 * <p>Chrome runs headless unless {@code -Dselenium.headless=false}. The driver binary is resolved by
 * Selenium Manager. Failed tests leave a screenshot thanks to {@link ScreenshotOnFailure}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(ScreenshotOnFailure.class)
abstract class UiTestBase {

    /** Organiser password of the "test" profile (src/test/resources/application-test.properties). */
    static final String ORGANISER_PASSWORD = "test-only-password";

    @LocalServerPort
    int port;

    @Autowired
    EventRepository events;
    @Autowired
    FeedbackRepository feedback;
    @Autowired
    AlertRepository alerts;
    @Autowired
    FeedbackService feedbackService;

    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach
    void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        // --no-sandbox lets Chrome start under service accounts such as Jenkins' SYSTEM user
        options.addArguments("--window-size=1366,900", "--no-sandbox", "--disable-gpu", "--lang=en-US");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void closeBrowserAndCleanData() {
        if (driver != null) {
            driver.quit();
        }
        alerts.deleteAllInBatch();
        feedback.deleteAllInBatch();
        events.deleteAllInBatch();
    }

    // ---------- navigation ----------

    String url(String path) {
        return "http://localhost:" + port + "/" + path;
    }

    void open(String path) {
        driver.get(url(path));
    }

    WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    void waitForText(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    List<String> texts(By locator) {
        return driver.findElements(locator).stream().map(WebElement::getText).toList();
    }

    void setValue(WebElement input, String value) {
        // date inputs cannot be typed reliably across locales, so set the value directly
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", input, value);
    }

    void signInAsOrganiser() {
        open("login.html");
        visible(By.id("password")).sendKeys(ORGANISER_PASSWORD);
        driver.findElement(By.id("loginBtn")).click();
        wait.until(ExpectedConditions.urlContains("organiser.html"));
    }

    // ---------- test data ----------

    Event event(String title, EventCategory category, EventStatus status, LocalDate date) {
        Event e = new Event(title, category, date);
        e.setStatus(status);
        e.setVenue("Seminar Hall A");
        return events.save(e);
    }

    /** Submits through the service, so the alert rules run exactly as for a real attendee. */
    void feedback(Event event, int overall, String comment) {
        feedbackService.submit(event.getId(),
                new FeedbackRequest(overall, overall, overall, overall >= 3, comment, null, null));
    }
}
