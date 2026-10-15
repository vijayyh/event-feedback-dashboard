package com.eventfeedback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventCategory;
import com.eventfeedback.event.EventStatus;

/** UJ-04 – The organiser reads the summary indicators and drills down by status. */
@DisplayName("UJ-04 Dashboard indicators and drill-down")
class DashboardUiIT extends UiTestBase {

    @Test
    @DisplayName("dashboard shows the indicators and drills down from a status to its events")
    void dashboardShowsIndicatorsAndDrillsDownByStatus() {
        Event lecture = event("Cloud Lecture", EventCategory.SEMINAR, EventStatus.COMPLETED, LocalDate.now().minusDays(3));
        Event sprint = event("Code Sprint", EventCategory.HACKATHON, EventStatus.ONGOING, LocalDate.now());
        feedback(lecture, 5, "Excellent speaker");
        feedback(lecture, 4, null);
        feedback(sprint, 2, "Far too long");          // LOW_RATING + NEGATIVE_COMMENT ("too long")

        signInAsOrganiser();
        open("dashboard.html");

        wait.until(d -> "3".equals(kpi(d, "Responses")));
        assertThat(kpi(driver, "Events")).isEqualTo("2");
        assertThat(kpi(driver, "Average rating")).isEqualTo("3.67");   // (5 + 4 + 2) / 3
        assertThat(kpi(driver, "Satisfaction")).isEqualTo("67%");
        assertThat(kpi(driver, "Open alerts")).isEqualTo("2");

        driver.findElement(By.cssSelector("#eventBreakdown .breakdown-row[data-status=COMPLETED]")).click();
        waitForText(By.id("drilldownTitle"), "Completed events (1)");
        assertThat(driver.findElement(By.id("drilldownList")).getText()).contains("Cloud Lecture", "2 responses");

        driver.findElement(By.linkText("Cloud Lecture")).click();
        waitForText(By.cssSelector("#eventHeader h1"), "Cloud Lecture");
        waitForText(By.cssSelector(".big-score"), "4.5");
    }

    /** Value of the KPI card with the given label, or null while the cards are loading. */
    private static String kpi(WebDriver d, String label) {
        return d.findElements(By.cssSelector("#kpis .kpi")).stream()
                .filter(card -> card.findElement(By.className("kpi-label")).getText().equals(label))
                .map(card -> card.findElement(By.className("kpi-value")).getText())
                .findFirst().orElse(null);
    }
}
