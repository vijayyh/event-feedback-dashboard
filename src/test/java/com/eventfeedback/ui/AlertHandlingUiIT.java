package com.eventfeedback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.eventfeedback.alert.Alert;
import com.eventfeedback.alert.AlertStatus;
import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventCategory;
import com.eventfeedback.event.EventStatus;

/** UJ-05 – A bad review raises alerts automatically and the organiser works them off. */
@DisplayName("UJ-05 Alert handling")
class AlertHandlingUiIT extends UiTestBase {

    private static final By ALERTS = By.cssSelector("#alertList .alert-item");

    @Test
    @DisplayName("a low rating raises alerts that the organiser acknowledges and resolves")
    void lowRatingRaisesAlertsThatTheOrganiserResolves() {
        Event workshop = event("Robotics Workshop", EventCategory.WORKSHOP, EventStatus.COMPLETED, LocalDate.now().minusDays(1));
        feedback(workshop, 1, "Worst session, the speaker was not audible");   // LOW_RATING + NEGATIVE_COMMENT

        signInAsOrganiser();
        waitForText(By.id("alertBadge"), "2");

        open("alerts.html");
        wait.until(ExpectedConditions.numberOfElementsToBe(ALERTS, 2));
        assertThat(driver.findElement(By.id("alertList")).getText())
                .contains("Low rating", "Rated 1/5 for \"Robotics Workshop\"", "Negative comment");

        // acknowledge one: it leaves the Open tab but still counts as unresolved
        driver.findElement(By.cssSelector("#alertList button[data-to=ACKNOWLEDGED]")).click();
        wait.until(ExpectedConditions.numberOfElementsToBe(ALERTS, 1));
        waitForText(By.id("alertBadge"), "2");

        // resolve the other one from the Open tab, then the acknowledged one
        driver.findElement(By.cssSelector("#alertList button[data-to=RESOLVED]")).click();
        waitForText(By.id("alertList"), "No alerts here");
        driver.findElement(By.cssSelector("#alertTabs [data-tab=ACKNOWLEDGED]")).click();
        visible(By.cssSelector("#alertList button[data-to=RESOLVED]")).click();
        waitForText(By.id("alertList"), "No alerts here");

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("alertBadge")));
        assertThat(alerts.findAll()).extracting(Alert::getStatus).containsOnly(AlertStatus.RESOLVED);
    }
}
