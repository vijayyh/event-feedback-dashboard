package com.eventfeedback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.eventfeedback.event.EventCategory;
import com.eventfeedback.event.EventStatus;

/** UJ-01 – An attendee browses events by status and searches them. */
@DisplayName("UJ-01 Browse and search events")
class BrowseEventsUiIT extends UiTestBase {

    private static final By CARD_TITLES = By.cssSelector("#eventCards .event-card h3");

    @BeforeEach
    void data() {
        LocalDate today = LocalDate.now();
        event("Jenkins Workshop", EventCategory.WORKSHOP, EventStatus.COMPLETED, today.minusDays(5));
        event("Smart Campus Hackathon", EventCategory.HACKATHON, EventStatus.ONGOING, today);
        event("Cultural Fest", EventCategory.CULTURAL, EventStatus.UPCOMING, today.plusDays(10));
    }

    @Test
    @DisplayName("status tabs show only the events with that status")
    void statusTabsShowOnlyEventsWithThatStatus() {
        open("");
        waitForText(By.cssSelector("#statusTabs [data-status=ALL]"), "3");
        assertThat(texts(CARD_TITLES))
                .containsExactlyInAnyOrder("Jenkins Workshop", "Smart Campus Hackathon", "Cultural Fest");

        driver.findElement(By.cssSelector("#statusTabs [data-status=COMPLETED]")).click();
        wait.until(d -> texts(CARD_TITLES).equals(List.of("Jenkins Workshop")));

        driver.findElement(By.cssSelector("#statusTabs [data-status=UPCOMING]")).click();
        wait.until(d -> texts(CARD_TITLES).equals(List.of("Cultural Fest")));
        WebElement upcoming = driver.findElement(By.cssSelector("#eventCards .event-card"));
        assertThat(upcoming.getText()).contains("Feedback opens when the event starts");
        assertThat(upcoming.findElements(By.linkText("Give feedback"))).isEmpty();
    }

    @Test
    @DisplayName("keyword search narrows the list")
    void keywordSearchNarrowsTheList() {
        open("");
        visible(By.id("search")).sendKeys("hack");
        wait.until(d -> texts(CARD_TITLES).equals(List.of("Smart Campus Hackathon")));
        assertThat(driver.findElement(By.cssSelector("#eventCards .event-card")).getText()).contains("Give feedback");
    }
}
