package com.eventfeedback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventCategory;
import com.eventfeedback.event.EventStatus;

/** UJ-02 – An attendee rates an event and sees the result on the event page. */
@DisplayName("UJ-02 Give feedback")
class SubmitFeedbackUiIT extends UiTestBase {

    @Test
    @DisplayName("attendee submits feedback and sees it on the event page")
    void attendeeSubmitsFeedbackAndSeesItOnTheEventPage() {
        Event seminar = event("DevOps Seminar", EventCategory.SEMINAR, EventStatus.ONGOING, LocalDate.now());

        open("feedback.html?eventId=" + seminar.getId());
        visible(By.cssSelector("label[for=overallRating-4]")).click();
        driver.findElement(By.cssSelector("label[for=contentRating-5]")).click();
        driver.findElement(By.cssSelector("label[for=organizationRating-4]")).click();
        driver.findElement(By.cssSelector("label[for=rec-yes]")).click();
        driver.findElement(By.id("comments")).sendKeys("Clear explanation of Jenkins pipelines");
        driver.findElement(By.id("attendeeName")).sendKeys("Asha");
        driver.findElement(By.id("submitBtn")).click();

        waitForText(By.id("thanks"), "Thank you! Your feedback has been recorded.");
        driver.findElement(By.id("seeFeedback")).click();

        // the event page renders its header (regression test for bug #24) and the new feedback
        waitForText(By.cssSelector("#eventHeader h1"), "DevOps Seminar");
        waitForText(By.cssSelector(".big-score"), "4.0");
        assertThat(driver.findElement(By.id("summary")).getText()).contains("1 response", "100% would recommend it");
        assertThat(texts(By.cssSelector(".feedback-item .comment")))
                .singleElement().asString().contains("Clear explanation of Jenkins pipelines");
        assertThat(driver.findElement(By.cssSelector(".feedback-item .feedback-meta")).getText()).startsWith("Asha");
    }

    @Test
    @DisplayName("missing ratings are reported next to the fields and nothing is stored")
    void missingRatingsAreReportedNextToTheFields() {
        Event hackathon = event("Code Sprint", EventCategory.HACKATHON, EventStatus.ONGOING, LocalDate.now());

        open("feedback.html?eventId=" + hackathon.getId());
        visible(By.id("submitBtn")).click();

        waitForText(By.cssSelector(".field-error[data-for=overallRating]"), "must not be null");
        assertThat(driver.findElement(By.cssSelector(".field-error[data-for=wouldRecommend]")).getText()).isNotBlank();
        assertThat(driver.findElement(By.id("thanks")).isDisplayed()).isFalse();
        assertThat(feedback.count()).isZero();
    }

    @Test
    @DisplayName("an upcoming event cannot be rated yet")
    void upcomingEventCannotBeRated() {
        Event fest = event("Cultural Fest", EventCategory.CULTURAL, EventStatus.UPCOMING, LocalDate.now().plusDays(7));

        open("feedback.html?eventId=" + fest.getId());

        waitForText(By.id("blocked"), "Feedback opens when the event starts");
        assertThat(driver.findElement(By.id("formCard")).isDisplayed()).isFalse();
    }
}
