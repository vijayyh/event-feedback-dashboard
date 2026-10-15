package com.eventfeedback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

/** UJ-03 – The organiser signs in, creates an event and opens it for feedback. */
@DisplayName("UJ-03 Organiser manages events")
class OrganiserManagesEventsUiIT extends UiTestBase {

    @Test
    @DisplayName("attendee is sent to sign-in and a wrong password is rejected")
    void attendeeIsSentToSignInAndWrongPasswordIsRejected() {
        open("organiser.html");
        wait.until(ExpectedConditions.urlContains("login.html"));

        visible(By.id("password")).sendKeys("not-the-password");
        driver.findElement(By.id("loginBtn")).click();

        waitForText(By.id("loginNotice"), "Wrong username or password");
        assertThat(driver.getCurrentUrl()).contains("login.html");
    }

    @Test
    @DisplayName("organiser creates an event and opens it for feedback")
    void organiserCreatesAnEventAndOpensItForFeedback() {
        signInAsOrganiser();
        waitForText(By.cssSelector(".role-badge"), "Organiser");

        visible(By.id("title")).sendKeys("Selenium Workshop");
        new Select(driver.findElement(By.id("category"))).selectByValue("WORKSHOP");
        setValue(driver.findElement(By.id("eventDate")), LocalDate.now().toString());
        driver.findElement(By.id("venue")).sendKeys("Lab 2");
        driver.findElement(By.id("saveBtn")).click();
        waitForText(By.id("formNotice"), "Event \"Selenium Workshop\" saved.");

        WebElement status = wait.until(d -> d.findElements(By.cssSelector("#eventRows tr")).stream()
                .filter(row -> row.getText().contains("Selenium Workshop"))
                .findFirst()
                .map(row -> row.findElement(By.tagName("select")))
                .orElse(null));
        new Select(status).selectByValue("ONGOING");
        waitForText(By.id("manageNotice"), "\"Selenium Workshop\" is now ongoing.");

        // the attendee view now offers feedback for the event
        driver.findElement(By.id("signOutBtn")).click();
        wait.until(ExpectedConditions.urlContains("index.html"));
        waitForText(By.cssSelector(".role-badge"), "Attendee view");
        WebElement card = wait.until(d -> d.findElements(By.cssSelector("#eventCards .event-card")).stream()
                .filter(c -> c.getText().contains("Selenium Workshop"))
                .findFirst().orElse(null));
        assertThat(card.findElements(By.linkText("Give feedback"))).hasSize(1);
    }
}
